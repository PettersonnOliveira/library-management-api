package com.portfolio.Library.Management.Controllers;


import com.portfolio.Library.Management.DTO.UserRequestDTO;
import com.portfolio.Library.Management.DTO.UserResponseDTO;
import com.portfolio.Library.Management.Services.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRequestDTO dadosEntrada) {
        UserResponseDTO userResponseDTO = userService.createUser(dadosEntrada);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponseDTO);
    }
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> listAll() {
        List<UserResponseDTO> listDtoResponse = userService.listAll();
        return ResponseEntity.ok(listDtoResponse);
    }
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> findById(@PathVariable Long id) {
        UserResponseDTO userResponseDTO = userService.findById(id);
        return ResponseEntity.ok(userResponseDTO);
    }


}
