package com.doctor_office.streams.serdes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Produces Kafka Connect-compatible JSON envelopes:
 *
 *   {
 *     "schema": { "type":"struct", "fields":[ {field, type, optional}... ], "optional": false },
 *     "payload": { field1: v1, field2: v2, ... }
 *   }
 *
 * This is what the JDBC sink connector expects when
 * `value.converter.schemas.enable=true`.
 *
 * We use this Serde for every result topic written by the Streams app so
 * the JDBC sink can map JSON fields directly to table columns.
 */
public final class ConnectJsonSerde<T extends ConnectJsonSerde.HasSchema> implements Serde<T> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Class<T> type;

    private ConnectJsonSerde(Class<T> type) { this.type = type; }

    public static <T extends HasSchema> ConnectJsonSerde<T> of(Class<T> type) {
        return new ConnectJsonSerde<>(type);
    }

    @Override public Serializer<T> serializer() {
        return (topic, data) -> {
            if (data == null) return null;
            try {
                Map<String, Object> env = new LinkedHashMap<>();
                env.put("schema",  data.connectSchema());
                env.put("payload", data.connectPayload());
                return MAPPER.writeValueAsBytes(env);
            } catch (JsonProcessingException e) {
                throw new SerializationException(e);
            }
        };
    }

    @Override public Deserializer<T> deserializer() {
        // We never consume our own output as POJOs in the topology.
        return (topic, bytes) -> { throw new UnsupportedOperationException(); };
    }

    /** Records that can produce their own Connect-style schema + payload. */
    public interface HasSchema {
        Map<String, Object> connectSchema();
        Map<String, Object> connectPayload();
    }

    /** Helper to build a `{type:"struct", fields:[...], optional:false}` schema. */
    public static Map<String, Object> struct(List<Map<String, Object>> fields) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "struct");
        m.put("fields", fields);
        m.put("optional", false);
        return m;
    }

    public static Map<String, Object> field(String name, String type, boolean optional) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("field", name);
        m.put("type", type);
        m.put("optional", optional);
        return m;
    }
}
