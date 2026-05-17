package com.doctor_office.streams;

import com.doctor_office.streams.topology.MetricsTopology;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * Standalone Kafka Streams app for Assignment #3.
 *
 * Reads `appointments` and `supplies`, computes 17 metrics, writes results
 * to result topics that a Kafka Connect sink will land in Postgres.
 */
public final class StreamsApp {

    private static final Logger log = LoggerFactory.getLogger(StreamsApp.class);

    public static void main(String[] args) throws InterruptedException {
        String bootstrap = System.getenv().getOrDefault(
            "KAFKA_BOOTSTRAP_SERVERS", "broker1:9092,broker2:9092,broker3:9092");
        String appId = System.getenv().getOrDefault(
            "APPLICATION_ID", "doctor-office-metrics");
        int windowMinutes = Integer.parseInt(
            System.getenv().getOrDefault("WINDOW_DURATION_MINUTES", "1"));

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, appId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 2);

        // ── Fault tolerance ─────────────────────────────────────────────
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, 3);
        props.put(StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG, 1);
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);

        // ── Consumer behaviour ──────────────────────────────────────────
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // ── Reasonable production defaults ──────────────────────────────
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10 * 1024 * 1024);

        // ── Build the topology ──────────────────────────────────────────
        MetricsTopology metrics = new MetricsTopology(Duration.ofMinutes(windowMinutes));
        Topology topology = metrics.build();
        log.info("Topology:\n{}", topology.describe());

        KafkaStreams streams = new KafkaStreams(topology, props);

        // On any uncaught exception, replace the thread (don't kill the app).
        // This is the safer default for the demo — a single broker hiccup
        // shouldn't bring the whole app down.
        streams.setUncaughtExceptionHandler(ex -> {
            log.error("Uncaught streams exception — replacing thread", ex);
            return StreamThreadExceptionResponse.REPLACE_THREAD;
        });

        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown hook — closing streams.");
            streams.close(Duration.ofSeconds(10));
            latch.countDown();
        }));

        log.info("Starting streams app `{}` on {}", appId, bootstrap);
        streams.start();
        latch.await();
    }
}
