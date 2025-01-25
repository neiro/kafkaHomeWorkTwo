package org.example.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.StreamsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.springframework.boot.ApplicationRunner;
import java.time.Duration;

/**
 * Конфигурационный класс для настройки Kafka Streams и инициализации данных.
 * 
 * Этот класс отвечает за конфигурацию параметров Kafka Streams,
 * проверку существования топиков и загрузку тестовых данных.
 */
@Configuration
public class ProcessorApiConfig {

    /** Логгер для вывода информации и ошибок. */
    private static final Logger logger = LoggerFactory.getLogger(ProcessorApiConfig.class);

    /** Адреса Kafka-брокеров. */
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /** ID приложения Kafka Streams. */
    @Value("${spring.kafka.streams.application-id:kafka_homework_two_app_id}")
    private String applicationId;

    /** Количество потоков для обработки Kafka Streams. */
    @Value("${spring.kafka.streams.num-stream-threads:1}")
    private int numStreamThreads;

    /** Гарантии обработки сообщений. */
    @Value("${spring.kafka.streams.process-guarantee:exactly_once_v2}")
    private String processingGuarantee;

    /** Директория для хранения состояния Kafka Streams. */
    @Value("${spring.kafka.streams.state-dir:/var/lib/kafka-streams-state}")
    private String stateDir;

    /** Название топика для заблокированных пользователей. */
    @Value("${kafka.topic.blockedUsers}")
    private String blockedUsersTopic;

    /** Название топика для запрещённых слов. */
    @Value("${kafka.topic.restrictedWords}")
    private String restrictedWordsTopic;

    /** Максимальное количество попыток для отправки данных. */
    @Value("${kafka.producer.max-retries:3}")
    private int maxRetries;

    /** Таймаут для ожидания ответа при создании топиков. */
    @Value("${kafka.admin.timeout-ms:10000}")
    private int adminTimeoutMs;

    /**
     * Настройки Kafka Streams.
     * 
     * @return Объект {@link Properties}, содержащий настройки Kafka Streams.
     */
    @Bean("kafkaStreamsProperties")
    public Properties kafkaStreamsProperties() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, numStreamThreads);
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, processingGuarantee);
        props.put(StreamsConfig.STATE_DIR_CONFIG, stateDir);
        props.put(StreamsConfig.METRICS_RECORDING_LEVEL_CONFIG, "INFO");
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 100);

        logger.info("Kafka Streams Properties: {}", props);
        return props;
    }

    /**
     * Инициализирует Kafka: проверяет существование топиков и загружает тестовые данные.
     * 
     * @return {@link ApplicationRunner}, выполняющий проверку и загрузку данных при запуске.
     */
    @Bean
    public ApplicationRunner initializeKafka() {
        return args -> {
            ensureTopicsExist();
            initializeTestData();
        };
    }

    /**
     * Создаёт экземпляр {@link KafkaStreams} с заданным топологией и настройками.
     * 
     * @param topology Топология Kafka Streams.
     * @param kafkaStreamsProperties Настройки Kafka Streams.
     * @return Экземпляр {@link KafkaStreams}.
     */
    @Bean(destroyMethod = "close")
    public KafkaStreams kafkaStreams(Topology topology, Properties kafkaStreamsProperties) {
        KafkaStreams streams = new KafkaStreams(topology, kafkaStreamsProperties);

        // Добавляем слушатель изменений состояния
        streams.setStateListener((newState, oldState) -> {
            logger.info("Состояние Kafka Streams изменилось: {} -> {}", oldState, newState);
        });

        // Запускаем Kafka Streams
        streams.start();
        logger.info("Kafka Streams запущен с настройками: {}", kafkaStreamsProperties);

        // Добавляем shutdown hook для корректного завершения
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Инициирована остановка Kafka Streams...");
            streams.close();
        }));

        return streams;
    }

    /**
     * Проверяет существование топиков и создаёт отсутствующие.
     */
    private void ensureTopicsExist() {
        Properties adminProps = new Properties();
        adminProps.put("bootstrap.servers", bootstrapServers);

        try (AdminClient adminClient = AdminClient.create(adminProps)) {
            // Список всех необходимых топиков
            List<NewTopic> topics = List.of(
                new NewTopic(blockedUsersTopic, 1, (short) 1),
                new NewTopic(restrictedWordsTopic, 1, (short) 1)
            );

            // Получение списка существующих топиков
            var existingTopics = adminClient.listTopics().names().get(adminTimeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);

            // Отфильтровываем только те топики, которые ещё не существуют
            List<NewTopic> topicsToCreate = topics.stream()
                .filter(topic -> !existingTopics.contains(topic.name()))
                .toList();

            if (!topicsToCreate.isEmpty()) {
                adminClient.createTopics(topicsToCreate).all().get();
                logger.info("Созданы топики: {}", topicsToCreate.stream().map(NewTopic::name).toList());
            } else {
                logger.info("Все топики уже существуют: {}", topics.stream().map(NewTopic::name).toList());
            }
        } catch (Exception e) {
            logger.error("Ошибка при создании топиков: {}", e.getMessage(), e);
            throw new IllegalStateException("Не удалось создать топики Kafka", e);
        }
    }

    /**
     * Загружает тестовые данные в топики Kafka, если их ещё нет.
     */
    private void initializeTestData() {
        logger.info("Инициализация тестовых данных для Kafka...");

        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("group.id", "test-data-initializer");
        props.put("auto.offset.reset", "earliest");

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props);
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {

            ObjectMapper objectMapper = new ObjectMapper();

            // Сначала читаем существующие данные из топиков
            Map<String, List<String>> existingBlockedUsers = readExistingDataFromTopic(consumer, blockedUsersTopic);
            List<String> existingRestrictedWords = readExistingKeysFromTopic(consumer, restrictedWordsTopic);

            // Данные для топика `blockedUsersTopic`
            Map<String, List<String>> blockedUsersMap = Map.of(
                "alice", List.of("bob", "bob1"),
                "eve", List.of("adam")
            );

            for (Map.Entry<String, List<String>> entry : blockedUsersMap.entrySet()) {
                String user = entry.getKey();
                List<String> blockedUsers = entry.getValue();

                if (!existingBlockedUsers.containsKey(user)) {
                    // Преобразуем список в JSON
                    String jsonValue = objectMapper.writeValueAsString(blockedUsers);
                    producer.send(new ProducerRecord<>(blockedUsersTopic, user, jsonValue));
                    logger.info("Добавлено в топик {}: ключ = {}, значение = {}", blockedUsersTopic, user, jsonValue);
                } else {
                    logger.info("Данные для ключа {} уже существуют в топике {}. Пропускаем запись.", user, blockedUsersTopic);
                }
            }

            // Данные для топика `restrictedWordsTopic`
            List<String> restrictedWords = List.of("111", "222", "333");
            for (String word : restrictedWords) {
                if (!existingRestrictedWords.contains(word)) {
                    producer.send(new ProducerRecord<>(restrictedWordsTopic, word, "ADD"));
                    logger.info("Добавлено в топик {}: запрещённое слово = {}", restrictedWordsTopic, word);
                } else {
                    logger.info("Слово {} уже существует в топике {}. Пропускаем запись.", word, restrictedWordsTopic);
                }
            }

            producer.flush();
        } catch (Exception e) {
            logger.error("Ошибка при инициализации данных Kafka: {}", e.getMessage(), e);
            throw new IllegalStateException("Не удалось инициализировать тестовые данные Kafka", e);
        }

        logger.info("Инициализация тестовых данных завершена.");
    }

    /**
     * Читает существующие данные из топика, возвращая их в виде Map.
     */
    private Map<String, List<String>> readExistingDataFromTopic(KafkaConsumer<String, String> consumer, String topic) {
        consumer.subscribe(Collections.singletonList(topic));
        Map<String, List<String>> data = new HashMap<>();

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        for (ConsumerRecord<String, String> record : records) {
            try {
                List<String> value = new ObjectMapper().readValue(record.value(), new TypeReference<>() {});
                data.put(record.key(), value);
            } catch (Exception e) {
                logger.warn("Не удалось прочитать сообщение из топика {}: ключ = {}, значение = {}", topic, record.key(), record.value(), e);
            }
        }

        return data;
    }

    /**
     * Читает все ключи из топика.
     */
    private List<String> readExistingKeysFromTopic(KafkaConsumer<String, String> consumer, String topic) {
        consumer.subscribe(Collections.singletonList(topic));
        List<String> keys = new ArrayList<>();

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        for (ConsumerRecord<String, String> record : records) {
            keys.add(record.key());
        }

        return keys;
    }

}
