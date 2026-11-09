package com.hyacinth.loan.service;

import com.hyacinth.loan.domain.Installment;
import com.hyacinth.loan.domain.Loan;
import com.hyacinth.loan.domain.LoanEvent;
import com.hyacinth.loan.domain.LoanEventType;
import com.hyacinth.loan.domain.LoanException;
import com.hyacinth.loan.domain.LoanSettings;
import com.hyacinth.loan.domain.LoanStatus;
import com.hyacinth.loan.domain.RepaymentMethod;
import com.hyacinth.loan.repository.LoanRepository;
import com.hyacinth.loan.schedule.RepaymentScheduleCalculator;
import com.hyacinth.loan.schedule.ScheduleRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LoanService {

	private static final int MAX_CUSTOMER_ID_LENGTH = 30;
	private static final int MAX_USER_NAME_LENGTH = 32;

	private final LoanRepository loanRepository;
	private final SettingService settingService;
	private final LoanEventService eventService;
	private final LoanStatusResolver statusResolver;
	private final Map<RepaymentMethod, RepaymentScheduleCalculator> calculators;
	private final Clock clock;

	public LoanService(LoanRepository loanRepository, SettingService settingService, LoanEventService eventService,
			LoanStatusResolver statusResolver, List<RepaymentScheduleCalculator> calculators, Clock clock) {
		this.loanRepository = loanRepository;
		this.settingService = settingService;
		this.eventService = eventService;
		this.statusResolver = statusResolver;
		this.calculators = calculators.stream()
				.collect(Collectors.toMap(RepaymentScheduleCalculator::method, Function.identity()));
		this.clock = clock;
	}

	public Loan register(RegisterLoanRequest request) {
		LoanSettings settings = settingService.get();
		String customerId = validText(request.customerId(), MAX_CUSTOMER_ID_LENGTH, "고유번호");
		String userName = validText(request.userName(), MAX_USER_NAME_LENGTH, "유저명");
		if (request.amount() < 1 || request.amount() > settings.maxLoanAmount()) {
			throw new LoanException("금액은 1~%,d원 사이로 입력해 주세요.".formatted(settings.maxLoanAmount()));
		}

		Instant now = clock.instant();
		List<Installment> installments = calculators.get(request.method()).calculate(new ScheduleRequest(
				request.amount(), settings.ratePerRound(), settings.rounds(), settings.cycle(), now));
		Loan draft = new Loan(UUID.randomUUID().toString(), customerId, userName, request.amount(), request.method(),
				settings.ratePerRound(), settings.rounds(), settings.cycle(), now, null, null, installments);
		Loan loan = draft.withInstallments(installments, now);

		loanRepository.createIfNoActiveLoan(loan);
		eventService.record(event(loan, LoanEventType.REGISTERED, request.amount(), now));
		return loan;
	}

	public RepaymentResult repay(RepayLoanRequest request) {
		Instant now = clock.instant();
		String customerId = request.customerId().strip();
		String userName = request.userName().strip();
		long amount = request.amount();
		AtomicReference<String> summary = new AtomicReference<>();

		Loan updated = loanRepository.update(customerId, current -> {
			if (!current.userName().equals(userName)) {
				throw new LoanException("고유번호와 유저명이 일치하지 않습니다.");
			}
			if (current.isCompleted()) {
				throw new LoanException("이미 완납한 대출입니다.");
			}
			if (amount < 1 || amount > current.remainingAmount()) {
				throw new LoanException("상환금액은 1~%,d원 사이로 입력해 주세요.".formatted(current.remainingAmount()));
			}
			Allocation allocation = allocate(current.installments(), amount);
			summary.set(allocation.summary());
			return current.withInstallments(allocation.installments(), now);
		});

		eventService.record(event(updated, LoanEventType.REPAID, amount, now));
		if (updated.isCompleted()) {
			eventService.record(event(updated, LoanEventType.COMPLETED, 0, now));
		}
		return new RepaymentResult(updated, amount, summary.get(),
				statusResolver.resolve(updated, now, settingService.get()));
	}

	public Map<LoanStatus, List<LoanListItem>> findAllGroupedByStatus() {
		Instant now = clock.instant();
		LoanSettings settings = settingService.get();
		Map<LoanStatus, List<LoanListItem>> groups = new EnumMap<>(LoanStatus.class);
		for (Loan loan : loanRepository.findAll()) {
			LoanStatus status = statusResolver.resolve(loan, now, settings);
			groups.computeIfAbsent(status, ignored -> new ArrayList<>()).add(new LoanListItem(loan, status));
		}
		return groups;
	}

	public LoanListItem findDetail(String customerId) {
		Loan loan = loanRepository.findByCustomerId(customerId.strip())
				.orElseThrow(() -> new LoanException("등록된 대출이 없습니다."));
		return new LoanListItem(loan, statusResolver.resolve(loan, clock.instant(), settingService.get()));
	}

	private Allocation allocate(List<Installment> installments, long amount) {
		List<Installment> updated = new ArrayList<>();
		List<Integer> completed = new ArrayList<>();
		Integer partial = null;
		long remaining = amount;

		for (Installment installment : installments) {
			if (remaining == 0 || installment.isSettled()) {
				updated.add(installment);
				continue;
			}
			long overduePayment = Math.min(remaining, installment.unpaidOverdueInterest());
			remaining -= overduePayment;
			long amountPayment = Math.min(remaining, installment.unpaidAmount());
			remaining -= amountPayment;

			Installment paid = installment.withPayment(overduePayment, amountPayment);
			updated.add(paid);
			if (paid.isSettled()) {
				completed.add(paid.round());
			}
			else {
				partial = paid.round();
			}
		}
		return new Allocation(updated, summary(completed, partial));
	}

	private String summary(List<Integer> completed, Integer partial) {
		List<String> parts = new ArrayList<>();
		if (completed.size() == 1) {
			parts.add(completed.get(0) + "회차 납부 완료");
		}
		else if (completed.size() > 1) {
			parts.add(completed.get(0) + "~" + completed.get(completed.size() - 1) + "회차 납부 완료");
		}
		if (partial != null) {
			parts.add(partial + "회차 일부 납부");
		}
		return String.join(", ", parts);
	}

	private String validText(String value, int maxLength, String label) {
		String text = value == null ? "" : value.strip();
		if (text.isEmpty() || text.length() > maxLength) {
			throw new LoanException("%s은(는) 1~%d자로 입력해 주세요.".formatted(label, maxLength));
		}
		return text;
	}

	private LoanEvent event(Loan loan, LoanEventType type, long amount, Instant now) {
		return new LoanEvent(loan.loanId(), loan.customerId(), loan.userName(), type, amount,
				loan.remainingAmount(), now);
	}

	private record Allocation(List<Installment> installments, String summary) {
	}
}
