package com.portfolio.Library.Management.DTO;

import com.portfolio.Library.Management.Entities.Book;

public record BookResponseDTO(
        Long id,
        String title,
        String author,
        Boolean available
) {
    public BookResponseDTO(Book book) {
        this(book.getId(), book.getTitle(), book.getAuthor(), book.isAvailable());
    }
}
