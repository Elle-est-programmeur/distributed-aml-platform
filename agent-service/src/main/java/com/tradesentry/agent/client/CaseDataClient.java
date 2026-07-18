package com.tradesentry.agent.client;

import java.math.BigDecimal;
import java.util.List;

/**
 * Read-side access to account history and prior cases used during an investigation.
 *
 * <p>The investigation nodes depend on this abstraction, not on gRPC stubs. That keeps the graph
 * fully testable with an in-memory fake and lets Phase 6 swap in a real gRPC-backed implementation
 * without touching any node or graph wiring.
 */
public interface CaseDataClient {

    AccountHistory getAccountHistory(String accountId, int lookbackDays);

    List<SimilarCase> retrieveSimilarCases(String summary, BigDecimal amount,
                                           String counterpartyCountry, int maxResults);

    record AccountHistory(String accountId, int totalTransactions, double avgAmount,
                          double maxAmount, int priorFlags, boolean hasPriorSar, String riskBand) {}

    record SimilarCase(String caseId, double similarityScore, String outcome, String summary) {}
}
