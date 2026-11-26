package com.portfolio.Library.Management.Services;

import com.portfolio.Library.Management.DTO.BookResponseDTO;
import com.portfolio.Library.Management.DTO.BookUpdateDTO;
import com.portfolio.Library.Management.DTO.LoanRequestDTO;
import com.portfolio.Library.Management.Entities.Book;
import com.portfolio.Library.Management.Entities.User;
import com.portfolio.Library.Management.Exceptions.BusinessRuleException;
import com.portfolio.Library.Management.Repositories.BookRepository;
import com.portfolio.Library.Management.Repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class BookServiceTest {

    @Autowired
    private BookService bookService;
    @Autowired
    private LoanService loanService;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private UserRepository userRepository;

    private Book persistBook() {
        Book book = new Book();
        book.setTitle("Título Original");
        book.setAuthor("Autor Original");
        book.setAvailable(true);
        return bookRepository.save(book);
    }

    @Test
    void updateChangesTitleAndAuthorWithoutTouchingAvailability() {
        Book book = persistBook();

        BookResponseDTO response = bookService.update(book.getId(),
                new BookUpdateDTO("Título Novo", "Autor Novo", null));

        assertThat(response.title()).isEqualTo("Título Novo");
        assertThat(response.author()).isEqualTo("Autor Novo");
        assertThat(response.available()).isTrue();
    }

    @Test
    void cannotMarkBookAsAvailableWhileItHasAnActiveLoan() {
        Book book = persistBook();
        User user = userRepository.save(new User("Maria", "maria@email.com", "hash-bcrypt"));
        loanService.createLoan(new LoanRequestDTO(null, book.getId()), user, false);

        assertThatThrownBy(() -> bookService.update(book.getId(),
                new BookUpdateDTO(book.getTitle(), book.getAuthor(), true)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("empréstimo ativo");
    }

    @Test
    void canChangeAvailabilityWhenThereIsNoActiveLoan() {
        Book book = persistBook();

        BookResponseDTO response = bookService.update(book.getId(),
                new BookUpdateDTO(book.getTitle(), book.getAuthor(), false));

        assertThat(response.available()).isFalse();
    }
}
