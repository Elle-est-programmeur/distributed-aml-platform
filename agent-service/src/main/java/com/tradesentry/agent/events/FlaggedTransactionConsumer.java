package com.tradesentry.agent.events;

import com.tradesentry.agent.graph.InvestigationState;
import com.tradesentry.agent.graph.StateGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Bridges Kafka and the investigation graph. Kafka provides the async choreography <em>between</em>
 * pipeline stages (screening → investigation → case management); the {@link StateGraph} runs
 * synchronously <em>within</em> this stage. Different transports for different problems.
 */
@Component
public class FlaggedTransactionConsumer {

    private static final Logger log = LoggerFactory.getLogger(FlaggedTransactionConsumer.class);

    static final String FLAGGED = "transactions.flagged";
    static final String ADJUDICATED = "transactions.adjudicated";

    private final StateGraph<InvestigationState> graph;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public FlaggedTransactionConsumer(StateGraph<InvestigationState> graph,
                                      KafkaTemplate<String, TransactionEvent> kafkaTemplate) {
        this.graph = graph;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = FLAGGED, groupId = "investigation-agent")
    public void onFlagged(TransactionEvent event) {
        log.info("Investigating [tx={}, account={}, reason={}]",
                event.transactionId(), event.accountId(), event.reason());

        InvestigationState initial = InvestigationState.start(
                event.transactionId(), event.accountId(), event.amount(),
                event.counterpartyCountry(), event.reason());

        InvestigationState result = graph.invoke(initial);

        kafkaTemplate.send(ADJUDICATED, event.accountId(),
                event.adjudicated(result.decision(), result.rationale()));

        log.info("Adjudicated [tx={}] -> {}", event.transactionId(), result.decision());
    }
}
