package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Сервис для управления списком запрещённых слов.
 * 
 * Предоставляет методы для добавления, удаления и получения запрещённых слов,
 * а также взаимодействует с Kafka для обработки событий изменения списка.
 */
@Service
public class RestrictedWordsService {

    /**
     * KafkaTemplate используется для отправки сообщений в Kafka.
     */
    private final KafkaTemplate<String, String> kafkaTemplate;
       /**
     * Логгер для записи действий сервиса.
     */
    private static final Logger logger = LoggerFactory.getLogger(BlockedUsersService.class);

    /**
     * Название топика Kafka для запрещённых слов.
     */
    @Value("${kafka.topic.restrictedWords}")
    private String restrictedWordsTopic;

    /**
     * Локальный кеш для хранения запрещённых слов.
     * Используется для ускорения операций чтения.
     */
    private final Set<String> restrictedWords = Collections.synchronizedSet(new HashSet<>());

    /**
     * Конструктор сервиса.
     * 
     * @param kafkaTemplate Экземпляр KafkaTemplate для отправки сообщений в Kafka.
     */
    public RestrictedWordsService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Добавляет новое запрещённое слово.
     * 
     * @param word Слово, которое нужно добавить в список запрещённых.
     *             Отправляет событие в Kafka с ключом слова и значением "ADD".
     */
    public void addWord(String word) {
        // Обновляем локальный кеш
        restrictedWords.add(word);
        logger.info("Слово '{}' добавлено в локальный кеш.", word);

        // Отправляем событие добавления слова в Kafka
        kafkaTemplate.send(restrictedWordsTopic, word, "ADD");
        logger.info("Событие 'ADD' для слова '{}' отправлено в Kafka.", word);
    }

    /**
     * Удаляет запрещённое слово из списка.
     * 
     * @param word Слово, которое нужно удалить из списка запрещённых.
     *             Отправляет событие в Kafka с ключом слова и значением null.
     */
    public void deleteWord(String word) {
        // Обновляем локальный кеш
        restrictedWords.remove(word);
        logger.info("Слово '{}' удалено из локального кеша.", word);

        // Отправляем событие удаления слова в Kafka
        kafkaTemplate.send(restrictedWordsTopic, word, null);
        logger.info("Событие 'DELETE' для слова '{}' отправлено в Kafka.", word);
    }

    /**
     * Возвращает текущий список запрещённых слов.
     * 
     * @return Невозможный для изменения набор запрещённых слов, хранящийся в локальном кеше.
     */
    public Set<String> getRestrictedWords() {
        // Возвращаем копию набора для предотвращения модификаций извне
        return Collections.unmodifiableSet(restrictedWords);
    }

        /**
     * Метод для обработки изменений из Kafka.
     * Синхронизирует локальный кеш с изменениями, поступившими через события Kafka.
     * 
     * @param word  Слово, которое нужно обработать.
     * @param event Тип события ("ADD" или "DELETE").
     */
    public void handleKafkaEvent(String word, String event) {
        if ("ADD".equalsIgnoreCase(event)) {
            restrictedWords.add(word);
            logger.info("Слово '{}' добавлено в локальный кеш приложения из Kafka.", word);
        } else if (event == null || "DELETE".equalsIgnoreCase(event)) {
            restrictedWords.remove(word);
            logger.info("Слово '{}' удалено из локального кеша приложения из Kafka.", word);
        } else {
            logger.warn("Неизвестное событие Kafka для слова '{}': {}", word, event);
        }
    }
}
