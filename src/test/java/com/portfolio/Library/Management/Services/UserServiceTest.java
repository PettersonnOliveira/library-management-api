package com.portfolio.Library.Management.Services;

import com.portfolio.Library.Management.DTO.UserRequestDTO;
import com.portfolio.Library.Management.DTO.UserResponseDTO;
import com.portfolio.Library.Management.Entities.User;
import com.portfolio.Library.Management.Exceptions.ConflictException;
import com.portfolio.Library.Management.Repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    @Test
    void createsUserWithEncodedPassword() {
        UserResponseDTO response = userService.createUser(
                new UserRequestDTO("Maria", "maria@email.com", "senha-forte-123"));

        User saved = userRepository.findById(response.id()).orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo("senha-forte-123");
        assertThat(saved.getPassword()).startsWith("$2");
        assertThat(saved.getRole().name()).isEqualTo("USER");
    }

    @Test
    void rejectsDuplicateEmail() {
        UserRequestDTO request = new UserRequestDTO("Maria", "maria@email.com", "senha-forte-123");
        userService.createUser(request);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("E-mail já cadastrado");
    }
}
