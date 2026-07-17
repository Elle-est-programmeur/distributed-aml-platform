package com.tradesentry.core.events.producer;

import com.tradesentry.core.events.model.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventProducer.class);

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public TransactionEventProducer(KafkaTemplate<String, TransactionEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, TransactionEvent event) {
        // Use accountId as the message key: all events for one account hash to the same
        // partition, which is what preserves per-account ordering downstream.
        kafkaTemplate.send(topic, event.accountId(), event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish to {} [tx={}, account={}]",
                        topic, event.transactionId(), event.accountId(), ex);
            } else {
                log.info("Published to {} [tx={}, account={}]",
                        topic, event.transactionId(), event.accountId());
            }
        });
    }
}
