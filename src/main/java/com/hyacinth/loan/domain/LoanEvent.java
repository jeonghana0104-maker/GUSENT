package com.hyacinth.loan.domain;

import java.time.Instant;

public record LoanEvent(
		String loanId,
		String customerId,
		String userName,
		LoanEventType type,
		long amount,
		long remainingAmount,
		Instant occurredAt
) {
}
