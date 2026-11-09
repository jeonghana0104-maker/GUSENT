package com.hyacinth.loan.repository;

import com.hyacinth.loan.domain.LoanEvent;

import java.util.List;

public interface LoanEventRepository {

	void save(LoanEvent event);

	List<LoanEvent> findRecentByCustomerId(String customerId, int limit);
}
