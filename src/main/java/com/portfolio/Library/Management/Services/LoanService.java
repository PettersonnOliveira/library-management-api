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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoanService {
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public LoanService(LoanRepository loanRepository, UserRepository userRepository, BookRepository bookRepository) {
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional
    public LoanResponseDTO createLoan(LoanRequestDTO dadosEntrada, User currentUser, boolean admin) {
        User user;
        if (admin) {
            if (dadosEntrada.userId() == null) {
                throw new BusinessRuleException("userId é obrigatório para ADMIN");
            }
            user = userRepository.findById(dadosEntrada.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        } else {
            user = currentUser;
        }

        // Lock pessimista evita que duas requisições emprestem o mesmo livro ao mesmo tempo.
        Book book = bookRepository.findByIdForUpdate(dadosEntrada.bookId())
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado"));

        if (!book.isAvailable()) {
            throw new BusinessRuleException("Livro não disponível para empréstimo");
        }

        Loan loan = new Loan(user, book);
        book.setAvailable(false);
        bookRepository.save(book);
        Loan loanSalved = loanRepository.save(loan);
        return new LoanResponseDTO(loanSalved);
    }

    public List<LoanResponseDTO> listAll(User currentUser, boolean admin) {
        List<Loan> loans = admin ? loanRepository.findAll() : loanRepository.findByUser(currentUser);
        return loans.stream()
                .map(LoanResponseDTO::new)
                .toList();
    }

    public LoanResponseDTO findById(Long id, User currentUser, boolean admin) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empréstimo não encontrado"));
        checkOwnership(loan, currentUser, admin);
        return new LoanResponseDTO(loan);
    }

    @Transactional
    public void returnLoan(Long id, User currentUser, boolean admin) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empréstimo não encontrado"));
        checkOwnership(loan, currentUser, admin);
        if (loan.getReturnDate() != null) {
            throw new BusinessRuleException("Empréstimo já foi devolvido");
        }
        loan.setReturnDate(LocalDateTime.now());
        loanRepository.save(loan);
        Book book = loan.getBook();
        book.setAvailable(true);
        bookRepository.save(book);
    }

    private void checkOwnership(Loan loan, User currentUser, boolean admin) {
        if (!admin && !loan.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Acesso negado: empréstimo pertence a outro usuário");
        }
    }
}
