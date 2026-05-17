# Doctor Office — Kafka/EAI Architecture (Assignment #3)

This document maps the assignment's "shop" example onto your existing
doctor-office domain, then gives you the topic schemas, the full Kafka
Streams topology mapping all 17 requirements, the fault-tolerance plan,
and the project layout.

---

## 1. Domain mapping: Shop → Doctor Office

The assignment's example is a shop with Sales and Purchases. We keep your
Doctors and Pets, and add a monetary transactional layer on top so every
requirement maps cleanly.

| Assignment concept    | Our concept                                              | Where the money is                        |
|-----------------------|----------------------------------------------------------|-------------------------------------------|
| Item for sale         | **PetType** (Dog–Husky, Cat–Persian, ...) — already exists | Each PetType has a `price`                |
| Country               | **Country** (clinic country / pet owner country)         | New table                                 |
| Sale                  | **Appointment** (a vet visit / treatment for a pet)      | Revenue side                              |
| Purchase from supplier| **Supply** (vaccines, food, medication bought from suppliers, keyed on PetType) | Expense side                              |
| Sales topic           | `appointments`                                           | Producer: `customers-app` (standalone)    |
| Purchases topic       | `supplies`                                               | Producer: `suppliers-app` (standalone)    |
| DBInfo topic          | `dbinfo.pettypes`, `dbinfo.countries`                    | Kafka Connect JDBC source                 |
| Results topics        | `results.*` (one per metric)                             | Kafka Connect JDBC sink → DB              |

This gives us:

- Revenue per item  → revenue per PetType (sum of appointment amounts).
- Expenses per item → cost per PetType (sum of supply amounts).
- Profit per item   → revenue − expenses, joined per PetType.
- Country dimension → each appointment carries a country (where the
  customer is from). Lets us implement requirement #17.

---

## 2. Topics

All topics: `replication.factor=3`, `min.insync.replicas=2`, `partitions=3`.

### Input topics

| Topic               | Key                | Value (JSON)                                                            | Producer               |
|---------------------|--------------------|-------------------------------------------------------------------------|------------------------|
| `dbinfo.pettypes`   | `petTypeId` (Long) | `{ id, species, breed, price }`                                         | Kafka Connect (source) |
| `dbinfo.countries`  | `countryCode` (Str)| `{ code, name }`                                                        | Kafka Connect (source) |
| `appointments`      | `petTypeId` (Long) | `{ appointmentId, petTypeId, doctorId, countryCode, amount, units, timestamp }` | `customers-app`        |
| `supplies`          | `petTypeId` (Long) | `{ supplyId, petTypeId, supplierCountry, amount, units, timestamp }`    | `suppliers-app`        |

Note: we key everything by `petTypeId` so co-partitioning is automatic for
joins. Requirements #8/9/12 demand `groupBy()` (not `groupByKey()`),
which we use precisely because they need re-keying.

### Output (results) topics — all written by the Streams app, read by Kafka Connect sink

| Topic                            | Key                 | Value                                  | Backs requirement |
|----------------------------------|---------------------|----------------------------------------|-------------------|
| `results.revenue-per-item`       | `petTypeId`         | `Double` (total revenue)               | #5                |
| `results.expenses-per-item`      | `petTypeId`         | `Double` (total expenses)              | #6                |
| `results.profit-per-item`        | `petTypeId`         | `Double` (revenue − expenses)          | #7                |
| `results.total-revenue`          | `"ALL"`             | `Double`                               | #8                |
| `results.total-expenses`         | `"ALL"`             | `Double`                               | #9                |
| `results.total-profit`           | `"ALL"`             | `Double`                               | #10               |
| `results.avg-per-appointment-by-item` | `petTypeId`    | `{count, sum, avg}`                    | #11               |
| `results.avg-per-appointment-all`     | `"ALL"`        | `{count, sum, avg}`                    | #12               |
| `results.top-profit-item`        | `"TOP"`             | `{petTypeId, profit}`                  | #13               |
| `results.windowed-revenue`       | `petTypeId@window`  | `Double`                               | #14               |
| `results.windowed-expenses`      | `petTypeId@window`  | `Double`                               | #15               |
| `results.windowed-profit`        | `petTypeId@window`  | `Double`                               | #16               |
| `results.top-country-per-item`   | `petTypeId`         | `{countryCode, salesAmount}`           | #17               |

---

## 3. Streams topology — operator-by-operator mapping

Pseudocode below; concrete Java will live in
`streams-app/src/main/java/.../topology/MetricsTopology.java`.

```java
StreamsBuilder b = new StreamsBuilder();

KStream<Long, Appointment> appts    = b.stream("appointments",  Consumed.with(longSerde, apptSerde));
KStream<Long, Supply>      supplies = b.stream("supplies",      Consumed.with(longSerde, supplySerde));

// --- #5 Revenue per item (reduce) ---------------------------------------
KTable<Long, Double> revenuePerItem =
    appts.mapValues(a -> a.amount * a.units)
         .groupByKey(Grouped.with(longSerde, doubleSerde))
         .reduce(Double::sum, Materialized.as("revenue-per-item-store"));
revenuePerItem.toStream().to("results.revenue-per-item");

// --- #6 Expenses per item (reduce) --------------------------------------
KTable<Long, Double> expensesPerItem =
    supplies.mapValues(s -> s.amount * s.units)
            .groupByKey(Grouped.with(longSerde, doubleSerde))
            .reduce(Double::sum, Materialized.as("expenses-per-item-store"));
expensesPerItem.toStream().to("results.expenses-per-item");

// --- #7 Profit per item (KTable-KTable join) ----------------------------
KTable<Long, Double> profitPerItem =
    revenuePerItem.outerJoin(expensesPerItem,
        (rev, exp) -> (rev == null ? 0.0 : rev) - (exp == null ? 0.0 : exp),
        Materialized.as("profit-per-item-store"));
profitPerItem.toStream().to("results.profit-per-item");

// --- #8 Total revenues (groupBy re-key — NOT groupByKey) ----------------
KTable<String, Double> totalRevenue =
    appts.mapValues(a -> a.amount * a.units)
         .groupBy((k, v) -> "ALL", Grouped.with(stringSerde, doubleSerde))
         .reduce(Double::sum, Materialized.as("total-revenue-store"));
totalRevenue.toStream().to("results.total-revenue");

// --- #9 Total expenses (groupBy) ----------------------------------------
KTable<String, Double> totalExpenses =
    supplies.mapValues(s -> s.amount * s.units)
            .groupBy((k, v) -> "ALL", Grouped.with(stringSerde, doubleSerde))
            .reduce(Double::sum, Materialized.as("total-expenses-store"));
totalExpenses.toStream().to("results.total-expenses");

// --- #10 Total profit (KTable-KTable join) ------------------------------
KTable<String, Double> totalProfit =
    totalRevenue.outerJoin(totalExpenses,
        (r, e) -> (r == null ? 0.0 : r) - (e == null ? 0.0 : e),
        Materialized.as("total-profit-store"));
totalProfit.toStream().to("results.total-profit");

// --- #11 Average per appointment per item (aggregate) -------------------
KTable<Long, Avg> avgPerItem =
    appts.mapValues(a -> a.amount * a.units)
         .groupByKey(Grouped.with(longSerde, doubleSerde))
         .aggregate(Avg::new,
                    (k, v, agg) -> agg.add(v),
                    Materialized.<Long, Avg, KeyValueStore<Bytes, byte[]>>as("avg-per-item-store")
                                .withValueSerde(avgSerde));
avgPerItem.toStream().to("results.avg-per-appointment-by-item");

// --- #12 Average per appointment all items (groupBy + aggregate) --------
KTable<String, Avg> avgAll =
    appts.mapValues(a -> a.amount * a.units)
         .groupBy((k, v) -> "ALL", Grouped.with(stringSerde, doubleSerde))
         .aggregate(Avg::new, (k, v, agg) -> agg.add(v),
                    Materialized.<String, Avg, KeyValueStore<Bytes, byte[]>>as("avg-all-store")
                                .withValueSerde(avgSerde));
avgAll.toStream().to("results.avg-per-appointment-all");

// --- #13 Item with highest profit (aggregate with state) ----------------
KTable<String, TopProfit> topProfit =
    profitPerItem.toStream()
        .map((petTypeId, profit) -> KeyValue.pair("TOP", new TopProfit(petTypeId, profit)))
        .groupByKey(Grouped.with(stringSerde, topProfitSerde))
        .aggregate(TopProfit::empty,
                   (k, candidate, current) -> candidate.profit > current.profit ? candidate : current,
                   Materialized.<String, TopProfit, KeyValueStore<Bytes, byte[]>>as("top-profit-store")
                               .withValueSerde(topProfitSerde));
topProfit.toStream().to("results.top-profit-item");

// --- #14/#15/#16 Tumbling 1-hour windows --------------------------------
Duration window = Duration.ofMinutes(1); // change to 1h for production

appts.mapValues(a -> a.amount * a.units)
     .groupByKey(Grouped.with(longSerde, doubleSerde))
     .windowedBy(TimeWindows.ofSizeWithNoGrace(window))
     .reduce(Double::sum, Materialized.as("windowed-revenue-store"))
     .toStream()
     .map((wk, v) -> KeyValue.pair(wk.key() + "@" + wk.window().start(), v))
     .to("results.windowed-revenue");

// (mirror the same shape for windowed-expenses and windowed-profit)

// --- #17 Top country per item (aggregate with composite value) ----------
KTable<String, CountrySales> perItemCountry =
    appts.map((k, a) -> KeyValue.pair(a.petTypeId + "|" + a.countryCode, a.amount * a.units))
         .groupByKey(Grouped.with(stringSerde, doubleSerde))
         .reduce(Double::sum);

KTable<Long, CountrySales> topCountryPerItem =
    perItemCountry.toStream()
        .map((compoundKey, total) -> {
            String[] parts = compoundKey.split("\\|");
            return KeyValue.pair(Long.parseLong(parts[0]), new CountrySales(parts[1], total));
        })
        .groupByKey(Grouped.with(longSerde, countrySalesSerde))
        .aggregate(CountrySales::empty,
                   (petTypeId, candidate, current) -> candidate.salesAmount > current.salesAmount ? candidate : current,
                   Materialized.<Long, CountrySales, KeyValueStore<Bytes, byte[]>>as("top-country-per-item-store")
                               .withValueSerde(countrySalesSerde));
topCountryPerItem.toStream().to("results.top-country-per-item");
```

Every operator the assignment names (`reduce`, `groupBy` vs `groupByKey`,
`join`, `aggregate`, tumbling windows) is used at least once.

---

## 4. Fault tolerance

Cluster:

- 3 brokers in **KRaft** mode (no Zookeeper), each in its own container.
- Each topic: `replication.factor=3`, `min.insync.replicas=2`,
  `partitions=3`.

Producers (`customers-app`, `suppliers-app`):

```properties
acks=all
enable.idempotence=true
retries=2147483647
max.in.flight.requests.per.connection=5
compression.type=lz4
```

Streams app:

```properties
replication.factor=3
num.standby.replicas=1
processing.guarantee=exactly_once_v2
```

Demo for the defense: produce a steady stream, `docker kill broker1`,
show producer/consumer keep going, then show a Streams app instance
takeover from its standby replica.

---

## 5. Project layout

```
mcp/
├── docker-compose.yml                  ← 3 brokers + Postgres + Connect + apps
├── kafka-connect/
│   ├── source-pettypes.json            ← JDBC source connector config
│   ├── source-countries.json
│   └── sinks/                          ← one JDBC sink per result topic
├── doctor-office/                      ← existing Spring Boot REST + MCP
│   └── ... (unchanged + Country entity added)
├── customers-app/                      ← NEW standalone producer
│   ├── pom.xml
│   └── src/main/java/.../CustomersApp.java
├── suppliers-app/                      ← NEW standalone producer
│   ├── pom.xml
│   └── src/main/java/.../SuppliersApp.java
├── streams-app/                        ← NEW standalone Streams app
│   ├── pom.xml
│   └── src/main/java/.../
│       ├── StreamsApp.java
│       ├── topology/MetricsTopology.java
│       ├── model/                      ← Appointment, Supply, Avg, TopProfit, CountrySales
│       └── serdes/JsonSerde.java
└── admin-cli/                          ← NEW CLI
    └── ... (picocli, calls REST)
```

Notice that the existing Spring Boot app is **not** the Streams app.
Streams runs as a separate standalone process — this matches the
assignment's "stand-alone applications" requirement and isolates
fault domains.

---

## 6. What to verify in the defense

- Show the topology with `streams.toString()` printed at startup.
- Show topic descriptions: `kafka-topics --describe` → 3 brokers, IRS=3.
- Kill broker, watch a CLI consumer keep printing.
- Hit REST `/api/metrics/profit/{petTypeId}` and see numbers that match
  the producers' deterministic seed.

Every metric has a corresponding REST endpoint reading from the result
table in Postgres (written there by the Kafka Connect sink).
