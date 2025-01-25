package org.example.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.model.MessageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Конфигурационный класс для настройки Kafka Producer.
 * 
 * Содержит настройки для создания продюсеров, топиков и шаблонов Kafka.
 */
@Configuration
public class KafkaProducerConfig {

    /** Адреса Kafka-брокеров. */
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /** Название топика для запрещённых слов. */
    @Value("${kafka.topic.restrictedWords:restricted_words}")
    private String restrictedWordsTopic;

    // Названия топиков для приложения
    public static final String MESSAGES_TOPIC = "messages";
    public static final String FILTERED_MESSAGES_TOPIC = "filtered_messages";
    public static final String BLOCKED_USERS_TOPIC = "blocked_users";

    // =================== Определение топиков ===================

    /**
     * Создаёт топик для исходных сообщений.
     * 
     * @return Экземпляр {@link NewTopic} для топика "messages".
     */
    @Bean
    public NewTopic messagesTopic() {
        return new NewTopic(MESSAGES_TOPIC, 1, (short) 1);
    }

    /**
     * Создаёт топик для фильтрованных сообщений.
     * 
     * @return Экземпляр {@link NewTopic} для топика "filtered_messages".
     */
    @Bean
    public NewTopic filteredMessagesTopic() {
        return new NewTopic(FILTERED_MESSAGES_TOPIC, 1, (short) 1);
    }

    /**
     * Создаёт топик для информации о блокировках пользователей.
     * 
     * @return Экземпляр {@link NewTopic} для топика "blocked_users".
     */
    @Bean
    public NewTopic blockedUsersTopic() {
        return new NewTopic(BLOCKED_USERS_TOPIC, 1, (short) 1);
    }

    /**
     * Создаёт топик для запрещённых слов.
     * 
     * @return Экземпляр {@link NewTopic} для топика "restricted_words".
     */
    @Bean
    public NewTopic restrictedWordsTopic() {
        return new NewTopic(restrictedWordsTopic, 1, (short) 1);
    }

    // =================== Общие настройки Producer ===================

    /**
     * Настройки конфигурации для Kafka Producer.
     * 
     * @return Карта настроек для Kafka Producer.
     */
    private Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();

        // Адреса Kafka-брокеров
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Сериализация ключей сообщений
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        return props;
    }

    // =================== ProducerFactory и KafkaTemplate для MessageModel ===================

    /**
     * Создаёт фабрику продюсеров для сообщений типа {@link MessageModel}.
     * 
     * @return Экземпляр {@link ProducerFactory} для сообщений типа {@link MessageModel}.
     */
    @Bean
    public ProducerFactory<String, MessageModel> messageModelProducerFactory() {
        Map<String, Object> props = producerConfigs();

        // Сериализация значений сообщений как JSON
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false); // Удаляем тип-заголовки

        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * Создаёт шаблон Kafka для отправки сообщений типа {@link MessageModel}.
     * 
     * @return Экземпляр {@link KafkaTemplate} для сообщений типа {@link MessageModel}.
     */
    @Bean
    public KafkaTemplate<String, MessageModel> messageModelKafkaTemplate() {
        return new KafkaTemplate<>(messageModelProducerFactory());
    }

    // =================== ProducerFactory и KafkaTemplate для String ===================

    /**
     * Создаёт фабрику продюсеров для строковых сообщений.
     * 
     * @return Экземпляр {@link ProducerFactory} для строковых сообщений.
     */
    @Bean
    public ProducerFactory<String, String> stringProducerFactory() {
        Map<String, Object> props = producerConfigs();

        // Сериализация значений сообщений как строки
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * Создаёт шаблон Kafka для отправки строковых сообщений.
     * 
     * @return Экземпляр {@link KafkaTemplate} для строковых сообщений.
     */
    @Bean
    public KafkaTemplate<String, String> stringKafkaTemplate() {
        return new KafkaTemplate<>(stringProducerFactory());
    }

    // =================== ProducerFactory и KafkaTemplate для List<String> ===================

    /**
     * Создаёт фабрику продюсеров для сообщений типа List<String>.
     * 
     * @return Экземпляр {@link ProducerFactory} для сообщений типа List<String>.
     */
    @Bean
    public ProducerFactory<String, List<String>> listProducerFactory() {
        Map<String, Object> props = producerConfigs();

        // Сериализация значений сообщений как JSON
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * Создаёт шаблон Kafka для отправки сообщений типа List<String>.
     * 
     * @return Экземпляр {@link KafkaTemplate} для сообщений типа List<String>.
     */
    @Bean
    public KafkaTemplate<String, List<String>> listKafkaTemplate() {
        return new KafkaTemplate<>(listProducerFactory());
    }
}
