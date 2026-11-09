package com.hyacinth.loan.service;

import com.hyacinth.loan.domain.LoanEvent;
import com.hyacinth.loan.repository.LoanEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoanEventService {

	private final LoanEventRepository repository;

	public LoanEventService(LoanEventRepository repository) {
		this.repository = repository;
	}

	public void record(LoanEvent event) {
		repository.save(event);
	}

	public List<LoanEvent> findRecent(String customerId, int limit) {
		return repository.findRecentByCustomerId(customerId, limit);
	}
}
