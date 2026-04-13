# П. 2.4: описание топика — партиции, retention, ISR (проверка после продюсера).
param(
    [string] $Topic = "transactions",
    [string] $Container = "kafka"
)

$ErrorActionPreference = "Stop"
$TopicsSh = "/opt/kafka/bin/kafka-topics.sh"

docker exec $Container $TopicsSh `
    --bootstrap-server localhost:9092 `
    --describe --topic $Topic
