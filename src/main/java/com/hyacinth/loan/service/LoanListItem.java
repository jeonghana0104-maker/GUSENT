package com.hyacinth.loan.service;

import com.hyacinth.loan.domain.Loan;
import com.hyacinth.loan.domain.LoanStatus;

public record LoanListItem(Loan loan, LoanStatus status) {
}
