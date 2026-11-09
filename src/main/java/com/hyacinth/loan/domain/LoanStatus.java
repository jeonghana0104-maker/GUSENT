package com.hyacinth.loan.domain;

public enum LoanStatus {
	NORMAL("🟢", "정상"),
	DUE_SOON("🟡", "납부 임박"),
	OVERDUE("🔴", "연체"),
	PAID("🔵", "완납");

	private final String emoji;
	private final String label;

	LoanStatus(String emoji, String label) {
		this.emoji = emoji;
		this.label = label;
	}

	public String emoji() {
		return emoji;
	}

	public String label() {
		return label;
	}
}
