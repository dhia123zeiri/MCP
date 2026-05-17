package com.doctor_office.streams.topology;

import com.doctor_office.streams.model.*;
import com.doctor_office.streams.serdes.ConnectJsonSerde;
import com.doctor_office.streams.serdes.JsonSerde;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.WindowStore;

import java.time.Duration;

/**
 * All 17 requirements as a single Kafka Streams topology.
 *
 * Internal stages use raw Long/Double Serdes (efficient).
 * The OUTPUT to each `results.*` topic is wrapped in a Connect-style
 * {schema, payload} JSON envelope so the JDBC sink connector can map
 * the columns directly (see ConnectJsonSerde + Results.*).
 */
public final class MetricsTopology {

    private final Duration windowSize;

    public MetricsTopology(Duration windowSize) {
        this.windowSize = windowSize;
    }

    public Topology build() {

        // -----------------------------------------------------------------
        // Serdes
        // -----------------------------------------------------------------
        Serde<String> stringSerde = Serdes.String();
        Serde<Long>   longSerde   = Serdes.Long();
        Serde<Double> doubleSerde = Serdes.Double();

        Serde<Appointment>  apptSerde         = JsonSerde.of(Appointment.class);
        Serde<Supply>       supplySerde       = JsonSerde.of(Supply.class);
        Serde<Avg>          avgSerde          = JsonSerde.of(Avg.class);
        Serde<TopProfit>    topProfitSerde    = JsonSerde.of(TopProfit.class);
        Serde<CountrySales> countrySalesSerde = JsonSerde.of(CountrySales.class);

        // Connect-envelope output Serdes (one per result shape)
        Serde<Results.PerItem>      perItemOut    = ConnectJsonSerde.of(Results.PerItem.class);
        Serde<Results.Total>        totalOut      = ConnectJsonSerde.of(Results.Total.class);
        Serde<Results.AvgByItem>    avgByItemOut  = ConnectJsonSerde.of(Results.AvgByItem.class);
        Serde<Results.AvgAll>       avgAllOut     = ConnectJsonSerde.of(Results.AvgAll.class);
        Serde<Results.TopProfitRow> topProfitOut  = ConnectJsonSerde.of(Results.TopProfitRow.class);
        Serde<Results.Windowed>     windowedOut   = ConnectJsonSerde.of(Results.Windowed.class);
        Serde<Results.TopCountry>   topCountryOut = ConnectJsonSerde.of(Results.TopCountry.class);

        StreamsBuilder b = new StreamsBuilder();

        // -----------------------------------------------------------------
        // Sources
        // -----------------------------------------------------------------
        KStream<Long, Appointment> appts =
            b.stream("appointments", Consumed.with(longSerde, apptSerde));
        KStream<Long, Supply> supplies =
            b.stream("supplies", Consumed.with(longSerde, supplySerde));

        // =================================================================
        // #5 Revenue per item (reduce)
        // =================================================================
        KTable<Long, Double> revenuePerItem = appts
            .mapValues(Appointment::total)
            .groupByKey(Grouped.with(longSerde, doubleSerde))
            .reduce(Double::sum, Materialized.<Long, Double, KeyValueStore<Bytes, byte[]>>as("revenue-per-item-store")
                .withKeySerde(longSerde).withValueSerde(doubleSerde));

        revenuePerItem.toStream()
            .map((k, v) -> KeyValue.pair(k, new Results.PerItem(k, "revenue", v)))
            .to("results.revenue-per-item", Produced.with(longSerde, perItemOut));

        // =================================================================
        // #6 Expenses per item (reduce)
        // =================================================================
        KTable<Long, Double> expensesPerItem = supplies
            .mapValues(Supply::total)
            .groupByKey(Grouped.with(longSerde, doubleSerde))
            .reduce(Double::sum, Materialized.<Long, Double, KeyValueStore<Bytes, byte[]>>as("expenses-per-item-store")
                .withKeySerde(longSerde).withValueSerde(doubleSerde));

        expensesPerItem.toStream()
            .map((k, v) -> KeyValue.pair(k, new Results.PerItem(k, "expenses", v)))
            .to("results.expenses-per-item", Produced.with(longSerde, perItemOut));

        // =================================================================
        // #7 Profit per item (KTable-KTable outer join)
        // =================================================================
        KTable<Long, Double> profitPerItem = revenuePerItem.outerJoin(
            expensesPerItem,
            (rev, exp) -> (rev == null ? 0.0 : rev) - (exp == null ? 0.0 : exp),
            Materialized.<Long, Double, KeyValueStore<Bytes, byte[]>>as("profit-per-item-store")
                .withKeySerde(longSerde).withValueSerde(doubleSerde));

        profitPerItem.toStream()
            .map((k, v) -> KeyValue.pair(k, new Results.PerItem(k, "profit", v)))
            .to("results.profit-per-item", Produced.with(longSerde, perItemOut));

        // =================================================================
        // #8 Total revenues — groupBy() (re-keying, NOT groupByKey)
        // =================================================================
        KTable<String, Double> totalRevenue = appts
            .mapValues(Appointment::total)
            .groupBy((k, v) -> "ALL", Grouped.with(stringSerde, doubleSerde))
            .reduce(Double::sum, Materialized.<String, Double, KeyValueStore<Bytes, byte[]>>as("total-revenue-store")
                .withKeySerde(stringSerde).withValueSerde(doubleSerde));

        totalRevenue.toStream()
            .map((k, v) -> KeyValue.pair("revenue", new Results.Total("revenue", v)))
            .to("results.total-revenue", Produced.with(stringSerde, totalOut));

        // =================================================================
        // #9 Total expenses — groupBy()
        // =================================================================
        KTable<String, Double> totalExpenses = supplies
            .mapValues(Supply::total)
            .groupBy((k, v) -> "ALL", Grouped.with(stringSerde, doubleSerde))
            .reduce(Double::sum, Materialized.<String, Double, KeyValueStore<Bytes, byte[]>>as("total-expenses-store")
                .withKeySerde(stringSerde).withValueSerde(doubleSerde));

        totalExpenses.toStream()
            .map((k, v) -> KeyValue.pair("expenses", new Results.Total("expenses", v)))
            .to("results.total-expenses", Produced.with(stringSerde, totalOut));

        // =================================================================
        // #10 Total profit (KTable-KTable outer join)
        // =================================================================
        KTable<String, Double> totalProfit = totalRevenue.outerJoin(
            totalExpenses,
            (r, e) -> (r == null ? 0.0 : r) - (e == null ? 0.0 : e),
            Materialized.<String, Double, KeyValueStore<Bytes, byte[]>>as("total-profit-store")
                .withKeySerde(stringSerde).withValueSerde(doubleSerde));

        totalProfit.toStream()
            .map((k, v) -> KeyValue.pair("profit", new Results.Total("profit", v)))
            .to("results.total-profit", Produced.with(stringSerde, totalOut));

        // =================================================================
        // #11 Avg per appointment, per item — aggregate()
        // =================================================================
        KTable<Long, Avg> avgPerItem = appts
            .mapValues(Appointment::total)
            .groupByKey(Grouped.with(longSerde, doubleSerde))
            .aggregate(
                Avg::new,
                (k, v, agg) -> agg.add(v),
                Materialized.<Long, Avg, KeyValueStore<Bytes, byte[]>>as("avg-per-item-store")
                    .withKeySerde(longSerde).withValueSerde(avgSerde));

        avgPerItem.toStream()
            .map((k, v) -> KeyValue.pair(k, new Results.AvgByItem(k, v.avg, v.count)))
            .to("results.avg-per-appointment-by-item", Produced.with(longSerde, avgByItemOut));

        // =================================================================
        // #12 Avg per appointment, all items — groupBy() + aggregate()
        // =================================================================
        KTable<String, Avg> avgAll = appts
            .mapValues(Appointment::total)
            .groupBy((k, v) -> "ALL", Grouped.with(stringSerde, doubleSerde))
            .aggregate(
                Avg::new,
                (k, v, agg) -> agg.add(v),
                Materialized.<String, Avg, KeyValueStore<Bytes, byte[]>>as("avg-all-store")
                    .withKeySerde(stringSerde).withValueSerde(avgSerde));

        avgAll.toStream()
            .map((k, v) -> KeyValue.pair("ALL", new Results.AvgAll("ALL", v.avg, v.count)))
            .to("results.avg-per-appointment-all", Produced.with(stringSerde, avgAllOut));

        // =================================================================
        // #13 Item with highest profit overall — aggregate() with state
        // =================================================================
        KTable<String, TopProfit> topProfit = profitPerItem
            .toStream()
            .map((petTypeId, profit) -> KeyValue.pair("TOP", new TopProfit(petTypeId, profit)))
            .groupByKey(Grouped.with(stringSerde, topProfitSerde))
            .aggregate(
                TopProfit::empty,
                (k, candidate, current) -> candidate.profit > current.profit ? candidate : current,
                Materialized.<String, TopProfit, KeyValueStore<Bytes, byte[]>>as("top-profit-store")
                    .withKeySerde(stringSerde).withValueSerde(topProfitSerde));

        topProfit.toStream()
            .map((k, v) -> KeyValue.pair("TOP",
                new Results.TopProfitRow("TOP", v.pet_type_id, v.profit)))
            .to("results.top-profit-item", Produced.with(stringSerde, topProfitOut));

        // =================================================================
        // #14 Windowed revenue — tumbling window
        // =================================================================
        appts.mapValues(Appointment::total)
             .groupByKey(Grouped.with(longSerde, doubleSerde))
             .windowedBy(TimeWindows.ofSizeWithNoGrace(windowSize))
             .reduce(Double::sum,
                Materialized.<Long, Double, WindowStore<Bytes, byte[]>>as("windowed-revenue-store")
                    .withKeySerde(longSerde).withValueSerde(doubleSerde))
             .toStream()
             .map((wk, v) -> {
                 String key = wk.key() + "@" + wk.window().start();
                 return KeyValue.pair(key, new Results.Windowed(key, "revenue", v));
             })
             .to("results.windowed-revenue", Produced.with(stringSerde, windowedOut));

        // =================================================================
        // #15 Windowed expenses — tumbling window
        // =================================================================
        supplies.mapValues(Supply::total)
                .groupByKey(Grouped.with(longSerde, doubleSerde))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(windowSize))
                .reduce(Double::sum,
                    Materialized.<Long, Double, WindowStore<Bytes, byte[]>>as("windowed-expenses-store")
                        .withKeySerde(longSerde).withValueSerde(doubleSerde))
                .toStream()
                .map((wk, v) -> {
                    String key = wk.key() + "@" + wk.window().start();
                    return KeyValue.pair(key, new Results.Windowed(key, "expenses", v));
                })
                .to("results.windowed-expenses", Produced.with(stringSerde, windowedOut));

        // =================================================================
        // #16 Windowed profit — tumbling window
        // =================================================================
        KTable<Windowed<Long>, Double> winRev = appts
            .mapValues(Appointment::total)
            .groupByKey(Grouped.with(longSerde, doubleSerde))
            .windowedBy(TimeWindows.ofSizeWithNoGrace(windowSize))
            .reduce(Double::sum,
                Materialized.<Long, Double, WindowStore<Bytes, byte[]>>as("windowed-revenue-join-store")
                    .withKeySerde(longSerde).withValueSerde(doubleSerde));

        KTable<Windowed<Long>, Double> winExp = supplies
            .mapValues(Supply::total)
            .groupByKey(Grouped.with(longSerde, doubleSerde))
            .windowedBy(TimeWindows.ofSizeWithNoGrace(windowSize))
            .reduce(Double::sum,
                Materialized.<Long, Double, WindowStore<Bytes, byte[]>>as("windowed-expenses-join-store")
                    .withKeySerde(longSerde).withValueSerde(doubleSerde));

        winRev.outerJoin(winExp, (r, e) -> (r == null ? 0.0 : r) - (e == null ? 0.0 : e))
              .toStream()
              .map((wk, v) -> {
                  String key = wk.key() + "@" + wk.window().start();
                  return KeyValue.pair(key, new Results.Windowed(key, "profit", v));
              })
              .to("results.windowed-profit", Produced.with(stringSerde, windowedOut));

        // =================================================================
        // #17 Country with highest sales per item (+ value)
        // =================================================================
        KTable<String, Double> salesPerItemAndCountry = appts
            .map((k, a) -> KeyValue.pair(a.petTypeId + "|" + a.countryCode, a.total()))
            .groupByKey(Grouped.with(stringSerde, doubleSerde))
            .reduce(Double::sum,
                Materialized.<String, Double, KeyValueStore<Bytes, byte[]>>as("sales-per-item-country-store")
                    .withKeySerde(stringSerde).withValueSerde(doubleSerde));

        KTable<Long, CountrySales> topCountryPerItem = salesPerItemAndCountry
            .toStream()
            .map((compoundKey, total) -> {
                String[] parts = compoundKey.split("\\|");
                long petTypeId = Long.parseLong(parts[0]);
                String country = parts[1];
                return KeyValue.pair(petTypeId, new CountrySales(country, total));
            })
            .groupByKey(Grouped.with(longSerde, countrySalesSerde))
            .aggregate(
                CountrySales::empty,
                (petTypeId, candidate, current) ->
                    candidate.sales_amount > current.sales_amount ? candidate : current,
                Materialized.<Long, CountrySales, KeyValueStore<Bytes, byte[]>>as("top-country-per-item-store")
                    .withKeySerde(longSerde).withValueSerde(countrySalesSerde));

        topCountryPerItem.toStream()
            .map((k, v) -> KeyValue.pair(k,
                new Results.TopCountry(k, v.country_code, v.sales_amount)))
            .to("results.top-country-per-item", Produced.with(longSerde, topCountryOut));

        return b.build();
    }
}
