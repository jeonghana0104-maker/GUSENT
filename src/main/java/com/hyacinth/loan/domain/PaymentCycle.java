package com.hyacinth.loan.domain;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public record PaymentCycle(long amount, ChronoUnit unit) {

	public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	public Instant addTo(Instant start, int times) {
		return start.atZone(ZONE).plus(amount * times, unit).toInstant();
	}
}
