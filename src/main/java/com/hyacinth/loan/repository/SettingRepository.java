package com.hyacinth.loan.repository;

import com.hyacinth.loan.domain.LoanSettings;

public interface SettingRepository {

	LoanSettings get();

	void save(LoanSettings settings);
}
