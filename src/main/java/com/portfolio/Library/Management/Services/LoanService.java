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
import org.springframework.stereotype.Service;

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

    public LoanResponseDTO createLoan(LoanRequestDTO dadosEntrada) {
        User user = userRepository.findById(dadosEntrada.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Book book = bookRepository.findById(dadosEntrada.bookId())
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
    public List<LoanResponseDTO> listAll(){
        List<Loan> loans = loanRepository.findAll();
        return loans.stream()
                .map(LoanResponseDTO::new)
                .toList();
    }

    public LoanResponseDTO findById(Long id){
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empréstimo não encontrado"));
        return new LoanResponseDTO(loan);

    }
    public void returnLoan(Long id){
        Loan loan = loanRepository.findById(id)
                .orElseThrow( () -> new ResourceNotFoundException("Empréstimo não encontrado"));
        if (loan.getReturnDate() != null) {
            throw new BusinessRuleException("Empréstimo já foi devolvido");
        }
        loan.setReturnDate(LocalDateTime.now());
        loanRepository.save(loan);
        Book book = loan.getBook();
        book.setAvailable(true);
        bookRepository.save(book);
    }
}
