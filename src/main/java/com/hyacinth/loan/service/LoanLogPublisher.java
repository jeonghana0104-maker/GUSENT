package com.hyacinth.loan.service;

import com.hyacinth.loan.domain.LoanLog;

public interface LoanLogPublisher {

	void publish(LoanLog log, String channelId);
}
