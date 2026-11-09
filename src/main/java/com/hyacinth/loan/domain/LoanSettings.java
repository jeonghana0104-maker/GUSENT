package com.hyacinth.loan.domain;

import java.math.BigDecimal;
import java.time.Duration;

/** 금리와 연체 이자율은 퍼센트 값으로 저장한다. */
public record LoanSettings(
		BigDecimal ratePerRound,
		BigDecimal overdueRatePerRound,
		int rounds,
		PaymentCycle cycle,
		Duration dueSoonThreshold,
		long maxLoanAmount,
		String logChannelId
) {
}
