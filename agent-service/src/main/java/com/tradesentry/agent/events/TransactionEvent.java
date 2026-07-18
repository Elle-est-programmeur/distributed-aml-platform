package com.tradesentry.agent.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Agent-service's own copy of the transaction event. It mirrors core-service's record exactly in
 * component names, order, and types — but is a separate class on purpose: each service stays
 * independently deployable and they agree on the JSON contract, not on a shared jar.
 */
public record TransactionEvent(
        UUID transactionId,
        String accountId,
        BigDecimal amount,
        String currency,
        String counterpartyCountry,
        String reason,
        String decision,
        Instant occurredAt) {

    public TransactionEvent adjudicated(String decision, String reason) {
        return new TransactionEvent(
                transactionId, accountId, amount, currency, counterpartyCountry,
                reason, decision, Instant.now());
    }
}
