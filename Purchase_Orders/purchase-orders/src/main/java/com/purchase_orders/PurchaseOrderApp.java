package com.purchase_orders;

import com.purchase_orders.processor.PurchaseProcessor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class PurchaseOrderApp {

    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderApp.class);
    private static final String INPUT_TOPIC = "DBInfo";
    private static final String OUTPUT_TOPIC = "Purchases";

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "purchase-orders-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "broker1:9092,broker2:9092,broker3:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        Topology topology = new Topology();

        topology.addSource("Source", INPUT_TOPIC)
                .addProcessor("Process", PurchaseProcessor::new, "Source")
                .addSink("Sink", OUTPUT_TOPIC, "Process");

        final KafkaStreams streams = new KafkaStreams(topology, props);
        final CountDownLatch latch = new CountDownLatch(1);

        // Attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
                log.info("PurchaseOrderApp gracefully shut down.");
            }
        });

        try {
            log.info("Starting PurchaseOrderApp...");
            streams.start();
            latch.await();
        } catch (Throwable e) {
            log.error("Application failed.", e);
            System.exit(1);
        }
        System.exit(0);
    }
}
