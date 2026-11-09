package com.hyacinth.loan.schedule;

import com.hyacinth.loan.domain.Installment;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

final class ScheduleMath {

	static final MathContext CONTEXT = MathContext.DECIMAL128;

	private ScheduleMath() {
	}

	static BigDecimal fraction(BigDecimal percent) {
		return percent.movePointLeft(2);
	}

	static long round(BigDecimal value) {
		return value.setScale(0, RoundingMode.HALF_UP).longValueExact();
	}

	static long interest(long balance, BigDecimal rate) {
		return round(BigDecimal.valueOf(balance).multiply(rate, CONTEXT));
	}

	static Installment installment(ScheduleRequest request, int round, long principal, long interest) {
		return new Installment(round, request.cycle().addTo(request.startAt(), round),
				principal, interest, 0, 0, 0, null);
	}
}
