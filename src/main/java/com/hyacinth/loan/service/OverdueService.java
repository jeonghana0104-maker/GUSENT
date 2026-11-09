package com.hyacinth.loan.service;

import com.hyacinth.loan.domain.Installment;
import com.hyacinth.loan.domain.Loan;
import com.hyacinth.loan.domain.LoanEvent;
import com.hyacinth.loan.domain.LoanEventType;
import com.hyacinth.loan.domain.PaymentCycle;
import com.hyacinth.loan.repository.LoanRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class OverdueService {

	private final LoanRepository loanRepository;
	private final SettingService settingService;
	private final LoanEventService eventService;

	public OverdueService(LoanRepository loanRepository, SettingService settingService, LoanEventService eventService) {
		this.loanRepository = loanRepository;
		this.settingService = settingService;
		this.eventService = eventService;
	}

	public void applyOverdueInterest(Instant now) {
		BigDecimal rate = settingService.get().overdueRatePerRound().movePointLeft(2);
		for (Loan loan : loanRepository.findActiveDueBefore(now)) {
			long[] totals = new long[2];
			Loan updated = loanRepository.update(loan.customerId(), current -> {
				totals[0] = 0;
				totals[1] = 0;
				return apply(current, now, rate, totals);
			});
			if (totals[0] > 0) {
				eventService.record(event(updated, LoanEventType.OVERDUE_STARTED, totals[0], now));
			}
			if (totals[1] > 0) {
				eventService.record(event(updated, LoanEventType.OVERDUE_INTEREST_ADDED, totals[1], now));
			}
		}
	}

	private Loan apply(Loan loan, Instant now, BigDecimal rate, long[] totals) {
		if (loan.isCompleted()) {
			return loan;
		}
		List<Installment> updated = new ArrayList<>();
		for (Installment installment : loan.installments()) {
			if (installment.unpaidAmount() == 0 || installment.dueAt().isAfter(now)) {
				updated.add(installment);
				continue;
			}
			boolean started = installment.lastOverdueAppliedAt() == null;
			Instant appliedAt = started ? installment.dueAt() : installment.lastOverdueAppliedAt();
			long added = 0;
			PaymentCycle cycle = loan.cycle();
			Instant next = cycle.addTo(appliedAt, 1);
			while (!next.isAfter(now)) {
				added += interest(installment.unpaidAmount(), rate);
				appliedAt = next;
				next = cycle.addTo(appliedAt, 1);
			}
			if (started) {
				totals[0] += installment.unpaidAmount();
			}
			totals[1] += added;
			updated.add(started || !appliedAt.equals(installment.lastOverdueAppliedAt())
					? installment.withOverdue(added, appliedAt)
					: installment);
		}
		return loan.withInstallments(updated, now);
	}

	private long interest(long amount, BigDecimal rate) {
		return BigDecimal.valueOf(amount).multiply(rate).setScale(0, RoundingMode.HALF_UP).longValueExact();
	}

	private LoanEvent event(Loan loan, LoanEventType type, long amount, Instant now) {
		return new LoanEvent(loan.loanId(), loan.customerId(), loan.userName(), type, amount,
				loan.remainingAmount(), now);
	}
}
