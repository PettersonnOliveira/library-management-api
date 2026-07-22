package com.portfolio.Library.Management.Repositories;

import com.portfolio.Library.Management.Entities.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {
}
