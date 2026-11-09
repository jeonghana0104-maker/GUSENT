package com.hyacinth.infra.memory;

import com.hyacinth.loan.domain.LoanLog;
import com.hyacinth.loan.repository.LoanLogRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class MemoryLoanLogRepository implements LoanLogRepository {

	private final List<LoanLog> logs = new ArrayList<>();

	@Override
	public synchronized void save(LoanLog log) {
		logs.add(log);
	}
}
