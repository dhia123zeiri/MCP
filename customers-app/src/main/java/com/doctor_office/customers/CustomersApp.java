package com.doctor_office.customers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Standalone producer simulating customers booking appointments.
 *
 * Behaviour:
 *   1. Bootstraps by reading `dbinfo.pettypes` and `dbinfo.countries` to
 *      learn which pet types and countries exist (Kafka Connect populates
 *      these from Postgres). Uses a short poll, then closes that consumer.
 *   2. Loops forever producing Appointment messages to `appointments`,
 *      keyed by petTypeId so they co-partition with `supplies` for joins.
 *
 * Configuration is via env vars:
 *   KAFKA_BOOTSTRAP_SERVERS  (default: broker1:9092,broker2:9092,broker3:9092)
 *   APPOINTMENTS_PER_SECOND  (default: 2)
 *   DETERMINISTIC_SEED       (default: 42 — for fast verification)
 *
 * Producer config follows the assignment's fault-tolerance requirements:
 *   acks=all, idempotent, retries=MAX, multiple bootstrap servers.
 */
public final class CustomersApp {

    private static final Logger log = LoggerFactory.getLogger(CustomersApp.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final AtomicBoolean RUNNING = new AtomicBoolean(true);

    public static void main(String[] args) throws Exception {
        String bootstrap = env("KAFKA_BOOTSTRAP_SERVERS", "broker1:9092,broker2:9092,broker3:9092");
        double rate      = Double.parseDouble(env("APPOINTMENTS_PER_SECOND", "2"));
        long   seed      = Long.parseLong(env("DETERMINISTIC_SEED", "42"));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> RUNNING.set(false)));

        // ── Step 1: discover pet types and countries from DBInfo ────────
        List<Long>   petTypeIds = discoverPetTypeIds(bootstrap);
        List<String> countries  = discoverCountryCodes(bootstrap);

        if (petTypeIds.isEmpty()) {
            log.warn("No pet types found in dbinfo.pettypes — using fallback IDs 1..8.");
            for (long i = 1; i <= 8; i++) petTypeIds.add(i);
        }
        if (countries.isEmpty()) {
            log.warn("No countries found in dbinfo.countries — using fallback list.");
            countries.addAll(List.of("PT", "TN", "FR", "DE", "ES", "IT", "US", "BR"));
        }

        log.info("Discovered {} pet types and {} countries.", petTypeIds.size(), countries.size());

        // ── Step 2: produce ─────────────────────────────────────────────
        Properties props = producerProps(bootstrap);
        Random rng = new Random(seed);

        try (KafkaProducer<Long, byte[]> producer = new KafkaProducer<>(props)) {
            long sleepMs = Math.max(1, (long) (1000.0 / rate));
            while (RUNNING.get()) {
                long   petTypeId = petTypeIds.get(rng.nextInt(petTypeIds.size()));
                String country   = countries.get(rng.nextInt(countries.size()));
                double amount    = roundTo2(20.0 + rng.nextDouble() * 180.0);  // 20..200
                int    units     = 1 + rng.nextInt(3);                          // 1..3
                long   doctorId  = 1 + rng.nextInt(10);
                long   now       = System.currentTimeMillis();
                String apptId    = UUID.randomUUID().toString();

                Appointment a = new Appointment(apptId, petTypeId, doctorId, country, amount, units, now);
                byte[] payload = MAPPER.writeValueAsBytes(a);

                ProducerRecord<Long, byte[]> rec =
                    new ProducerRecord<>("appointments", petTypeId, payload);

                producer.send(rec, (md, ex) -> {
                    if (ex != null) log.error("Send failed", ex);
                    else log.debug("Sent appt petTypeId={} country={} total={}",
                                   a.petTypeId, a.countryCode, a.amount * a.units);
                });

                Thread.sleep(sleepMs);
            }
            producer.flush();
        }
        log.info("Customers-app shutting down cleanly.");
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────

    private static Properties producerProps(String bootstrap) {
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        p.put(ProducerConfig.ACKS_CONFIG, "all");
        p.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        p.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        p.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        p.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        p.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        return p;
    }

    /** One-shot consumer to learn pet type IDs from dbinfo.pettypes. */
    private static List<Long> discoverPetTypeIds(String bootstrap) {
        List<Long> ids = new ArrayList<>();
        Properties p = bootstrapConsumerProps(bootstrap, "customers-bootstrap-pettypes");

        try (KafkaConsumer<String, String> c = new KafkaConsumer<>(p)) {
            c.subscribe(List.of("dbinfo.pettypes"));
            long deadline = System.currentTimeMillis() + 15_000;
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> rs = c.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> r : rs) {
                    try {
                        var node = MAPPER.readTree(r.value());
                        long id = node.has("pet_type_id") ? node.get("pet_type_id").asLong() : -1;
                        if (id <= 0 && node.has("id")) id = node.get("id").asLong();
                        if (id > 0 && !ids.contains(id)) ids.add(id);
                    } catch (Exception ignored) {}
                }
                if (!ids.isEmpty() && rs.isEmpty()) break; // saw data, then nothing new
            }
        } catch (Exception e) {
            log.warn("Bootstrap consumer for pettypes failed: {}", e.toString());
        }
        return ids;
    }

    /** One-shot consumer to learn country codes from dbinfo.countries. */
    private static List<String> discoverCountryCodes(String bootstrap) {
        List<String> codes = new ArrayList<>();
        Properties p = bootstrapConsumerProps(bootstrap, "customers-bootstrap-countries");

        try (KafkaConsumer<String, String> c = new KafkaConsumer<>(p)) {
            c.subscribe(List.of("dbinfo.countries"));
            long deadline = System.currentTimeMillis() + 15_000;
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> rs = c.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> r : rs) {
                    try {
                        var node = MAPPER.readTree(r.value());
                        String code = node.has("code") ? node.get("code").asText() : null;
                        if (code != null && !codes.contains(code)) codes.add(code);
                    } catch (Exception ignored) {}
                }
                if (!codes.isEmpty() && rs.isEmpty()) break;
            }
        } catch (Exception e) {
            log.warn("Bootstrap consumer for countries failed: {}", e.toString());
        }
        return codes;
    }

    private static Properties bootstrapConsumerProps(String bootstrap, String groupId) {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ConsumerConfig.GROUP_ID_CONFIG, groupId + "-" + UUID.randomUUID());
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return p;
    }

    private static String env(String key, String dflt) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? dflt : v;
    }

    private static double roundTo2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
