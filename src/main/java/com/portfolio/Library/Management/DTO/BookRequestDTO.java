package com.portfolio.Library.Management.DTO;

import jakarta.validation.constraints.NotBlank;

public record BookRequestDTO(
       @NotBlank String title,
       @NotBlank String author
) {
}
