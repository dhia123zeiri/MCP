1. Run post_connectors.sh (in config folder context)
2. Run producers (*Producer.java)
3. Run Proje3Streams.java


Checks:
1. check with kafka-topics.sh --bootstrap-server broker1:9092 --list
2. see data in postgre container
3. hookup to the producer streams with kafka-console-consumer.sh --bootstrap-server broker1:9092 --include "Proj3SockPurchasesTopic|Proj3SockSalesTopic" --from-beginning