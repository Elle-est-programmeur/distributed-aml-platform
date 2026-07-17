package com.tradesentry.core.transaction;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService service;
    private final TransactionRepository repository;

    public TransactionController(TransactionService service, TransactionRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @PostMapping("")
    public ResponseEntity<Map<String, Object>> submit(@Valid @RequestBody TransactionRequest request) {
        UUID id = service.submit(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("transactionId", id, "status", "SUBMITTED"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> get(@PathVariable UUID id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
