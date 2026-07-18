package com.tradesentry.agent.nodes;

import com.tradesentry.agent.client.CaseDataClient;
import com.tradesentry.agent.client.CaseDataClient.AccountHistory;
import com.tradesentry.agent.client.CaseDataClient.SimilarCase;
import com.tradesentry.agent.graph.InvestigationState;
import com.tradesentry.agent.graph.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory for the individual {@link Node}s that make up the investigation graph. Each method
 * returns a pure function over {@link InvestigationState}; the graph wiring lives in the factory.
 */
@Component
public class InvestigationNodes {

    private static final Logger log = LoggerFactory.getLogger(InvestigationNodes.class);

    private final CaseDataClient caseData;

    public InvestigationNodes(CaseDataClient caseData) {
        this.caseData = caseData;
    }

    public Node<InvestigationState> enrich() {
        return state -> {
            AccountHistory history = caseData.getAccountHistory(state.accountId(), 90);
            log.info("enrich [tx={}] riskBand={} priorSar={}",
                    state.transactionId(), history.riskBand(), history.hasPriorSar());
            return state.withEnrichment(history.riskBand(), history.hasPriorSar());
        };
    }

    public Node<InvestigationState> retrieveCases() {
        return state -> {
            String summary = buildSummary(state);
            List<SimilarCase> cases = caseData.retrieveSimilarCases(
                    summary, state.amount(), state.counterpartyCountry(), 5);
            String worst = worstOutcome(cases);
            log.info("retrieveCases [tx={}] found={} worstOutcome={}",
                    state.transactionId(), cases.size(), worst);
            return state.withSimilarCases(cases.size(), worst);
        };
    }

    // Deterministic rule-based scoring. This is the seam to later replace with a Spring AI
    // ChatClient call that asks an LLM to weigh the same signals.
    public Node<InvestigationState> assess() {
        return state -> {
            double score = 0.0;

            String flagReason = state.flagReason();
            if (flagReason != null) {
                if (flagReason.contains("structuring")) {
                    score += 0.3;
                }
                if (flagReason.contains("high-risk")) {
                    score += 0.3;
                }
                if (flagReason.contains("large amount")) {
                    score += 0.2;
                }
            }
            if ("HIGH".equalsIgnoreCase(state.accountRiskBand())) {
                score += 0.2;
            }
            if (state.priorSar()) {
                score += 0.25;
            }
            if ("SAR_FILED".equals(state.worstPriorOutcome())) {
                score += 0.2;
            } else if ("ESCALATED".equals(state.worstPriorOutcome())) {
                score += 0.1;
            }
            score += state.investigationDepth() * 0.08;

            double clamped = Math.min(1.0, score);
            log.info("assess [tx={}] score={} depth={}",
                    state.transactionId(), clamped, state.investigationDepth());
            return state.withRiskScore(clamped);
        };
    }

    public Node<InvestigationState> investigateDeeper() {
        return state -> {
            log.info("investigateDeeper [tx={}] depth {} -> {}",
                    state.transactionId(), state.investigationDepth(), state.investigationDepth() + 1);
            return state.deeper();
        };
    }

    public Node<InvestigationState> decide() {
        return state -> {
            String decision;
            if (state.riskScore() >= 0.7) {
                decision = "ESCALATE";
            } else if (state.riskScore() >= 0.4) {
                decision = "FLAG";
            } else {
                decision = "CLEAR";
            }
            String rationale = String.format(
                    "score=%.2f, band=%s, priorSar=%s, similarCases=%d (worst=%s), reason=%s",
                    state.riskScore(), state.accountRiskBand(), state.priorSar(),
                    state.similarCaseCount(), state.worstPriorOutcome(), state.flagReason());
            log.info("decide [tx={}] -> {} ({})", state.transactionId(), decision, rationale);
            return state.withDecision(decision, rationale);
        };
    }

    private String buildSummary(InvestigationState state) {
        return String.format("amount=%s to %s; flagged for: %s",
                state.amount(), state.counterpartyCountry(), state.flagReason());
    }

    private String worstOutcome(List<SimilarCase> cases) {
        boolean sar = cases.stream().anyMatch(c -> "SAR_FILED".equals(c.outcome()));
        if (sar) {
            return "SAR_FILED";
        }
        boolean escalated = cases.stream().anyMatch(c -> "ESCALATED".equals(c.outcome()));
        if (escalated) {
            return "ESCALATED";
        }
        if (cases.isEmpty()) {
            return "NONE";
        }
        return "CLEARED";
    }
}
