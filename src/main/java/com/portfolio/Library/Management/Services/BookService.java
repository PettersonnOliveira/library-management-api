package com.portfolio.Library.Management.Services;

import com.portfolio.Library.Management.DTO.BookRequestDTO;
import com.portfolio.Library.Management.DTO.BookResponseDTO;
import com.portfolio.Library.Management.DTO.BookUpdateDTO;
import com.portfolio.Library.Management.Entities.Book;
import com.portfolio.Library.Management.Exceptions.BusinessRuleException;
import com.portfolio.Library.Management.Exceptions.ResourceNotFoundException;
import com.portfolio.Library.Management.Repositories.BookRepository;
import com.portfolio.Library.Management.Repositories.LoanRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;

    public BookService(BookRepository bookRepository, LoanRepository loanRepository) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
    }

    public BookResponseDTO createBook(BookRequestDTO dadosEntrada) {
        Book book = new Book(dadosEntrada);
        Book bookSalved = bookRepository.save(book);
        return new BookResponseDTO(bookSalved);
    }

    public List<BookResponseDTO> listAll() {
        List<Book> books = bookRepository.findAll();
        return books.stream()
                .map(BookResponseDTO::new)
                .toList();
    }

    public BookResponseDTO findById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado"));
        return new BookResponseDTO(book);
    }

    public BookResponseDTO update(Long id, BookUpdateDTO dadosUpdate) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurso não encontrado com o ID:" + id));

        if (dadosUpdate.available() != null && dadosUpdate.available() != book.isAvailable()) {
            if (loanRepository.existsByBookAndReturnDateIsNull(book)) {
                throw new BusinessRuleException(
                        "Livro possui empréstimo ativo; devolva antes de alterar a disponibilidade");
            }
            book.setAvailable(dadosUpdate.available());
        }

        book.setTitle(dadosUpdate.title());
        book.setAuthor(dadosUpdate.author());

        Book bookUpdate = bookRepository.save(book);
        return new BookResponseDTO(bookUpdate);
    }
}
