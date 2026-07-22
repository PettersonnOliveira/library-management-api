package com.portfolio.Library.Management.Services;

import com.portfolio.Library.Management.DTO.BookRequestDTO;
import com.portfolio.Library.Management.DTO.BookResponseDTO;
import com.portfolio.Library.Management.DTO.BookUpdateDTO;
import com.portfolio.Library.Management.Entities.Book;
import com.portfolio.Library.Management.Exceptions.ResourceNotFoundException;
import com.portfolio.Library.Management.Repositories.BookRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Service
public class BookService {
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

   public BookResponseDTO createBook(@Valid BookRequestDTO dadosEntrada){
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
                .orElseThrow(() -> new ResourceNotFoundException("Book não encontrado"));
        return new BookResponseDTO(book);
    }

    public BookResponseDTO update(@Valid @PathVariable Long id, BookUpdateDTO dadosUpdate){
        Book book = bookRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Recurso não encontrado com o ID:" + id));

        book.setTitle(dadosUpdate.title());
        book.setAuthor(dadosUpdate.author());
        book.setAvailable(dadosUpdate.available());

        Book bookUpdate = bookRepository.save(book);
        return new BookResponseDTO(bookUpdate);

    }
}
