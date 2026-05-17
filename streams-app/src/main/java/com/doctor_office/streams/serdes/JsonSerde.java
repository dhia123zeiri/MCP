package com.doctor_office.streams.serdes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Reusable JSON Serde for any POJO, built on Jackson.
 *
 * Usage:
 *   Serde<Appointment> apptSerde = JsonSerde.of(Appointment.class);
 *
 * This is the "standard Kafka Serializer/De-serializer construct" the
 * assignment asks for. Used in Produced.with(...), Consumed.with(...),
 * AND — critically — Materialized.with(...).
 */
public final class JsonSerde<T> implements Serde<T> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    private final Class<T> type;

    private JsonSerde(Class<T> type) { this.type = type; }

    public static <T> JsonSerde<T> of(Class<T> type) { return new JsonSerde<>(type); }

    @Override public Serializer<T> serializer() {
        return (topic, data) -> {
            if (data == null) return null;
            try { return MAPPER.writeValueAsBytes(data); }
            catch (Exception e) { throw new SerializationException(e); }
        };
    }

    @Override public Deserializer<T> deserializer() {
        return (topic, bytes) -> {
            if (bytes == null) return null;
            try { return MAPPER.readValue(bytes, type); }
            catch (Exception e) { throw new SerializationException(e); }
        };
    }
}
