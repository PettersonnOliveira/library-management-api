package com.portfolio.Library.Management.Repositories;

import com.portfolio.Library.Management.Entities.Book;
import com.portfolio.Library.Management.Entities.Loan;
import com.portfolio.Library.Management.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUser(User user);

    boolean existsByBookAndReturnDateIsNull(Book book);
}
