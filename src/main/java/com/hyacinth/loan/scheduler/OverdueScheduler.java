package com.hyacinth.loan.scheduler;

import com.hyacinth.loan.service.OverdueService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class OverdueScheduler {

	private final OverdueService overdueService;
	private final Clock clock;

	public OverdueScheduler(OverdueService overdueService, Clock clock) {
		this.overdueService = overdueService;
		this.clock = clock;
	}

	@Scheduled(fixedDelay = 60_000)
	public void run() {
		overdueService.applyOverdueInterest(clock.instant());
	}
}
