#!/usr/bin/env bash
# П. 2.4: создать топик transactions с retention.ms (7 суток).
set -euo pipefail
TOPIC="${KAFKA_TOPIC:-transactions}"
CONTAINER="${KAFKA_CONTAINER:-kafka}"
TOPICS_SH="/opt/kafka/bin/kafka-topics.sh"

docker exec "$CONTAINER" "$TOPICS_SH" \
  --bootstrap-server localhost:9092 \
  --create --if-not-exists \
  --topic "$TOPIC" \
  --partitions 1 \
  --replication-factor 1 \
  --config retention.ms=604800000

echo "Topic '$TOPIC' is ready (created or already existed)."
