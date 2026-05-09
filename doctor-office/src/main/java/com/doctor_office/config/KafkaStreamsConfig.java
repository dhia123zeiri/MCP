package com.doctor_office.config;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Properties;

@Configuration
public class KafkaStreamsConfig {

    private KafkaStreams kafkaStreams;

    @PostConstruct
    public void startStreams() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "doctor-office-streams-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "broker1:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        StreamsBuilder builder = new StreamsBuilder();
        
        // Basic topology placeholder for future stream processing
        // We add a dummy topic so the topology is not empty
        builder.stream("dummy-input-topic");

        Topology topology = builder.build();

        kafkaStreams = new KafkaStreams(topology, props);
        kafkaStreams.start();
        System.out.println("Kafka Streams topology started.");
    }

    @PreDestroy
    public void closeStreams() {
        if (kafkaStreams != null) {
            kafkaStreams.close();
        }
    }
}
