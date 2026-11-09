package com.hyacinth.loan.service;

import com.hyacinth.loan.domain.LoanLog;
import com.hyacinth.loan.repository.LoanLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoanLogService {

	private static final Logger log = LoggerFactory.getLogger(LoanLogService.class);

	private final LoanLogRepository repository;
	private final SettingService settingService;
	private final LoanLogPublisher publisher;

	public LoanLogService(LoanLogRepository repository, SettingService settingService, LoanLogPublisher publisher) {
		this.repository = repository;
		this.settingService = settingService;
		this.publisher = publisher;
	}

	public void record(LoanLog loanLog) {
		repository.save(loanLog);
		String channelId = settingService.get().logChannelId();
		if (channelId == null) {
			return;
		}
		try {
			publisher.publish(loanLog, channelId);
		}
		catch (RuntimeException ex) {
			log.warn("Failed to publish a loan log to channel {}", channelId, ex);
		}
	}
}
