package com.portfolio.Library.Management.DTO;

import jakarta.validation.constraints.NotNull;

public record LoanRequestDTO(
        @NotNull Long userId,
        @NotNull Long bookId
) {
}
