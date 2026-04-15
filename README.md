# Payment Transaction Stream Simulation

Сквозная демонстрация потоковой обработки платёжных транзакций:

- **Producer** (ZIO + Kafka) генерирует случайные транзакции и отправляет их в Kafka.
- **Consumer** (Spark Structured Streaming) читает поток, фильтрует корректные записи, считает выручку и пишет аудит.

## Требования

- JDK 17, [sbt](https://www.scala-sbt.org/), [Docker](https://docs.docker.com/get-docker/)

## Запуск тестов

`sbt test` (или `tests.bat`) проходит без Kafka и без отдельной настройки Hadoop: в consumer Mockito имитирует пакет JSON-строк (`ConsumerMockSpec`), отдельно гоняются небольшие Spark-проверки на локальном наборе строк (`TransactionStreamProcessorSpec`). На Windows в логах Spark может появляться предупреждение про winutils — на эти тесты это обычно не влияет; при сбоях файловой системы удобнее WSL2 или Linux в Docker.

Ниже — все **6** тестов; в выводе `sbt` рядом с ними дублируются формулировки из исходников (имя класса и текст проверки в коде).

### 1. `DataDictionariesSpec` — выбор из словаря (`pick`)

- **Что проверяет:** функция `DataDictionaries.pick` действительно выбирает элемент из непустой последовательности и не принимает пустой список.
- **Как проверяет:** для фиксированного `Random(99L)` и `Seq("a","b","c")` вызывается `pick`; результат должен входить в исходную последовательность. Для `Seq.empty` ожидается `IllegalArgumentException`, текст сообщения содержит подстроку про пустой ввод.

### 2. `TransactionJsonSpec` — кодек JSON для транзакции

- **Что проверяет:** схема JSON для `Transaction` (zio-json) не теряет поля при сериализации и обратном разборе.
- **Как проверяет:** собирается эталонный `Transaction` с фиксированным моментом времени; строка из `toJson` парсится через `fromJson[Transaction]`; результат сравнивается с исходным значением.

### 3. `TransactionGeneratorSpec` — сборка транзакции генератором

- **Что проверяет:** детерминированная генерация `buildTransaction(Random(seed))` укладывается в словари домена и правила суммы.
- **Как проверяет:** цикл по `seed` от 0 до 199; для каждой строки проверяется принадлежность `user` / `productType` / `eventType` словарям, `category` — списку категорий для типа продукта; `amount` либо 0, либо в диапазоне 1…1000; `timestamp` не `null`.

### 4. `ConsumerMockSpec` — разбор пакета JSON под видом Kafka

- **Что проверяет:** общая логика разбора пакета JSON-строк (как после брокера) совпадает с правилами consumer: принять только `amount > 0`, отделить некорректные суммы и битый JSON; выручка по принятым строкам.
- **Как проверяет:** Mockito подменяет `KafkaJsonBatchSource.fetch()` тремя строками: валидная транзакция с `amount = 10`, JSON с `amount = 0`, строка не по схеме транзакции. Вызывается `TransactionJsonLines.partition`; проверяются списки `accepted`, `rejectedNonPositive`, `invalidJson` и `totalRevenue == 10L`.

### 5. `TransactionStreamProcessorSpec` — фильтр положительной суммы

- **Что проверяет:** Spark-фильтр `filterPositiveAmount` оставляет только строки с `amount > 0` (как в потоковом пайплайне).
- **Как проверяет:** локальная сессия Spark (`local[1]`); таблица с одной колонкой `amount` и строками `0`, `5`, `-1`, `null`; после фильтра в множестве значений остаётся только `5`.

### 6. `TransactionStreamProcessorSpec` — итоговая выручка за пакет данных

- **Что проверяет:** агрегат `aggregateTotals` даёт сумму колонки `amount` в одной строке с полем `totalRevenue`.
- **Как проверяет:** локальная сессия Spark; таблица с `amount` 10, 20, 5; вызывается `aggregateTotals`, первая строка результата читается как `totalRevenue` с учётом возможного числового типа в Spark и сравнивается с 35.

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
├── build.sbt                    # Три модуля: common, producerApp, consumerApp
├── tests.bat                    # Windows: одна команда `sbt test`
├── run.bat                      # Windows: образы → compose → поток логов
├── project/
│   ├── Dependencies.scala       # Версии; Log4j2 только у consumer (Spark); у producer — slf4j-nop для kafka-clients
│   ├── plugins.sbt              # sbt-native-packager, scalafmt
│   └── build.properties         # Версия sbt
├── config/
│   └── application.conf         # HOCON; значения из переменных окружения
├── docker-compose.yml           # Kafka, producer, consumer; тома checkpoint и output
├── .dockerignore
├── .gitignore
├── .scalafmt.conf
├── .scalafmtignore
├── common/                      # Общий код producer и consumer
│   └── src/
│       ├── main/scala/com/paymentstream/
│       │   ├── config/
│       │   │   ├── AppConfig.scala          # корневая модель HOCON (kafka, producer, spark)
│       │   │   └── ConfigLoader.scala       # загрузка PureConfig
│       │   ├── data/
│       │   │   └── DataDictionaries.scala   # словари и pick для генератора
│       │   ├── generator/
│       │   │   └── TransactionGenerator.scala   # сборка случайной Transaction под продьюсер
│       │   ├── kafka/
│       │   │   └── KafkaProducerConfig.scala    # параметры клиента и топика из AppConfig
│       │   └── model/
│       │       ├── Transaction.scala            # кейс-класс; кодеки zio-json
│       │       └── TransactionJsonLines.scala   # разбор пакета JSON-строк без Spark (общая логика с consumer)
│       └── test/scala/com/paymentstream/
│           ├── data/
│           │   └── DataDictionariesSpec.scala # тест DataDictionaries.pick
│           ├── generator/
│           │   └── TransactionGeneratorSpec.scala # доменные ограничения buildTransaction
│           └── model/
│               └── TransactionJsonSpec.scala  # круговой toJson / fromJson для Transaction
├── producer/
│   └── src/main/scala/com/paymentstream/producer/
│       ├── ProducerMain.scala           # точка входа ZIO
│       └── TransactionProducer.scala    # генерация и отправка в Kafka
├── consumer/
│   └── src/
│       ├── main/scala/com/paymentstream/
│       │   ├── consumer/
│       │   │   └── ConsumerMain.scala     # Structured Streaming, микробатчи
│       │   └── spark/
│       │       ├── SparkConsumerConfig.scala  # объединение секций spark + kafka из AppConfig
│       │       └── TransactionStreamProcessor.scala   # источник из Kafka, разбор JSON, фильтры, агрегаты
│       └── test/scala/com/paymentstream/
│           ├── consumer/
│           │   └── ConsumerMockSpec.scala   # мок источника JSON; трейт KafkaJsonBatchSource в том же файле
│           └── spark/
│               └── TransactionStreamProcessorSpec.scala   # локальный Spark: фильтр по сумме, итоговая выручка
├── checkpoint/                  # том с хоста для Docker; в репозитории только заглушка
│   └── .gitkeep
└── output/                      # аудит; в репозитории только заглушка, при работе — part-* от Spark
    ├── .gitkeep
    ├── transactions.log/
    └── rejected-transactions/
```
