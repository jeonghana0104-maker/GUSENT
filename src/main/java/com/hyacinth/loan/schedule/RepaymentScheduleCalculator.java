package com.hyacinth.loan.schedule;

import com.hyacinth.loan.domain.Installment;
import com.hyacinth.loan.domain.RepaymentMethod;

import java.util.List;

public interface RepaymentScheduleCalculator {

	RepaymentMethod method();

	List<Installment> calculate(ScheduleRequest request);
}
