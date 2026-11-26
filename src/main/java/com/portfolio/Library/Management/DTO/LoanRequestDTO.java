package com.portfolio.Library.Management.DTO;

import jakarta.validation.constraints.NotNull;

public record LoanRequestDTO(
        Long userId,
        @NotNull Long bookId
) {
}
