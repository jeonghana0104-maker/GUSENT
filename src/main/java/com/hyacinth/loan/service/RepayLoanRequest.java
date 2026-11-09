package com.hyacinth.loan.service;

import com.hyacinth.loan.domain.Actor;

public record RepayLoanRequest(
		String customerId,
		String userName,
		long amount,
		Actor actor
) {
}
