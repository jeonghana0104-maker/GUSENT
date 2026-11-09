package com.hyacinth.infra.memory;

import com.hyacinth.loan.domain.DuplicateLoanException;
import com.hyacinth.loan.domain.Loan;
import com.hyacinth.loan.domain.LoanException;
import com.hyacinth.loan.repository.LoanRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

@Repository
public class MemoryLoanRepository implements LoanRepository {

	private final Map<String, Loan> loans = new LinkedHashMap<>();
	private final List<Loan> archivedLoans = new ArrayList<>();

	@Override
	public synchronized Optional<Loan> findByCustomerId(String customerId) {
		return Optional.ofNullable(loans.get(customerId));
	}

	@Override
	public synchronized List<Loan> findAll() {
		return List.copyOf(loans.values());
	}

	@Override
	public synchronized List<Loan> findActiveDueBefore(Instant now) {
		return loans.values().stream()
				.filter(loan -> !loan.isCompleted())
				.filter(loan -> loan.nextDueAt() != null && !loan.nextDueAt().isAfter(now))
				.toList();
	}

	@Override
	public synchronized void createIfNoActiveLoan(Loan loan) {
		Loan existing = loans.get(loan.customerId());
		if (existing != null && !existing.isCompleted()) {
			throw new DuplicateLoanException();
		}
		if (existing != null) {
			archivedLoans.add(existing);
			loans.remove(loan.customerId());
		}
		loans.put(loan.customerId(), loan);
	}

	@Override
	public synchronized Loan update(String customerId, UnaryOperator<Loan> change) {
		Loan current = loans.get(customerId);
		if (current == null) {
			throw new LoanException("등록된 대출이 없습니다.");
		}
		Loan updated = change.apply(current);
		loans.put(customerId, updated);
		return updated;
	}
}
