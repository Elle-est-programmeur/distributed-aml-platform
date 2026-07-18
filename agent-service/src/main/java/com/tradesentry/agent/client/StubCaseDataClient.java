package com.tradesentry.agent.client;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory stand-in for the case-data service. Produces deterministic pseudo-data derived from
 * the accountId so behaviour is stable and repeatable across runs.
 *
 * <p>TODO: replaced by a real gRPC client in Phase 6.
 */
@Component
public class StubCaseDataClient implements CaseDataClient {

    @Override
    public AccountHistory getAccountHistory(String accountId, int lookbackDays) {
        int hash = Math.abs(accountId.hashCode());
        String riskBand = switch (hash % 3) {
            case 0 -> "LOW";
            case 1 -> "MEDIUM";
            default -> "HIGH";
        };
        boolean hasPriorSar = (hash % 5 == 0);
        int totalTransactions = 50 + hash % 200;
        double avgAmount = 1000 + hash % 5000;
        double maxAmount = 20000 + hash % 80000;
        int priorFlags = hash % 6;
        return new AccountHistory(accountId, totalTransactions, avgAmount, maxAmount,
                priorFlags, hasPriorSar, riskBand);
    }

    @Override
    public List<SimilarCase> retrieveSimilarCases(String summary, BigDecimal amount,
                                                  String counterpartyCountry, int maxResults) {
        boolean risky = amount.doubleValue() > 20000
                || List.of("KP", "IR", "SY").contains(counterpartyCountry);

        List<String> outcomes = risky
                ? List.of("SAR_FILED", "ESCALATED", "SAR_FILED")
                : List.of("CLEARED");

        List<SimilarCase> cases = new ArrayList<>();
        double similarityScore = 0.95;
        for (int i = 0; i < outcomes.size(); i++) {
            String outcome = outcomes.get(i);
            cases.add(new SimilarCase(
                    "case-" + (1000 + i),
                    similarityScore,
                    outcome,
                    "Prior " + outcome + " case with comparable profile"));
            similarityScore -= 0.07;
        }
        return cases;
    }
}
