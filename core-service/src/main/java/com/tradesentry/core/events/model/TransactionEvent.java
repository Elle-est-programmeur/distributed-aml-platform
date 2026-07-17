package com.tradesentry.core.events.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable event describing a transaction as it moves through the monitoring pipeline.
 * The same shape is reused across every topic; {@code reason}/{@code decision} are populated
 * only for the later lifecycle stages.
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

    /** Event emitted when a transaction is first accepted by the intake API. */
    public static TransactionEvent ingested(UUID transactionId,
                                            String accountId,
                                            BigDecimal amount,
                                            String currency,
                                            String counterpartyCountry) {
        return new TransactionEvent(
                transactionId,
                accountId,
                amount,
                currency,
                counterpartyCountry,
                null,
                null,
                Instant.now());
    }

    /** Copy of this event marked as flagged, carrying the screening reason. */
    public TransactionEvent flagged(String reason) {
        return new TransactionEvent(
                transactionId,
                accountId,
                amount,
                currency,
                counterpartyCountry,
                reason,
                null,
                Instant.now());
    }

    /** Copy of this event marked as adjudicated, carrying the final decision and reason. */
    public TransactionEvent adjudicated(String decision, String reason) {
        return new TransactionEvent(
                transactionId,
                accountId,
                amount,
                currency,
                counterpartyCountry,
                reason,
                decision,
                Instant.now());
    }
}
