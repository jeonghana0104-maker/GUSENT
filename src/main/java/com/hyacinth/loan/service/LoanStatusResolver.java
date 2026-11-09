package com.hyacinth.loan.service;

import com.hyacinth.loan.domain.Installment;
import com.hyacinth.loan.domain.Loan;
import com.hyacinth.loan.domain.LoanSettings;
import com.hyacinth.loan.domain.LoanStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class LoanStatusResolver {

	public LoanStatus resolve(Loan loan, Instant now, LoanSettings settings) {
		if (loan.isCompleted()) {
			return LoanStatus.PAID;
		}
		Optional<Installment> next = loan.nextUnpaid();
		if (next.isEmpty()) {
			return LoanStatus.PAID;
		}
		if (!next.get().dueAt().isAfter(now)) {
			return LoanStatus.OVERDUE;
		}
		if (!next.get().dueAt().isAfter(now.plus(settings.dueSoonThreshold()))) {
			return LoanStatus.DUE_SOON;
		}
		return LoanStatus.NORMAL;
	}
}
