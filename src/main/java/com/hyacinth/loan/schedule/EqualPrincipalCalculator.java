package com.hyacinth.loan.schedule;

import com.hyacinth.loan.domain.Installment;
import com.hyacinth.loan.domain.RepaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class EqualPrincipalCalculator implements RepaymentScheduleCalculator {

	@Override
	public RepaymentMethod method() {
		return RepaymentMethod.EQUAL_PRINCIPAL;
	}

	@Override
	public List<Installment> calculate(ScheduleRequest request) {
		BigDecimal rate = ScheduleMath.fraction(request.ratePerRound());
		long basePrincipal = request.principal() / request.rounds();
		List<Installment> installments = new ArrayList<>();
		long balance = request.principal();

		for (int round = 1; round <= request.rounds(); round++) {
			long interest = ScheduleMath.interest(balance, rate);
			long principal = round == request.rounds() ? balance : basePrincipal;
			installments.add(ScheduleMath.installment(request, round, principal, interest));
			balance -= principal;
		}
		return installments;
	}
}
