#!/usr/bin/env bash
# П. 2.4: консольный consumer для топика transactions.
set -euo pipefail
TOPIC="${KAFKA_TOPIC:-transactions}"
CONTAINER="${KAFKA_CONTAINER:-kafka}"
CONSUMER_SH="/opt/kafka/bin/kafka-console-consumer.sh"

docker exec -it "$CONTAINER" "$CONSUMER_SH" \
  --bootstrap-server localhost:9092 \
  --topic "$TOPIC" \
  --from-beginning \
  --property print.timestamp=true
