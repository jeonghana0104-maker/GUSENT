package com.hyacinth.loan.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record Loan(
		String loanId,
		String customerId,
		String userName,
		long principal,
		RepaymentMethod method,
		BigDecimal ratePerRound,
		int rounds,
		PaymentCycle cycle,
		Instant createdAt,
		Instant completedAt,
		Instant nextDueAt,
		List<Installment> installments
) {

	public Loan {
		installments = List.copyOf(installments);
	}

	public boolean isCompleted() {
		return completedAt != null;
	}

	public long remainingAmount() {
		return installments.stream().mapToLong(Installment::totalUnpaid).sum();
	}

	public long totalScheduledAmount() {
		return installments.stream().mapToLong(Installment::amountDue).sum();
	}

	public Optional<Installment> nextUnpaid() {
		return installments.stream().filter(installment -> !installment.isSettled()).findFirst();
	}

	public Loan withInstallments(List<Installment> updated, Instant now) {
		Instant next = updated.stream()
				.filter(installment -> !installment.isSettled())
				.map(Installment::dueAt)
				.findFirst()
				.orElse(null);
		Instant completed = next == null ? (completedAt == null ? now : completedAt) : null;
		return new Loan(loanId, customerId, userName, principal, method, ratePerRound, rounds, cycle,
				createdAt, completed, next, updated);
	}
}
