package com.hyacinth.loan.schedule;

import com.hyacinth.loan.domain.Installment;
import com.hyacinth.loan.domain.PaymentCycle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RepaymentScheduleCalculatorTest {

	private final ScheduleRequest request = new ScheduleRequest(1_000_000, BigDecimal.ONE, 3,
			new PaymentCycle(1, ChronoUnit.DAYS), Instant.parse("2026-11-09T00:00:00Z"));

	@Test
	void equalPaymentMatchesDesignExample() {
		assertThat(amounts(new EqualPaymentCalculator().calculate(request)))
				.containsExactly(340_022L, 340_022L, 340_023L);
	}

	@Test
	void equalPrincipalMatchesDesignExample() {
		assertThat(amounts(new EqualPrincipalCalculator().calculate(request)))
				.containsExactly(343_333L, 340_000L, 336_667L);
	}

	@Test
	void bulletMatchesDesignExample() {
		assertThat(amounts(new BulletRepaymentCalculator().calculate(request)))
				.containsExactly(10_000L, 10_000L, 1_010_000L);
	}

	@Test
	void principalAlwaysAddsUpWhenRateIsZero() {
		ScheduleRequest zeroRate = new ScheduleRequest(100, BigDecimal.ZERO, 3,
				new PaymentCycle(1, ChronoUnit.DAYS), request.startAt());

		assertThat(new EqualPaymentCalculator().calculate(zeroRate).stream().mapToLong(Installment::principal).sum())
				.isEqualTo(100);
		assertThat(new EqualPrincipalCalculator().calculate(zeroRate).stream().mapToLong(Installment::principal).sum())
				.isEqualTo(100);
	}

	private List<Long> amounts(List<Installment> installments) {
		return installments.stream().map(Installment::amountDue).toList();
	}
}
