package com.portfolio.Library.Management.DTO;

import com.portfolio.Library.Management.Entities.User;

public record UserResponseDTO(
        Long id,
        String name,
        String email
) {
    public UserResponseDTO (User user) {
        this(user.getId(), user.getName(), user.getEmail());
    }
}
