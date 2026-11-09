package com.hyacinth.infra.memory;

import com.hyacinth.loan.domain.LoanSettings;
import com.hyacinth.loan.domain.PaymentCycle;
import com.hyacinth.loan.repository.SettingRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Repository
public class MemorySettingRepository implements SettingRepository {

	private LoanSettings settings = new LoanSettings(
			BigDecimal.ONE,
			BigDecimal.ONE,
			12,
			new PaymentCycle(1, ChronoUnit.DAYS),
			Duration.ofHours(24),
			100_000_000L,
			null);

	@Override
	public synchronized LoanSettings get() {
		return settings;
	}

	@Override
	public synchronized void save(LoanSettings settings) {
		this.settings = settings;
	}
}
