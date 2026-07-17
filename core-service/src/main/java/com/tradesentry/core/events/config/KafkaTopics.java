package com.tradesentry.core.events.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic names and their provisioning.
 *
 * <p>Every topic is keyed by {@code accountId}. Kafka guarantees ordering only within a
 * partition, and all messages sharing a key land on the same partition, so keying by account
 * gives us strict ordering of a single account's events while still allowing multiple
 * partitions to be consumed in parallel across different accounts.
 */
@Configuration
public class KafkaTopics {

    public static final String INGESTED = "transactions.ingested";
    public static final String FLAGGED = "transactions.flagged";
    public static final String ADJUDICATED = "transactions.adjudicated";
    public static final String DLQ = "transactions.dlq";

    @Bean
    public NewTopic ingestedTopic() {
        return TopicBuilder.name(INGESTED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic flaggedTopic() {
        return TopicBuilder.name(FLAGGED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic adjudicatedTopic() {
        return TopicBuilder.name(ADJUDICATED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic dlqTopic() {
        return TopicBuilder.name(DLQ).partitions(1).replicas(1).build();
    }
}
