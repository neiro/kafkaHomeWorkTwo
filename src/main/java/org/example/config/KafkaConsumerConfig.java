package org.example.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Конфигурационный класс для настройки Kafka Consumer.
 * 
 * Этот класс отвечает за создание и настройку экземпляра {@link KafkaConsumer},
 * используя параметры, определённые в файле настроек.
 */
@Configuration
public class KafkaConsumerConfig {

    /** Адреса Kafka-брокеров, которые используются для подключения. */
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /** ID группы потребителей (consumer group ID), который используется для управления оффсетами и балансировкой нагрузки. */
    @Value("${spring.kafka.consumer.group-id:default-group}")
    private String groupId;

    /**
     * Создаёт и настраивает экземпляр {@link KafkaConsumer} для работы с Kafka.
     * 
     * @return Экземпляр {@link KafkaConsumer} для обработки сообщений.
     */
    @Bean
    public KafkaConsumer<String, String> kafkaConsumer() {
        // Создаём карту свойств для конфигурации Kafka Consumer
        Map<String, Object> props = new HashMap<>();

        // Указываем адреса Kafka-брокеров
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Указываем идентификатор группы потребителей
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        // Указываем классы десериализаторов для ключей и значений сообщений
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Настраиваем поведение при отсутствии доступных оффсетов
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // Начинаем чтение с самого начала

        // Возвращаем экземпляр KafkaConsumer с заданными параметрами
        return new KafkaConsumer<>(props);
    }
}