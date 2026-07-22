package com.portfolio.Library.Management.Repositories;

import com.portfolio.Library.Management.Entities.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
}
