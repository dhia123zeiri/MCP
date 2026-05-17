# 5-minute Defense Presentation — Outline

Tight script for a 5-minute slot. Aim for **8 slides**, ~35 seconds each,
leaving 30 seconds of buffer. Both group members should be able to
present any slide.

---

## Slide 1 — Title (15s)

* **Doctor Office EAI** — Kafka Streams + Connect on a Spring Boot/MCP stack
* Group: <names>
* Domain: a veterinary clinic — appointments (sales) and supplies (purchases)

---

## Slide 2 — Domain mapping (30s)

> "The assignment uses a shop. We use a vet clinic with the same shape."

| Shop concept | Our concept |
|---|---|
| Item for sale | PetType (Dog–Husky, Cat–Persian, …) |
| Country | Customer country / Supplier country |
| Sale | **Appointment** (revenue) |
| Purchase | **Supply** (expense) |

This gives revenue, expenses, profit, and a country dimension for #17.

---

## Slide 3 — Architecture (45s)

Architecture diagram (use the one in `README.md`). Highlight:

* **3-broker KRaft Kafka** (no Zookeeper)
* **Standalone Java apps**: customers-app, suppliers-app, streams-app
* **Kafka Connect**: JDBC source for DBInfo, JDBC sink for results
* **Spring Boot doctor-office**: REST + MCP, reads from `res_*` tables
* **Admin CLI** (Python) talks to REST

> "We deliberately separated the Streams app from the Spring Boot app —
> the assignment asks for stand-alone applications, and isolated fault
> domains."

---

## Slide 4 — Topics & data flow (30s)

* `dbinfo.pettypes`, `dbinfo.countries` — fed by **JDBC source connector**
* `appointments` — **customers-app** producer
* `supplies` — **suppliers-app** producer
* `results.*` (13 topics) — **streams-app** sinks; **JDBC sink** lands them in Postgres

All keys: `petTypeId` (Long) — co-partitions appointments & supplies for joins.

---

## Slide 5 — Streams topology highlights (60s)

Show the topology header from `MetricsTopology.java` and walk **3 examples**:

1. **#7 Profit per item** — `revenuePerItem.outerJoin(expensesPerItem)`. Clean
   KTable-KTable outer join; null-safe.
2. **#8 Total revenue** — uses `groupBy((k,v) -> "ALL")`, not `groupByKey`.
   This is the **re-keying** path the assignment explicitly wants.
3. **#17 Top country per item** — composite key `petTypeId|countryCode`,
   re-key by petTypeId, aggregate to keep the max **including the value**.

Cover briefly: tumbling windows (#14–16) use `TimeWindows.ofSizeWithNoGrace`.

---

## Slide 6 — Fault tolerance (45s)

Live demo (or screenshots if recording is risky):

1. `docker compose ps` — 3 brokers up
2. `python admin-cli/admin.py metrics totals` — numbers
3. `docker kill broker1`
4. `python admin-cli/admin.py metrics totals` — **numbers keep growing**
5. Restart broker; show it re-joins the cluster

Key configs called out:

* topics: `replication.factor=3`, `min.insync.replicas=2`
* producers: `acks=all`, `enable.idempotence=true`
* streams: `processing.guarantee=exactly_once_v2`, `num.standby.replicas=1`

---

## Slide 7 — Verification (45s)

Producers use a **deterministic seed** (`DETERMINISTIC_SEED=42`).

Walk through:

* `customers-app` produces N appointments with seed 42
* `suppliers-app` produces M supplies with seed 42
* Sum offline → matches `results.total-revenue` and `results.total-expenses`
* Profit per item shown in Admin CLI matches manual calculation

> "We can stop the producers and verify the totals to the penny."

---

## Slide 8 — Coverage table & wrap (30s)

Project all 17 requirements in a compact table (use the one in `README.md`),
with the operator used for each. Say:

* **All 17 requirements implemented** in the Streams topology.
* **Standard JSON Serde** (custom Jackson-based `JsonSerde<T>`), used in
  `Produced.with`, `Consumed.with`, **and** `Materialized.with`.
* **Source code** zipped & submitted alongside this PDF.

End: *"Questions?"*

---

## Speaker notes — common questions to expect

* **Why JSON not Avro/Protobuf?** Avro/Protobuf would need a schema registry;
  we did serdes without one as the assignment allows. JSON is enough for
  this scale and keeps the demo readable.
* **Why exactly-once?** Requirement #17 (and #14–16) accumulate state; we
  don't want double-counts on rebalance.
* **Why not use the Spring Boot app for Streams?** The assignment is
  explicit: *"stand-alone applications"*. Also, a Streams app with state
  stores should not share a JVM with a web server.
* **How do you handle late events for windowing?** `ofSizeWithNoGrace` —
  the assignment doesn't require grace periods, and it keeps the demo
  deterministic.
* **What about consumer groups?** Every consumer in the system uses a
  group: streams-app's group is its `application.id`, Connect uses a group
  for offsets/configs/status, and our bootstrap consumers use unique group
  IDs so they don't conflict.

## Production tips for the demo run

* Bump `WINDOW_DURATION_MINUTES=1` to keep things visible in 5 minutes.
* Pre-warm: run the stack 30s before the demo so initial Connect tasks are
  RUNNING (not RESTARTING).
* Have **screenshots** of working metrics in case live demo fails.
