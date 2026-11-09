package com.hyacinth.loan.domain;

import java.time.Instant;

public record Installment(
		int round,
		Instant dueAt,
		long principal,
		long interest,
		long paidAmount,
		long overdueInterest,
		long paidOverdueInterest,
		Instant lastOverdueAppliedAt
) {

	public long amountDue() {
		return principal + interest;
	}

	public long unpaidAmount() {
		return amountDue() - paidAmount;
	}

	public long unpaidOverdueInterest() {
		return overdueInterest - paidOverdueInterest;
	}

	public long totalUnpaid() {
		return unpaidAmount() + unpaidOverdueInterest();
	}

	public boolean isSettled() {
		return totalUnpaid() == 0;
	}

	public Installment withPayment(long overduePayment, long amountPayment) {
		return new Installment(round, dueAt, principal, interest,
				paidAmount + amountPayment,
				overdueInterest,
				paidOverdueInterest + overduePayment,
				lastOverdueAppliedAt);
	}

	public Installment withOverdue(long addedInterest, Instant appliedAt) {
		return new Installment(round, dueAt, principal, interest, paidAmount,
				overdueInterest + addedInterest,
				paidOverdueInterest,
				appliedAt);
	}
}
