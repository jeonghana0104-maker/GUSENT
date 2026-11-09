package com.hyacinth.loan.domain;

import java.time.Instant;

public record LoanLog(
		Actor actor,
		String command,
		String input,
		boolean success,
		String result,
		Instant occurredAt
) {
}
