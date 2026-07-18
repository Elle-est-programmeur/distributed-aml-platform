package com.tradesentry.core.events.consumer;

import com.tradesentry.core.events.config.KafkaTopics;
import com.tradesentry.core.events.model.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AuditConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditConsumer.class);

    // This listener uses its own "audit" consumer group. Kafka delivers every message to each
    // group independently, so having a dedicated group is what lets audit fan out alongside the
    // other consumers (screening, case management) without any of them stealing each other's records.
    @KafkaListener(topics = KafkaTopics.INGESTED, groupId = "audit")
    public void onIngested(TransactionEvent event) {
        log.info("AUDIT ingested [tx={}, account={}, amount={} {}]",
                event.transactionId(), event.accountId(), event.amount(), event.currency());
    }

    @KafkaListener(topics = KafkaTopics.ADJUDICATED, groupId = "audit")
    public void onAdjudicated(TransactionEvent event) {
        log.info("AUDIT adjudicated [tx={}, account={}, decision={}, reason={}]",
                event.transactionId(), event.accountId(), event.decision(), event.reason());
    }
}
