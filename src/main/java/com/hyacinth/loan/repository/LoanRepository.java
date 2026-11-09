package com.hyacinth.loan.repository;

import com.hyacinth.loan.domain.Loan;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public interface LoanRepository {

	Optional<Loan> findByCustomerId(String customerId);

	List<Loan> findAll();

	List<Loan> findActiveDueBefore(Instant now);

	void createIfNoActiveLoan(Loan loan);

	Loan update(String customerId, UnaryOperator<Loan> change);
}
