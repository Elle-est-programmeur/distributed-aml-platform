package com.tradesentry.core.transaction;

/**
 * Lifecycle states of a monitored transaction.
 */
public enum TransactionStatus {

    /** Received via the intake API; screening has not yet run. */
    SUBMITTED,

    /** Screening found nothing suspicious; no investigation needed. */
    CLEARED_EARLY,

    /** Screening flagged the transaction for agent investigation. */
    FLAGGED,

    /** Investigation finished and a final disposition was recorded. */
    ADJUDICATED
}
