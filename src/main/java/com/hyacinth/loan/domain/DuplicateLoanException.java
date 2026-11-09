package com.hyacinth.loan.domain;

public class DuplicateLoanException extends LoanException {

	public DuplicateLoanException() {
		super("이미 대출 중입니다.");
	}
}
