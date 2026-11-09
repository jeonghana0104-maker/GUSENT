package com.hyacinth.loan.service;

import com.hyacinth.loan.domain.Loan;
import com.hyacinth.loan.domain.LoanStatus;

public record RepaymentResult(
		Loan loan,
		long paidAmount,
		String appliedSummary,
		LoanStatus status
) {
}
