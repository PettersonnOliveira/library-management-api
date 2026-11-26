package com.portfolio.Library.Management.Services;

import com.portfolio.Library.Management.DTO.LoanRequestDTO;
import com.portfolio.Library.Management.DTO.LoanResponseDTO;
import com.portfolio.Library.Management.Entities.Book;
import com.portfolio.Library.Management.Entities.Loan;
import com.portfolio.Library.Management.Entities.User;
import com.portfolio.Library.Management.Exceptions.BusinessRuleException;
import com.portfolio.Library.Management.Exceptions.ResourceNotFoundException;
import com.portfolio.Library.Management.Repositories.BookRepository;
import com.portfolio.Library.Management.Repositories.LoanRepository;
import com.portfolio.Library.Management.Repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class LoanServiceTest {

    @Autowired
    private LoanService loanService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private LoanRepository loanRepository;

    private User persistUser(String email) {
        return userRepository.save(new User("Usuário " + email, email, "hash-bcrypt"));
    }

    private Book persistBook(String title) {
        return persistBook(title, true);
    }

    private Book persistBook(String title, boolean available) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor("Autor de Teste");
        book.setAvailable(available);
        return bookRepository.save(book);
    }

    @Test
    void createLoanMarksBookAsUnavailable() {
        User user = persistUser("maria@email.com");
        Book book = persistBook("Dom Casmurro");

        LoanResponseDTO response = loanService.createLoan(
                new LoanRequestDTO(null, book.getId()), user, false);

        assertThat(response.loanDate()).isNotBlank();
        assertThat(response.returnDate()).isNull();
        assertThat(response.book().available()).isFalse();
        assertThat(bookRepository.findById(book.getId()).orElseThrow().isAvailable()).isFalse();
    }

    @Test
    void rejectsLoanWhenBookIsUnavailable() {
        User user = persistUser("maria@email.com");
        Book book = persistBook("Memórias Póstumas", false);

        assertThatThrownBy(() -> loanService.createLoan(
                new LoanRequestDTO(null, book.getId()), user, false))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("não disponível");
    }

    @Test
    void rejectsLoanWhenBookDoesNotExist() {
        User user = persistUser("maria@email.com");

        assertThatThrownBy(() -> loanService.createLoan(
                new LoanRequestDTO(null, 999_999L), user, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void adminMustProvideUserIdToCreateLoan() {
        User admin = persistUser("admin@email.com");
        Book book = persistBook("Dom Casmurro");

        assertThatThrownBy(() -> loanService.createLoan(
                new LoanRequestDTO(null, book.getId()), admin, true))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("userId");
    }

    @Test
    void adminCanCreateLoanForAnotherUser() {
        User admin = persistUser("admin@email.com");
        User leitor = persistUser("leitor@email.com");
        Book book = persistBook("Dom Casmurro");

        LoanResponseDTO response = loanService.createLoan(
                new LoanRequestDTO(leitor.getId(), book.getId()), admin, true);

        assertThat(response.user().id()).isEqualTo(leitor.getId());
    }

    @Test
    void regularUserCannotCreateLoanForAnotherUser() {
        User maria = persistUser("maria@email.com");
        User joao = persistUser("joao@email.com");
        Book book = persistBook("Dom Casmurro");

        LoanResponseDTO response = loanService.createLoan(
                new LoanRequestDTO(joao.getId(), book.getId()), maria, false);

        assertThat(response.user().id()).isEqualTo(maria.getId());
    }

    @Test
    void returnLoanSetsReturnDateAndMakesBookAvailable() {
        User user = persistUser("maria@email.com");
        Book book = persistBook("Dom Casmurro");
        LoanResponseDTO loan = loanService.createLoan(
                new LoanRequestDTO(null, book.getId()), user, false);

        loanService.returnLoan(loan.id(), user, false);

        Loan saved = loanRepository.findById(loan.id()).orElseThrow();
        assertThat(saved.getReturnDate()).isNotNull();
        assertThat(bookRepository.findById(book.getId()).orElseThrow().isAvailable()).isTrue();
    }

    @Test
    void rejectsReturningAnAlreadyReturnedLoan() {
        User user = persistUser("maria@email.com");
        Book book = persistBook("Dom Casmurro");
        LoanResponseDTO loan = loanService.createLoan(
                new LoanRequestDTO(null, book.getId()), user, false);

        loanService.returnLoan(loan.id(), user, false);

        assertThatThrownBy(() -> loanService.returnLoan(loan.id(), user, false))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("já foi devolvido");
    }

    @Test
    void userCannotReadLoanFromAnotherUser() {
        User maria = persistUser("maria@email.com");
        User joao = persistUser("joao@email.com");
        Book book = persistBook("Dom Casmurro");
        LoanResponseDTO loan = loanService.createLoan(
                new LoanRequestDTO(null, book.getId()), maria, false);

        assertThatThrownBy(() -> loanService.findById(loan.id(), joao, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminCanReadAnyLoan() {
        User maria = persistUser("maria@email.com");
        User admin = persistUser("admin@email.com");
        Book book = persistBook("Dom Casmurro");
        LoanResponseDTO loan = loanService.createLoan(
                new LoanRequestDTO(null, book.getId()), maria, false);

        assertThat(loanService.findById(loan.id(), admin, true).id()).isEqualTo(loan.id());
    }
}
