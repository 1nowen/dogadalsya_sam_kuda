# П. 2.4: создать топик `transactions` с retention.ms (7 суток), как в config/application.conf.
# Предусловие: `docker compose up -d` из корня репозитория.
param(
    [string] $Topic = "transactions",
    [string] $Container = "kafka"
)

$ErrorActionPreference = "Stop"
# Образ apache/kafka:3.x — утилиты в /opt/kafka/bin/, не в PATH.
$TopicsSh = "/opt/kafka/bin/kafka-topics.sh"

docker exec $Container $TopicsSh `
    --bootstrap-server localhost:9092 `
    --create --if-not-exists `
    --topic $Topic `
    --partitions 1 `
    --replication-factor 1 `
    --config retention.ms=604800000

Write-Host "Topic '$Topic' is ready (created or already existed)."
