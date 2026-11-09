package com.hyacinth.loan.schedule;

import com.hyacinth.loan.domain.Installment;
import com.hyacinth.loan.domain.RepaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class EqualPaymentCalculator implements RepaymentScheduleCalculator {

	@Override
	public RepaymentMethod method() {
		return RepaymentMethod.EQUAL_PAYMENT;
	}

	@Override
	public List<Installment> calculate(ScheduleRequest request) {
		BigDecimal rate = ScheduleMath.fraction(request.ratePerRound());
		long payment = payment(request.principal(), rate, request.rounds());
		List<Installment> installments = new ArrayList<>();
		long balance = request.principal();

		for (int round = 1; round <= request.rounds(); round++) {
			long interest = ScheduleMath.interest(balance, rate);
			long principal = round == request.rounds()
					? balance
					: Math.min(balance, Math.max(0, payment - interest));
			installments.add(ScheduleMath.installment(request, round, principal, interest));
			balance -= principal;
		}
		return installments;
	}

	private long payment(long principal, BigDecimal rate, int rounds) {
		if (rate.signum() == 0) {
			return principal / rounds;
		}
		BigDecimal factor = BigDecimal.ONE.add(rate).pow(rounds, ScheduleMath.CONTEXT);
		BigDecimal numerator = BigDecimal.valueOf(principal).multiply(rate, ScheduleMath.CONTEXT)
				.multiply(factor, ScheduleMath.CONTEXT);
		return ScheduleMath.round(numerator.divide(factor.subtract(BigDecimal.ONE), ScheduleMath.CONTEXT));
	}
}
