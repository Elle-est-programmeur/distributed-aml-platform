package com.tradesentry.core.transaction;

import com.tradesentry.core.events.config.KafkaTopics;
import com.tradesentry.core.events.model.TransactionEvent;
import com.tradesentry.core.events.producer.TransactionEventProducer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository repository;
    private final TransactionEventProducer producer;

    public TransactionService(TransactionRepository repository, TransactionEventProducer producer) {
        this.repository = repository;
        this.producer = producer;
    }

    @Transactional
    public UUID submit(TransactionRequest request) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Transaction transaction = new Transaction(
                id,
                request.accountId(),
                request.amount(),
                request.currency(),
                request.counterpartyCountry(),
                TransactionStatus.SUBMITTED,
                now,
                now);
        repository.save(transaction);

        TransactionEvent event = TransactionEvent.ingested(
                id,
                request.accountId(),
                request.amount(),
                request.currency(),
                request.counterpartyCountry());
        producer.publish(KafkaTopics.INGESTED, event);

        return id;
    }
}
