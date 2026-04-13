# Payment Transaction Stream Simulation

Сквозная демонстрация потоковой обработки платёжных транзакций:

- **Producer** (ZIO + Kafka) генерирует случайные транзакции и отправляет их в Kafka.
- **Consumer** (Spark Structured Streaming) читает поток, фильтрует корректные записи, считает выручку и пишет аудит.

## Требования

- JDK 17, [sbt](https://www.scala-sbt.org/), [Docker](https://docs.docker.com/get-docker/) 

## Быстрый запуск

### Windows

Запустите `run.bat` из корня репозитория.

Скрипт:

1. Собирает образы `producer:latest` и `consumer:latest` через `sbt-native-packager`.
2. Останавливает предыдущий стек и поднимает Kafka, Producer и Consumer с пересозданием контейнеров.
3. Выводит объединённый поток логов сервисов в консоль.

Аудит и чекпойнт по умолчанию пишутся в **`output/`** и **`checkpoint/`** в корне проекта (см. `application.conf`; в Docker те же пути проброшены в контейнер в `docker-compose.yml`).

## Структура проекта

```text
├── build.sbt                    # Многомодульная сборка: common, producerApp, consumerApp
├── project/
│   ├── Dependencies.scala       # object Versions + object Dependencies
│   ├── plugins.sbt              # sbt-native-packager, sbt-scalafmt
│   └── build.properties         # Версия sbt
├── config/
│   └── application.conf         # HOCON + подстановки из ENV
├── docker-compose.yml           # Kafka, producer, consumer; тома checkpoint/output
├── run.bat                      # Windows: образы → compose down/up → логи
├── .scalafmt.conf
├── common/
│   └── src/main/scala/com/paymentstream/
│       ├── config/              # AppConfig, ConfigLoader (PureConfig)
│       ├── model/               # Transaction (zio-json)
│       ├── data/                # DataDictionaries
│       ├── generator/           # TransactionGenerator (ZIO)
│       └── kafka/               # KafkaProducerConfig
├── producer/
│   └── src/main/scala/com/paymentstream/producer/
│       ├── ProducerMain.scala   # ZIOAppDefault
│       └── TransactionProducer.scala
├── consumer/
│   └── src/main/scala/com/paymentstream/
│       ├── consumer/
│       │   └── ConsumerMain.scala
│       └── spark/
│           ├── SparkConsumerConfig.scala
│           └── TransactionStreamProcessor.scala
├── checkpoint/                  # Чекпойнт Spark Streaming (bind mount)
└── output/                      # Аудит в Docker: каталоги с part-* файлами Spark
    ├── transactions.log/
    └── rejected-transactions/
```
