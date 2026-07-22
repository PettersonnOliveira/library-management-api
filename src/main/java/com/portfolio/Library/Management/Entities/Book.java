package com.portfolio.Library.Management.Entities;

import com.portfolio.Library.Management.DTO.BookRequestDTO;
import com.portfolio.Library.Management.DTO.BookUpdateDTO;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "books")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String author;
    private boolean available;

    public Book(BookRequestDTO dadosEntrada){
        this.title = dadosEntrada.title();
        this.author = dadosEntrada.author();
        this.available = true; // Assuming new books are available by default
    }
    public Book(BookUpdateDTO dadosUpdate){
        this.title = dadosUpdate.title();
        this.author = dadosUpdate.author();
        this.available = dadosUpdate.available();
    }

    public Book() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        return Objects.equals(id, book.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
