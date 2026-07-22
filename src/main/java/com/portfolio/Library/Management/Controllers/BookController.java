package com.portfolio.Library.Management.Controllers;

import com.portfolio.Library.Management.DTO.BookRequestDTO;
import com.portfolio.Library.Management.DTO.BookResponseDTO;
import com.portfolio.Library.Management.DTO.BookUpdateDTO;
import com.portfolio.Library.Management.DTO.UserResponseDTO;
import com.portfolio.Library.Management.Repositories.BookRepository;
import com.portfolio.Library.Management.Services.BookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/books")
public class BookController {
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }


    @PostMapping
    public ResponseEntity<BookResponseDTO> createBook(@Valid @RequestBody BookRequestDTO dadosEntrada) {
        BookResponseDTO BookResponseDTO = bookService.createBook(dadosEntrada);
        return ResponseEntity.status(HttpStatus.CREATED).body(BookResponseDTO);
    }

    @GetMapping
    public ResponseEntity<List<BookResponseDTO>> listAll() {
        List<BookResponseDTO> listDtoResponse = bookService.listAll();
        return ResponseEntity.ok(listDtoResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponseDTO> findById(@PathVariable Long id) {
        BookResponseDTO responseDTO = bookService.findById(id);
        return ResponseEntity.ok(responseDTO);
    }
    @PutMapping("/{id}")
    public ResponseEntity<BookResponseDTO> update(@PathVariable Long id, @RequestBody BookUpdateDTO dadosUpdate){
        BookResponseDTO bookResponseDTO = bookService.update(id, dadosUpdate);
        return ResponseEntity.ok(bookResponseDTO);
    }
}
