package com.hyacinth.loan.schedule;

import com.hyacinth.loan.domain.PaymentCycle;

import java.math.BigDecimal;
import java.time.Instant;

public record ScheduleRequest(
		long principal,
		BigDecimal ratePerRound,
		int rounds,
		PaymentCycle cycle,
		Instant startAt
) {
}
