package com.hyacinth.loan.domain;

/** 사용자에게 그대로 안내할 수 있는 대출 처리 거부 사유. */
public class LoanException extends RuntimeException {

	public LoanException(String message) {
		super(message);
	}
}
