package com.doctor_office.streams.model;

import com.doctor_office.streams.serdes.ConnectJsonSerde;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Connect-shape result records — one class per output topic.
 *
 * Each record knows its own schema and payload so the ConnectJsonSerde
 * can wrap it in the {schema, payload} envelope the JDBC sink needs.
 *
 * Why: emitting raw Long/Double bytes works between Streams stages but
 * the JDBC sink connector needs a JSON object with named columns.
 */
public final class Results {

    private Results() {}

    // ── res_revenue_per_item, res_expenses_per_item, res_profit_per_item ──
    public static final class PerItem implements ConnectJsonSerde.HasSchema {
        public final long   petTypeId;
        public final String column;       // "revenue" | "expenses" | "profit"
        public final double value;

        public PerItem(long petTypeId, String column, double value) {
            this.petTypeId = petTypeId;
            this.column    = column;
            this.value     = value;
        }
        @Override public Map<String, Object> connectSchema() {
            return ConnectJsonSerde.struct(List.of(
                ConnectJsonSerde.field("pet_type_id", "int64",  false),
                ConnectJsonSerde.field(column,        "double", false)
            ));
        }
        @Override public Map<String, Object> connectPayload() {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("pet_type_id", petTypeId);
            p.put(column, value);
            return p;
        }
    }

    // ── res_totals ────────────────────────────────────────────────────────
    public static final class Total implements ConnectJsonSerde.HasSchema {
        public final String scope;    // "revenue" | "expenses" | "profit"
        public final double amount;

        public Total(String scope, double amount) {
            this.scope  = scope;
            this.amount = amount;
        }
        @Override public Map<String, Object> connectSchema() {
            return ConnectJsonSerde.struct(List.of(
                ConnectJsonSerde.field("scope",  "string", false),
                ConnectJsonSerde.field("amount", "double", false)
            ));
        }
        @Override public Map<String, Object> connectPayload() {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("scope", scope);
            p.put("amount", amount);
            return p;
        }
    }

    // ── res_avg_per_appointment_by_item ──────────────────────────────────
    public static final class AvgByItem implements ConnectJsonSerde.HasSchema {
        public final long   petTypeId;
        public final double avgAmount;
        public final long   count;

        public AvgByItem(long petTypeId, double avgAmount, long count) {
            this.petTypeId = petTypeId;
            this.avgAmount = avgAmount;
            this.count     = count;
        }
        @Override public Map<String, Object> connectSchema() {
            return ConnectJsonSerde.struct(List.of(
                ConnectJsonSerde.field("pet_type_id", "int64",  false),
                ConnectJsonSerde.field("avg_amount",  "double", false),
                ConnectJsonSerde.field("count",       "int64",  false)
            ));
        }
        @Override public Map<String, Object> connectPayload() {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("pet_type_id", petTypeId);
            p.put("avg_amount", avgAmount);
            p.put("count", count);
            return p;
        }
    }

    // ── res_avg_per_appointment_all ──────────────────────────────────────
    public static final class AvgAll implements ConnectJsonSerde.HasSchema {
        public final String scope;
        public final double avgAmount;
        public final long   count;

        public AvgAll(String scope, double avgAmount, long count) {
            this.scope     = scope;
            this.avgAmount = avgAmount;
            this.count     = count;
        }
        @Override public Map<String, Object> connectSchema() {
            return ConnectJsonSerde.struct(List.of(
                ConnectJsonSerde.field("scope",      "string", false),
                ConnectJsonSerde.field("avg_amount", "double", false),
                ConnectJsonSerde.field("count",      "int64",  false)
            ));
        }
        @Override public Map<String, Object> connectPayload() {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("scope", scope);
            p.put("avg_amount", avgAmount);
            p.put("count", count);
            return p;
        }
    }

    // ── res_top_profit_item ──────────────────────────────────────────────
    public static final class TopProfitRow implements ConnectJsonSerde.HasSchema {
        public final String scope;       // always "TOP"
        public final long   petTypeId;
        public final double profit;

        public TopProfitRow(String scope, long petTypeId, double profit) {
            this.scope     = scope;
            this.petTypeId = petTypeId;
            this.profit    = profit;
        }
        @Override public Map<String, Object> connectSchema() {
            return ConnectJsonSerde.struct(List.of(
                ConnectJsonSerde.field("scope",       "string", false),
                ConnectJsonSerde.field("pet_type_id", "int64",  false),
                ConnectJsonSerde.field("profit",      "double", false)
            ));
        }
        @Override public Map<String, Object> connectPayload() {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("scope", scope);
            p.put("pet_type_id", petTypeId);
            p.put("profit", profit);
            return p;
        }
    }

    // ── res_windowed (one table for all three windowed metrics) ──────────
    public static final class Windowed implements ConnectJsonSerde.HasSchema {
        public final String windowKey;    // "petTypeId@windowStart"
        public final String metric;       // "revenue" | "expenses" | "profit"
        public final double amount;

        public Windowed(String windowKey, String metric, double amount) {
            this.windowKey = windowKey;
            this.metric    = metric;
            this.amount    = amount;
        }
        @Override public Map<String, Object> connectSchema() {
            return ConnectJsonSerde.struct(List.of(
                ConnectJsonSerde.field("window_key", "string", false),
                ConnectJsonSerde.field("metric",     "string", false),
                ConnectJsonSerde.field("amount",     "double", false)
            ));
        }
        @Override public Map<String, Object> connectPayload() {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("window_key", windowKey);
            p.put("metric", metric);
            p.put("amount", amount);
            return p;
        }
    }

    // ── res_top_country_per_item ─────────────────────────────────────────
    public static final class TopCountry implements ConnectJsonSerde.HasSchema {
        public final long   petTypeId;
        public final String countryCode;
        public final double salesAmount;

        public TopCountry(long petTypeId, String countryCode, double salesAmount) {
            this.petTypeId   = petTypeId;
            this.countryCode = countryCode;
            this.salesAmount = salesAmount;
        }
        @Override public Map<String, Object> connectSchema() {
            return ConnectJsonSerde.struct(List.of(
                ConnectJsonSerde.field("pet_type_id",  "int64",  false),
                ConnectJsonSerde.field("country_code", "string", false),
                ConnectJsonSerde.field("sales_amount", "double", false)
            ));
        }
        @Override public Map<String, Object> connectPayload() {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("pet_type_id", petTypeId);
            p.put("country_code", countryCode);
            p.put("sales_amount", salesAmount);
            return p;
        }
    }
}
