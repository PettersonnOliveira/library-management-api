package com.portfolio.Library.Management.Controllers;

import com.portfolio.Library.Management.DTO.LoanRequestDTO;
import com.portfolio.Library.Management.DTO.LoanResponseDTO;
import com.portfolio.Library.Management.Entities.User;
import com.portfolio.Library.Management.Repositories.UserRepository;
import com.portfolio.Library.Management.Services.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/loans")
public class LoanController {
    private final LoanService loanService;
    private final UserRepository userRepository;

    public LoanController(LoanService loanService, UserRepository userRepository) {
        this.loanService = loanService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<LoanResponseDTO> createLoan(@Valid @RequestBody LoanRequestDTO dadosEntrada,
                                                       @AuthenticationPrincipal UserDetails principal) {
        LoanResponseDTO loanResponseDTO = loanService.createLoan(
                dadosEntrada, currentUser(principal), isAdmin(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(loanResponseDTO);
    }
    @GetMapping
    public ResponseEntity<List<LoanResponseDTO>> listAll(@AuthenticationPrincipal UserDetails principal){
        List<LoanResponseDTO> listDtoResponse = loanService.listAll(
                currentUser(principal), isAdmin(principal));
        return ResponseEntity.ok(listDtoResponse);
    }
    @GetMapping("/{id}")
    public ResponseEntity<LoanResponseDTO> findById(@PathVariable Long id,
                                                     @AuthenticationPrincipal UserDetails principal){
        LoanResponseDTO loanResponseDTO = loanService.findById(
                id, currentUser(principal), isAdmin(principal));
        return ResponseEntity.ok(loanResponseDTO);
    }
@PutMapping("/{id}/return")
    public ResponseEntity<Void> returnLoan(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails principal){
        loanService.returnLoan(id, currentUser(principal), isAdmin(principal));
        return ResponseEntity.noContent().build();
}

    private User currentUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private boolean isAdmin(UserDetails principal) {
        return principal.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
