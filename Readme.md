# Итоговый проект первого модуля Kafka 

## Описание

Проект состоит из продюсера и двух консьюмеров для работы с Kafka-топиком `my-topic`. Один консьюмер использует pull-модель, а другой эмулирует push-модель.

## Структура проекта

- `docker-compose.yml` – настройка Kafka-кластера.
- `ProducerApp.java` – продюсер для отправки сообщений.
- `ConsumerPull.java` – консьюмер с pull-моделью.
- `ConsumerPush.java` – консьюмер с эмулированной push-моделью.
- `Message.java` – класс сообщения с сериализацией и десериализацией.
- `topic.txt` – команды для создания и описания топика.

## Запуск проекта

1. **Запуск Kafka-кластера over Kraft:**

    ```bash
    docker-compose up -d
    ```

2. **Создание топика:**

    Выполните команды из `topic.txt`:

    ```bash
    docker exec -it kafka-0 /opt/bitnami/kafka/bin/kafka-topics.sh --create --topic my-topic --bootstrap-server kafka-0:9092,kafka-1:9092,kafka-2:9092 --partitions 3 --replication-factor 2

    docker exec -it kafka-0 /opt/bitnami/kafka/bin/kafka-topics.sh --describe --topic my-topic --bootstrap-server kafka-0:9092
    ```

docker exec -it kafka-0: Выполнение команды внутри контейнера kafkahomework-kafka-0-1.
/opt/bitnami/kafka/bin/kafka-topics.sh --create --topic my-topic: Создание топика с именем my-topic.
--bootstrap-server kafka-0:9092,kafka-1:9092,kafka-2:9092: Использование трёх брокеров (kafka-0, kafka-1, kafka-2) для установления начального соединения с кластером.
--partitions 3: Создание топика с 3 партициями.
--replication-factor 2: Установка фактора репликации равным 2, что означает, что каждая партиция будет иметь 2 реплики на разных брокерах для обеспечения отказоустойчивости

после команды проверки должно отобразиться сообщение подобного вида:
Topic: my-topic TopicId: OVXBUJKOTASUNN7fuP9zlw PartitionCount: 3       ReplicationFactor: 2    Configs:                                                                                                                   
Topic: my-topic Partition: 0    Leader: 0       Replicas: 0,1   Isr: 1,0                                                                                                                                           
Topic: my-topic Partition: 1    Leader: 1       Replicas: 1,2   Isr: 1,2                                                                                                                                           
Topic: my-topic Partition: 2    Leader: 2       Replicas: 2,0   Isr: 0,2  

3. **Запуск консьюмеров:**

    В отдельных терминалах запустите:

    ```bash
    java -cp target/kafka-home-work-1.0-jar-with-dependencies.jar kafka.consumer.ConsumerPull
    ```
    и
    ```bash
    java -cp target/kafka-home-work-1.0-jar-with-dependencies.jar kafka.consumer.ConsumerPush
    ```

    если запуск в windows, то рекомендую вызвать chcp 65001 для корректного вывода рускоязычных сообщений в консоль (по другому пока не знаю как в ява )

4. **Запуск продюсера:**

    В отдельном терминале запустите:

    ```bash
    java -cp target/kafka-home-work-1.0-jar-with-dependencies.jar kafka.producer.ProducerApp
    ```
    если запуск в windows, то рекомендую вызвать chcp 65001 для корректного вывода рускоязычных сообщений в консоль (по другому пока не знаю как в ява )


## Проверка работы

- **Продюсер** выводит отправленные сообщения (автоматически сгенерированный json). Скорость вывода можно настривать.
- **ConsumerPull** выводит полученные сообщения и подтверждает смещения вручную.
- **ConsumerPush** выводит полученные сообщения сразу после получения, используя автокоммит.

## Параметры конфигурации

- **Producer:**
  - `BOOTSTRAP_SERVERS = "127.0.0.1:9094,127.0.0.1:9095,127.0.0.1:9096"` - Адреса брокеров Kafka, к которым будет подключаться Producer (несколкьо для надежности)
  - `ACKS = "all"` - Гарантирует, что сообщение записано на все реплики, которые указаны в параметре min.insync.replicas
  - `RETRIES = 3` - Максимальное количество ретраев при сбоях
  - `IDEMPOTENCE = true` - Включение идемпотентности для предотвращения дублирования сообщений
  - `RETRY_BACKOFF_MS = 1000` - Задержка между повторными попытками отправки сообщений (в миллисекундах)
  - `DELIVERY_TIMEOUT_MS = 120000` - Таймаут доставки сообщения (в миллисекундах)
  - `MESSAGES_PER_SECOND = 3` - Количество сообщений, отправляемых в секунду
  - At Least Once достигается за счет `ACKS = "all"` и `IDEMPOTENCE = true` в продюсере и ENABLE_AUTO_COMMIT_CONFIG в консьюмере со значением true
  - Подробные комментарии логики смотреть в классе src\main\java\kafka\producer\ProducerApp.java


- **ConsumerPull:**
  - `BOOTSTRAP_SERVERS = "127.0.0.1:9094,127.0.0.1:9095,127.0.0.1:9096` - Адреса брокеров Kafka, к которым будет подключаться Consumer (несколкьо для надежности)
  - `GROUP_ID = "consumer-pull-group"` - Уникальный идентификатор группы консьюмеров для совместного чтения топика
  - `OFFSET_RESET = "earliest"` - Указывает, с какого места начинать чтение, если не существует сохранённого смещения
  - `AUTO_COMMIT = false` - Отключение автокоммитм
  - `FETCH_MIN_BYTES = 1 * 1024 * 1024; // 1 MB` - Минимальный объём данных (в байтах), который должен быть доступен для чтения за один запрос
  - `FETCH_MAX_WAIT_MS = 5000; // 5 секунд` - Максимальное время ожидания новых данных до выполнения poll()
  - `POLL_TIMEOUT = Duration.ofMillis(5000)` - Таймаут для метода poll, который определяет, как долго ждать новых данных
  - `MAX_RETRIES = 3` - Максимальное количество попыток повторной обработки сообщения при ошибке
  - `RETRY_DELAY_MS = 1000` - Задержка между повторными попытками обработки сообщения (в миллисекундах)
  - Ручной коммит смещений после обработки сообщений. Классическое поведение консюмера в кафке
  - At Least Once достигается за счет `ACKS = "all"` и `IDEMPOTENCE = true` в продюсере и consumer.commitSync() в консьюмере в секции "Ручной коммит смещений после успешной обработки сообщений"
  - Подробные комментарии логики смотреть в классе src\main\java\kafka\consumer\ConsumerPull.java


- **ConsumerPush:**
  - `BOOTSTRAP_SERVERS = "127.0.0.1:9094,127.0.0.1:9095,127.0.0.1:9096"` - Адреса брокеров Kafka, к которым будет подключаться Consumer (несколкьо для надежности)
  - `GROUP_ID = "consumer-push-group"` - Уникальный идентификатор группы консьюмеров для совместного чтения топика
  - `OFFSET_RESET = "earliest"` - Указывает, с какого места начинать чтение, если не существует сохранённого смещения
  - `AUTO_COMMIT = true` - Включение автокоммита
  - `POLL_TIMEOUT = Duration.ofMillis(100)` - Таймаут для метода poll, который определяет, как долго ждать новых данных
  - `MAX_RETRIES = 3` - Максимальное количество попыток повторной обработки сообщения при ошибке
  - `RETRY_DELAY_MS = 1000` - Задержка между повторными попытками обработки сообщения (в миллисекундах)
  - Эмулирует push-модель через обработку сообщений сразу после получения. Для этого создается отдельный поток и с помощю параметра POLL_TIMEOUT = 100 мс извлекаем сообщения из топика, что выглядит как будто бы собщения обрабатываются сразу по мере их поступления в топик 
  - Подробные комментарии логики смотреть в классе src\main\java\kafka\consumer\ConsumerPush.java

## Зависимости

- Apache Kafka
- Jackson Databind
- Jackson Core
- SLF4J API
- Logback Classic
- Java Faker


