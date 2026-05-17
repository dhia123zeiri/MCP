# Doctor Office EAI — Assignment #3

Message-Oriented Middleware (Apache Kafka) + Kafka Streams + Kafka Connect,
on top of a Spring Boot REST/MCP application backed by PostgreSQL.

> See `KAFKA_ARCHITECTURE.md` for the design rationale and the operator-by-operator
> mapping of the 17 assignment requirements.

## Components

```
                        ┌────────────────────┐
                        │ Admin CLI (python) │
                        └─────────┬──────────┘
                                  │  REST
                                  ▼
                        ┌─────────────────────┐       ┌──────────────┐
                        │   doctor-office     │◄─────►│  PostgreSQL  │
                        │   (Spring Boot)     │       │ (countries,  │
                        │   REST + MCP        │       │  pet_types,  │
                        └────────┬────────────┘       │  res_*)      │
                                 │ (DBInfo bootstrap)  └──────┬───────┘
                                 ▼                            ▲
                        ┌────────────────────────────────────┐│
                        │ dbinfo.pettypes / dbinfo.countries │├── Kafka Connect (JDBC source/sink)
                        └────────────────────────────────────┘│
                                                              │
   ┌──────────────┐   appointments      results.*             │
   │ customers-app├──────────────► Kafka ──────► streams-app ─┘
   └──────────────┘                  │             (Kafka Streams)
                                     │
   ┌──────────────┐   supplies        │
   │ suppliers-app├──────────────────►│
   └──────────────┘                    \
                                        \  3-broker KRaft cluster
                                         \  (broker1, broker2, broker3)
                                          \  rf=3  min.isr=2
```

## Run everything

Prereqs: Docker Desktop, curl + jq for the helper script (Linux/macOS) or
PowerShell (Windows).

```bash
# 1. Start the whole stack
docker compose up -d --build

# 2. Watch services come up
docker compose ps
docker compose logs -f topic-init   # waits for topics to be created

# 3. Register Kafka Connect source + sink connectors
#    Linux/macOS:
bash kafka-connect/register-connectors.sh
#    Windows:
powershell -ExecutionPolicy Bypass -File kafka-connect/register-connectors.ps1

# 4. Open the Admin CLI
python admin-cli/admin.py            # interactive menu
# or
python admin-cli/admin.py metrics totals
```

Inside the menu (or via subcommands) you can add countries (req. #1),
list countries (#2), add pet types (#3), list pet types (#4), and read
each of the stream-computed metrics (#5 through #17).

## Verifying the metrics

The producers run with a deterministic seed (default 42). Stop them after
N seconds and the totals are reproducible. For a stronger verification:

```bash
# Tail any result topic directly:
docker exec -it broker1 /opt/kafka/bin/kafka-console-consumer.sh \
   --bootstrap-server localhost:9092 \
   --topic results.profit-per-item \
   --property print.key=true --from-beginning
```

## Fault-tolerance demo

```bash
# 1. Confirm everything is healthy
python admin-cli/admin.py metrics totals

# 2. Kill broker 1
docker kill broker1

# 3. The producers keep producing and the streams app keeps computing —
#    you'll see a brief rebalance in the logs, then steady state.
python admin-cli/admin.py metrics totals   # numbers keep growing

# 4. Restart broker
docker start broker1
```

What makes this work:

- **3 brokers, KRaft mode** (no Zookeeper, per the assignment).
- **All topics** created with `replication.factor=3`, `min.insync.replicas=2`.
- **Producers**: `acks=all`, `enable.idempotence=true`, `retries=Integer.MAX_VALUE`.
- **Streams app**: `processing.guarantee=exactly_once_v2`,
  `num.standby.replicas=1`, `replication.factor=3`.

## Requirements coverage

| #   | Requirement                                  | Where it lives                                          | Operator       |
|-----|----------------------------------------------|---------------------------------------------------------|----------------|
| 1   | Add countries                                | `ReferenceDataController` POST `/api/refdata/countries` | —              |
| 2   | List countries                               | `ReferenceDataController` GET  `/api/refdata/countries` | —              |
| 3   | Add pet types (items)                        | `ReferenceDataController` POST `/api/refdata/pettypes`  | —              |
| 4   | List pet types                               | `ReferenceDataController` GET  `/api/refdata/pettypes`  | —              |
| 5   | Revenue per item                             | `MetricsTopology` → `results.revenue-per-item`          | `reduce()`     |
| 6   | Expenses per item                            | `MetricsTopology` → `results.expenses-per-item`         | `reduce()`     |
| 7   | Profit per item                              | `MetricsTopology` → `results.profit-per-item`           | `join()`       |
| 8   | Total revenues                               | `MetricsTopology` → `results.total-revenue`             | `groupBy()`    |
| 9   | Total expenses                               | `MetricsTopology` → `results.total-expenses`            | `groupBy()`    |
| 10  | Total profit                                 | `MetricsTopology` → `results.total-profit`              | `join()`       |
| 11  | Avg per appointment, per item                | `MetricsTopology` → `results.avg-per-appointment-by-item` | `aggregate()` |
| 12  | Avg per appointment, all items               | `MetricsTopology` → `results.avg-per-appointment-all`   | `groupBy() + aggregate()` |
| 13  | Item with highest profit                     | `MetricsTopology` → `results.top-profit-item`           | `aggregate()`  |
| 14  | Windowed revenue (1h tumbling)               | `MetricsTopology` → `results.windowed-revenue`          | `TimeWindows`  |
| 15  | Windowed expenses                            | `MetricsTopology` → `results.windowed-expenses`         | `TimeWindows`  |
| 16  | Windowed profit                              | `MetricsTopology` → `results.windowed-profit`           | `TimeWindows`  |
| 17  | Top country per item (+ value)               | `MetricsTopology` → `results.top-country-per-item`      | `aggregate()` w/ composite value |

## Project layout

```
mcp/
├── docker-compose.yml          ← 3 KRaft brokers + Postgres + Connect + apps
├── KAFKA_ARCHITECTURE.md       ← Design doc
├── README.md                   ← (this file)
├── PRESENTATION_OUTLINE.md     ← 5-minute defense plan
│
├── db-init/                    ← Postgres bootstrap (countries, result tables)
├── kafka-connect/              ← JDBC source + sink connector configs
│
├── doctor-office/              ← Spring Boot REST + MCP server (existing app, augmented)
│   ├── Dockerfile
│   └── src/main/java/.../
│       ├── controller/ReferenceDataController.java   (#1-#4)
│       ├── controller/MetricsController.java         (read #5-#17)
│       ├── entity/Country.java
│       └── service/KafkaPublisherService.java         (DBInfo bootstrap)
│
├── customers-app/              ← Standalone producer → appointments topic
├── suppliers-app/              ← Standalone producer → supplies topic
├── streams-app/                ← Standalone Kafka Streams app (the 17 metrics)
└── admin-cli/                  ← Python CLI talking to REST
```

## Notes

- The Spring Boot app's old Kafka Streams placeholder is gone (it would
  have conflicted with the real `streams-app`).
- The `streams-app` window size defaults to **1 minute** for fast
  demonstration. Set `WINDOW_DURATION_MINUTES=60` in `docker-compose.yml`
  for the real 1-hour windows mentioned in the assignment.
