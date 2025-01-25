package org.example.topology;

import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.api.*;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.example.model.MessageModel;
import org.apache.kafka.streams.processor.api.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.kafka.streams.KeyValue;

/**
 * Процессор для фильтрации и цензуры сообщений.
 * 
 * Основные задачи:
 * - Проверка, заблокирован ли отправитель у получателя.
 * - Цензура сообщений, содержащих запрещённые слова.
 * - Периодическое обновление паттерна запрещённых слов из хранилища.
 */
public class BlockAndCensorProcessor extends ContextualProcessor<String, MessageModel, String, MessageModel> {

    /** Логгер для записи действий процессора. */
    private static final Logger logger = LoggerFactory.getLogger(BlockAndCensorProcessor.class);

    /** Имя State Store для хранения информации о заблокированных пользователях. */
    private final String blockedUsersStoreName;

    /** Имя State Store для хранения запрещённых слов. */
    private final String restrictedWordsStoreName;

    /** Предыдущий список запрещённых слов (для сравнения изменений). */
    private Set<String> previousForbiddenWords = new HashSet<>();

    /** State Store для хранения заблокированных пользователей. */
    private KeyValueStore<String, List<String>> blockedUsersStore;

    /** State Store для хранения запрещённых слов. */
    private KeyValueStore<String, String> restrictedWordsStore;

    /** Компилированный паттерн для проверки запрещённых слов. */
    private Pattern forbiddenWordsPattern;

    /**
     * Конструктор процессора.
     * 
     * @param blockedUsersStoreName    Имя хранилища заблокированных пользователей.
     * @param restrictedWordsStoreName Имя хранилища запрещённых слов.
     */
    public BlockAndCensorProcessor(String blockedUsersStoreName, String restrictedWordsStoreName) {
        this.blockedUsersStoreName = blockedUsersStoreName;
        this.restrictedWordsStoreName = restrictedWordsStoreName;
    }

    /**
     * Инициализация процессора.
     * 
     * @param context Контекст процессора, предоставляющий доступ к State Store.
     */
    @Override
    public void init(ProcessorContext<String, MessageModel> context) {
        super.init(context);
        this.blockedUsersStore = context.getStateStore(blockedUsersStoreName);
        this.restrictedWordsStore = context.getStateStore(restrictedWordsStoreName);
        updateForbiddenWordsPattern();

        // Планируем периодическое обновление паттерна запрещённых слов
        context.schedule(
            java.time.Duration.ofSeconds(1),
            PunctuationType.WALL_CLOCK_TIME,
            timestamp -> updateForbiddenWordsPattern()
        );
    }

    /**
     * Основной метод обработки сообщений.
     * 
     * @param record Сообщение, полученное для обработки.
     */
    @Override
    public void process(Record<String, MessageModel> record) {
        logger.info("Обрабатывается сообщение: {}", record.value());
        MessageModel message = record.value();

        if (message == null || message.getSenderId() == null || message.getContent() == null) {
            logger.warn("Некорректное сообщение. Пропускаем.");
            return;
        }

        String senderId = message.getSenderId();
        String recipientId = message.getRecipientId();
        String content = message.getContent();

        // Проверяем, заблокирован ли отправитель у получателя
        List<String> blockedUsers = blockedUsersStore.get(recipientId);


        if (blockedUsers != null && blockedUsers.contains(senderId)) {
            logger.info("Получатель {} заблокировл отправителя {}. Блокируем сообщение: '{}'.", recipientId,senderId, content);
            return;
        }

        // Цензура содержания сообщения
        if (forbiddenWordsPattern != null) {
            String censoredContent = forbiddenWordsPattern.matcher(content).replaceAll("****");
            if (!censoredContent.equals(content)) {
                logger.info("Сообщение отцензурировано: '{}' -> '{}'", content, censoredContent);
                message.setContent(censoredContent);
            } else {
                logger.info("Сообщение: '{}'' не содержит запрещённых слов.",content);
            }
        }

        // Отправляем сообщение дальше в топик
        context().forward(record.withValue(message));
    }

    /**
     * Метод закрытия процессора.
     * Вызывается при завершении работы.
     */
    @Override
    public void close() {
        // Закрытие ресурсов, если требуется
    }

    /**
     * Обновляет паттерн запрещённых слов на основе данных из State Store.
     */
    private void updateForbiddenWordsPattern() {
        try {
            Set<String> currentForbiddenWords = new HashSet<>();
            try (KeyValueIterator<String, String> iterator = restrictedWordsStore.all()) {
                while (iterator.hasNext()) {
                    KeyValue<String, String> entry = iterator.next();
                    String key = entry.key.toLowerCase();
                    String value = entry.value;
    
                    // Пропускаем слова, если значение null или равно "DELETE"
                    if (value != null && !value.equalsIgnoreCase("DELETE")) {
                        currentForbiddenWords.add(key);
                    } else {
                        logger.info("Запрещенное слово '{}' исключёно из фильтрации из-за признака удаления (томбстоуна): {}", key, value);
                    }
                }
            }

            if (!currentForbiddenWords.equals(previousForbiddenWords)) {
                // Определяем добавленные и удалённые слова
                Set<String> addedWords = new HashSet<>(currentForbiddenWords);
                addedWords.removeAll(previousForbiddenWords);

                Set<String> removedWords = new HashSet<>(previousForbiddenWords);
                removedWords.removeAll(currentForbiddenWords);

                // Логируем изменения
                if (!addedWords.isEmpty()) {
                    logger.info("В локальный кеш приложения были добавлены запрещённые слова: {}", addedWords);
                }
                if (!removedWords.isEmpty()) {
                    logger.info("В локальном кеш приложения были удалены запрещённые слова: {}", removedWords);
                }

                // Обновляем паттерн
                previousForbiddenWords = currentForbiddenWords;
                if (!currentForbiddenWords.isEmpty()) {
                    String regex = "\\b(" + String.join("|", currentForbiddenWords) + ")\\b";
                    forbiddenWordsPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                    logger.info("Обновлён паттерн запрещённых слов: {}", currentForbiddenWords);
                } else {
                    forbiddenWordsPattern = null;
                    logger.info("ЛОкальный кеш приложения запрещённых слов пуст.");
                }
            }
        } catch (Exception e) {
            logger.error("Ошибка при обновлении паттерна запрещённых слов.", e);
        }
    }
}
