package com.portfolio.Library.Management.Services;

import com.portfolio.Library.Management.DTO.UserRequestDTO;
import com.portfolio.Library.Management.DTO.UserResponseDTO;
import com.portfolio.Library.Management.Entities.User;
import com.portfolio.Library.Management.Exceptions.ResourceNotFoundException;
import com.portfolio.Library.Management.Repositories.UserRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponseDTO createUser( @Valid UserRequestDTO dadosEntrada) {
        User user = new User(dadosEntrada);
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
                .orElseThrow(() -> new ResourceNotFoundException("User não encontrado"));
        return new UserResponseDTO(user);
    }
}
