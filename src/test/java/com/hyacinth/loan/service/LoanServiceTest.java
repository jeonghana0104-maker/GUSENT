package com.hyacinth.loan.service;

import com.hyacinth.discord.message.LoanMessageFormatter;
import com.hyacinth.infra.memory.MemoryLoanEventRepository;
import com.hyacinth.infra.memory.MemoryLoanRepository;
import com.hyacinth.infra.memory.MemorySettingRepository;
import com.hyacinth.loan.domain.Actor;
import com.hyacinth.loan.domain.DuplicateLoanException;
import com.hyacinth.loan.domain.Loan;
import com.hyacinth.loan.domain.LoanEvent;
import com.hyacinth.loan.domain.LoanEventType;
import com.hyacinth.loan.domain.LoanException;
import com.hyacinth.loan.domain.LoanStatus;
import com.hyacinth.loan.domain.RepaymentMethod;
import com.hyacinth.loan.schedule.BulletRepaymentCalculator;
import com.hyacinth.loan.schedule.EqualPaymentCalculator;
import com.hyacinth.loan.schedule.EqualPrincipalCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanServiceTest {

	private final Actor actor = new Actor("1", "tester");
	private final MutableClock clock = new MutableClock(Instant.parse("2026-11-09T00:00:00Z"));
	private final MemoryLoanRepository loans = new MemoryLoanRepository();
	private final MemorySettingRepository settings = new MemorySettingRepository();
	private final MemoryLoanEventRepository events = new MemoryLoanEventRepository();
	private final SettingService settingService = new SettingService(settings, clock);
	private final LoanEventService eventService = new LoanEventService(events);
	private final LoanMessageFormatter formatter = new LoanMessageFormatter();
	private final OverdueService overdueService = new OverdueService(loans, settingService, eventService);
	private LoanService service;

	@BeforeEach
	void setUp() {
		service = new LoanService(loans, settingService, eventService, new LoanStatusResolver(),
				List.of(new EqualPaymentCalculator(), new EqualPrincipalCalculator(), new BulletRepaymentCalculator()),
				clock);
		settingService.update(SettingService.ROUNDS, "3", actor);
	}

	@Test
	void registrationCreatesScheduleAndFormattedMessage() {
		Loan loan = register();

		assertThat(loan.totalScheduledAmount()).isEqualTo(1_020_067);
		assertThat(formatter.registered(loan))
				.contains("🏦 대출 등록 완료", "#1001 · 하나 님", "✦ 대출금　　1,000,000원", "✦ 총 상환 예정액　1,020,067원");
	}

	@Test
	void sameCustomerCannotRegisterUntilPaidOff() {
		register();

		assertThatThrownBy(this::register).isInstanceOf(DuplicateLoanException.class);

		service.repay(new RepayLoanRequest("1001", "하나", 1_020_067, actor));
		assertThat(register().isCompleted()).isFalse();
	}

	@Test
	void repaymentMovesThroughInstallmentsAndCompletes() {
		register();

		RepaymentResult first = service.repay(new RepayLoanRequest("1001", "하나", 340_022, actor));
		assertThat(first.appliedSummary()).isEqualTo("1회차 납부 완료");
		assertThat(first.loan().remainingAmount()).isEqualTo(680_045);

		RepaymentResult partial = service.repay(new RepayLoanRequest("1001", "하나", 10, actor));
		assertThat(partial.appliedSummary()).isEqualTo("2회차 일부 납부");

		RepaymentResult last = service.repay(new RepayLoanRequest("1001", "하나", 680_035, actor));
		assertThat(last.appliedSummary()).isEqualTo("2~3회차 납부 완료");
		assertThat(last.status()).isEqualTo(LoanStatus.PAID);
		assertThat(events.findRecentByCustomerId("1001", 1).get(0).type()).isEqualTo(LoanEventType.COMPLETED);
	}

	@Test
	void repaymentRejectsWrongNameAndTooMuchMoney() {
		register();

		assertThatThrownBy(() -> service.repay(new RepayLoanRequest("1001", "다른사람", 1, actor)))
				.isInstanceOf(LoanException.class);
		assertThatThrownBy(() -> service.repay(new RepayLoanRequest("1001", "하나", 1_020_068, actor)))
				.isInstanceOf(LoanException.class);
	}

	@Test
	void overdueInterestIsAddedOncePerCycleAndPaidFirst() {
		register();

		clock.advance(Duration.ofDays(2));
		overdueService.applyOverdueInterest(clock.instant());
		overdueService.applyOverdueInterest(clock.instant());

		Loan overdue = service.findDetail("1001").loan();
		assertThat(overdue.installments().get(0).overdueInterest()).isEqualTo(3_400);
		assertThat(service.findDetail("1001").status()).isEqualTo(LoanStatus.OVERDUE);

		List<LoanEvent> recent = events.findRecentByCustomerId("1001", 10);
		assertThat(recent).extracting(LoanEvent::type)
				.contains(LoanEventType.OVERDUE_STARTED, LoanEventType.OVERDUE_INTEREST_ADDED);

		RepaymentResult result = service.repay(new RepayLoanRequest("1001", "하나", 3_400, actor));
		assertThat(result.loan().installments().get(0).unpaidOverdueInterest()).isZero();
		assertThat(result.appliedSummary()).isEqualTo("1회차 일부 납부");
	}

	@Test
	void settingsValidateRangesAndChangeNewLoansOnly() {
		Loan before = register();

		settingService.update(SettingService.RATE, "2%", actor);
		assertThat(service.findDetail("1001").loan().ratePerRound()).isEqualByComparingTo(before.ratePerRound());
		assertThat(settingService.get().ratePerRound()).isEqualByComparingTo("2");

		settingService.update(SettingService.CYCLE, "1분", actor);
		assertThatThrownBy(() -> settingService.update(SettingService.RATE, "31", actor))
				.isInstanceOf(LoanException.class);
		assertThatThrownBy(() -> settingService.update(SettingService.CYCLE, "13개월", actor))
				.isInstanceOf(LoanException.class);
		assertThatThrownBy(() -> settingService.update(SettingService.ROUNDS, "61", actor))
				.isInstanceOf(LoanException.class);
	}

	@Test
	void listGroupsLoansByStatusAndUsesMessageFormat() {
		register();

		List<String> messages = formatter.list(service.findAllGroupedByStatus());

		assertThat(messages.get(0)).startsWith("📋 대출 목록").contains("🟡 납부 임박", "#1001 하나");
	}

	private Loan register() {
		return service.register(new RegisterLoanRequest("1001", "하나", 1_000_000, RepaymentMethod.EQUAL_PAYMENT, actor));
	}

	private static final class MutableClock extends Clock {

		private Instant instant;

		private MutableClock(Instant instant) {
			this.instant = instant;
		}

		private void advance(Duration duration) {
			instant = instant.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneId.of("UTC");
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}
}
