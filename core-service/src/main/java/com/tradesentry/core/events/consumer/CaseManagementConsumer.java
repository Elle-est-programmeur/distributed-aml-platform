package com.tradesentry.core.events.consumer;

import com.tradesentry.core.events.config.KafkaTopics;
import com.tradesentry.core.events.model.TransactionEvent;
import com.tradesentry.core.transaction.TransactionRepository;
import com.tradesentry.core.transaction.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes the agent's adjudication decisions, marks the transaction ADJUDICATED, and opens a
 * SAR case when the decision is to escalate. Runs in its own "case-mgmt" group.
 */
@Component
public class CaseManagementConsumer {

    private static final Logger log = LoggerFactory.getLogger(CaseManagementConsumer.class);

    private final TransactionRepository repository;

    public CaseManagementConsumer(TransactionRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = KafkaTopics.ADJUDICATED, groupId = "case-mgmt")
    public void onAdjudicated(TransactionEvent event) {
        repository.findById(event.transactionId()).ifPresent(tx -> {
            tx.setStatus(TransactionStatus.ADJUDICATED);
            repository.save(tx);
        });

        if ("ESCALATE".equalsIgnoreCase(event.decision())) {
            log.info("SAR CASE FILED [tx={}, account={}, reason={}]",
                    event.transactionId(), event.accountId(), event.reason());
        } else {
            log.info("No case needed [tx={}, decision={}]",
                    event.transactionId(), event.decision());
        }
    }
}
