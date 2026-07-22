package com.portfolio.Library.Management.DTO;

import com.portfolio.Library.Management.Entities.Book;
import com.portfolio.Library.Management.Entities.Loan;
import com.portfolio.Library.Management.Entities.User;

public record LoanResponseDTO(
        Long id,
        UserResponseDTO user,
        BookResponseDTO book,
        String loanDate,
        String returnDate
) {
    public LoanResponseDTO(Loan loan){
        this(loan.getId(),
                new UserResponseDTO(loan.getUser()),
                new BookResponseDTO(loan.getBook()),
                loan.getLoanDate().toString(),
                loan.getReturnDate() != null ? loan.getReturnDate().toString() : null);
    }
}
