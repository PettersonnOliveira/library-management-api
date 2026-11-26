package com.portfolio.Library.Management.Services;

import com.portfolio.Library.Management.DTO.UserRequestDTO;
import com.portfolio.Library.Management.DTO.UserResponseDTO;
import com.portfolio.Library.Management.Entities.User;
import com.portfolio.Library.Management.Exceptions.ConflictException;
import com.portfolio.Library.Management.Exceptions.ResourceNotFoundException;
import com.portfolio.Library.Management.Repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponseDTO createUser(UserRequestDTO dadosEntrada) {
        if (userRepository.existsByEmail(dadosEntrada.email())) {
            throw new ConflictException("E-mail já cadastrado");
        }
        User user = new User(dadosEntrada.name(), dadosEntrada.email(),
                passwordEncoder.encode(dadosEntrada.password()));
        User userSalved = userRepository.save(user);
        return new UserResponseDTO(userSalved);
    }

    public List<UserResponseDTO> listAll() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(UserResponseDTO::new)
                .toList();
    }

    public UserResponseDTO findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        return new UserResponseDTO(user);
    }
}
