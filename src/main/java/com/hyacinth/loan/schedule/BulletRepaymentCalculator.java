package com.hyacinth.loan.schedule;

import com.hyacinth.loan.domain.Installment;
import com.hyacinth.loan.domain.RepaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class BulletRepaymentCalculator implements RepaymentScheduleCalculator {

	@Override
	public RepaymentMethod method() {
		return RepaymentMethod.BULLET;
	}

	@Override
	public List<Installment> calculate(ScheduleRequest request) {
		BigDecimal rate = ScheduleMath.fraction(request.ratePerRound());
		long interest = ScheduleMath.interest(request.principal(), rate);
		List<Installment> installments = new ArrayList<>();

		for (int round = 1; round <= request.rounds(); round++) {
			long principal = round == request.rounds() ? request.principal() : 0;
			installments.add(ScheduleMath.installment(request, round, principal, interest));
		}
		return installments;
	}
}
