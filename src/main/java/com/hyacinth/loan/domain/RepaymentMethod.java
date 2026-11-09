package com.hyacinth.loan.domain;

public enum RepaymentMethod {
	EQUAL_PAYMENT("원리금균등"),
	EQUAL_PRINCIPAL("원금균등"),
	BULLET("만기일시");

	private final String label;

	RepaymentMethod(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
