package com.hyacinth.infra.memory;

import com.hyacinth.loan.domain.LoanEvent;
import com.hyacinth.loan.repository.LoanEventRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class MemoryLoanEventRepository implements LoanEventRepository {

	private final List<LoanEvent> events = new ArrayList<>();

	@Override
	public synchronized void save(LoanEvent event) {
		events.add(event);
	}

	@Override
	public synchronized List<LoanEvent> findRecentByCustomerId(String customerId, int limit) {
		List<LoanEvent> recent = new ArrayList<>();
		for (int index = events.size() - 1; index >= 0 && recent.size() < limit; index--) {
			if (events.get(index).customerId().equals(customerId)) {
				recent.add(events.get(index));
			}
		}
		return recent;
	}
}
