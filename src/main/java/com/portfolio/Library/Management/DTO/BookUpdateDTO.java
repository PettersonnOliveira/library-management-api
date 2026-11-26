package com.portfolio.Library.Management.DTO;

import jakarta.validation.constraints.NotBlank;

public record BookUpdateDTO(
        @NotBlank String title,
        @NotBlank String author,
        Boolean available
        ) {
}
