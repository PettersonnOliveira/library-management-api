package com.portfolio.Library.Management.Controllers;

import com.portfolio.Library.Management.DTO.LoanRequestDTO;
import com.portfolio.Library.Management.DTO.LoanResponseDTO;
import com.portfolio.Library.Management.Services.LoanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/loans")
public class LoanController {
    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    public ResponseEntity<LoanResponseDTO> createLoan( @RequestBody LoanRequestDTO dadosEntrada){
        LoanResponseDTO loanResponseDTO = loanService.createLoan(dadosEntrada);
        return ResponseEntity.status(HttpStatus.CREATED).body(loanResponseDTO);
    }
    @GetMapping
    public ResponseEntity<List<LoanResponseDTO>> listAll(){
        List<LoanResponseDTO> listDtoResponse = loanService.listAll();
        return ResponseEntity.ok(listDtoResponse);
    }
    @GetMapping("/{id}")
    public ResponseEntity<LoanResponseDTO> findById(@PathVariable Long id){
        LoanResponseDTO loanResponseDTO = loanService.findById(id);
        return ResponseEntity.ok(loanResponseDTO);
    }
@PutMapping("/{id}/return")
    public ResponseEntity<Void> returnLoan(@PathVariable Long id){
        loanService.returnLoan(id);
        return ResponseEntity.noContent().build();
}
}
