package com.hyacinth.loan.service;

import com.hyacinth.loan.domain.Actor;
import com.hyacinth.loan.domain.RepaymentMethod;

public record RegisterLoanRequest(
		String customerId,
		String userName,
		long amount,
		RepaymentMethod method,
		Actor actor
) {
}
