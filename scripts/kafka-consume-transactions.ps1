# П. 2.4: консольный consumer — сообщения в JSON из топика `transactions` (--from-beginning).
# Останов: Ctrl+C. Имя топика должно совпадать с kafka.topic в application.conf.
param(
    [string] $Topic = "transactions",
    [string] $Container = "kafka"
)

$ErrorActionPreference = "Stop"
$ConsumerSh = "/opt/kafka/bin/kafka-console-consumer.sh"

docker exec -it $Container $ConsumerSh `
    --bootstrap-server localhost:9092 `
    --topic $Topic `
    --from-beginning `
    --property print.timestamp=true
