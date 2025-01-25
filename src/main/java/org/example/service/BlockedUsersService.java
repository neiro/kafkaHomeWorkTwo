package org.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.example.model.UserBlockInfo;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.StoreQueryParameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Сервис для управления списками заблокированных пользователей.
 * 
 * Содержит методы для добавления, удаления, получения заблокированных пользователей,
 * а также для работы с Kafka и State Store.
 */
@Service
public class BlockedUsersService {

    /**
     * Экземпляр KafkaStreams для работы с State Store.
     */
    private final KafkaStreams kafkaStreams;

    /**
     * KafkaTemplate для отправки сообщений в Kafka.
     */
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Имя State Store для хранения заблокированных пользователей.
     */
    private final String blockedUsersStore;

    /**
     * Название топика Kafka для заблокированных пользователей.
     */
    private final String blockedUsersTopic;

    /**
     * ObjectMapper для сериализации и десериализации JSON.
     */
    private final ObjectMapper objectMapper;

    /**
     * Логгер для записи действий сервиса.
     */
    private static final Logger logger = LoggerFactory.getLogger(BlockedUsersService.class);

    /**
     * Конструктор сервиса.
     * 
     * @param kafkaStreams       Экземпляр KafkaStreams для работы с State Store.
     * @param kafkaTemplate      KafkaTemplate для отправки сообщений.
     * @param blockedUsersStore  Имя State Store.
     * @param blockedUsersTopic  Название топика Kafka.
     * @param objectMapper       ObjectMapper для работы с JSON.
     */
    public BlockedUsersService(
        KafkaStreams kafkaStreams,
        KafkaTemplate<String, String> kafkaTemplate,
        @Value("${kafka.stateStore.blockedUsersStore}") String blockedUsersStore,
        @Value("${kafka.topic.blockedUsers}") String blockedUsersTopic,
        ObjectMapper objectMapper
    ) {
        this.kafkaStreams = kafkaStreams;
        this.kafkaTemplate = kafkaTemplate;
        this.blockedUsersStore = blockedUsersStore;
        this.blockedUsersTopic = blockedUsersTopic;
        this.objectMapper = objectMapper;
    }



    /**
     * Получение списка заблокированных пользователей для конкретного пользователя.
     * 
     * @param recipientId ID пользователя.
     * @return Список заблокированных пользователей.
     */
    public List<String> getBlockedUsers(String recipientId) {
        try {
            // Получаем доступ к State Store (read-only)
            ReadOnlyKeyValueStore<String, List<String>> store = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType(blockedUsersStore, QueryableStoreTypes.keyValueStore())
            );

            // Возвращаем список заблокированных пользователей или пустой список, если данных нет
            List<String> blockedUsers = store.get(recipientId);
            return blockedUsers != null ? blockedUsers : new ArrayList<>();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при чтении списка заблокированных пользователей для пользователя: " + recipientId, e);
        }
    }

    /**
     * Получение всех заблокированных пользователей в системе.
     * 
     * @return Список всех пользователей и их заблокированных пользователей.
     */
    public List<UserBlockInfo> getAllBlockedUsers() {
        if (kafkaStreams.state() != KafkaStreams.State.RUNNING) {
            String errorMsg = "Kafka Streams не запущен. Текущее состояние сервиса: " + kafkaStreams.state();
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        ReadOnlyKeyValueStore<String, List<String>> store;
        try {
            store = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType(blockedUsersStore, QueryableStoreTypes.keyValueStore())
            );
        } catch (Exception e) {
            String errorMsg = "Ошибка при доступе к State Store: " + blockedUsersStore;
            logger.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }

        List<UserBlockInfo> allBlockedUsers = new ArrayList<>();
        try (var iterator = store.all()) {
            iterator.forEachRemaining(entry -> {
                List<String> blockedList = entry.value != null ? entry.value : new ArrayList<>();
                allBlockedUsers.add(new UserBlockInfo(entry.key, blockedList));
            });

        } catch (Exception e) {
            String errorMsg = "Ошибка при получении всех блокировок из State Store";
            logger.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }

        return allBlockedUsers;
    }
}
