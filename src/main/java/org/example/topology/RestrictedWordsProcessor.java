package org.example.topology;

import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Процессор для управления списком запрещённых слов.
 * 
 * Основные задачи:
 * - Добавление новых запрещённых слов в хранилище.
 * - Удаление запрещённых слов из хранилища.
 * - Логирование текущего состояния хранилища.
 */
public class RestrictedWordsProcessor implements Processor<String, String, Void, Void> {

    /** Логгер для записи действий процессора. */
    private static final Logger logger = LoggerFactory.getLogger(RestrictedWordsProcessor.class);

    /** Имя State Store для хранения запрещённых слов. */
    private final String restrictedWordsStoreName;

    /** State Store для работы с запрещёнными словами. */
    private KeyValueStore<String, String> restrictedWordsStore;

    /**
     * Конструктор процессора.
     * 
     * @param restrictedWordsStoreName Имя хранилища запрещённых слов.
     */
    public RestrictedWordsProcessor(String restrictedWordsStoreName) {
        this.restrictedWordsStoreName = restrictedWordsStoreName;
    }

    /**
     * Инициализация процессора.
     * 
     * @param context Контекст процессора, предоставляющий доступ к State Store.
     */
    @Override
    public void init(ProcessorContext<Void, Void> context) {
        this.restrictedWordsStore = context.getStateStore(restrictedWordsStoreName);
    }

    /**
     * Обработка входящих записей.
     * 
     * @param record Запись, содержащая ключ (слово) и значение (действие: добавление или удаление).
     */
    @Override
    public void process(Record<String, String> record) {
        logger.info("Текущее состояние хранилища запрещённых слов: {}", getAllRestrictedWords());

        if (record.key() == null) {
            logger.warn("Получена запись с null ключом. Пропускаем.");
            return;
        }

        String key = record.key().toLowerCase();
        String value = record.value();

        if (value == null) {
            // Удаление слова из хранилища
            restrictedWordsStore.delete(key);
            logger.info("В хранилище удалено запрещённое слово: {}", key);
        } else {
            // Добавление слова в хранилище
            restrictedWordsStore.put(key, "true");
            logger.info("В хранилище добавлено запрещённое слово: {}", key);
        }
    }

    /**
     * Получение всех запрещённых слов из хранилища.
     * 
     * @return Список всех запрещённых слов, хранящихся в State Store.
     */
    public List<String> getAllRestrictedWords() {
        List<String> words = new ArrayList<>();
        try (KeyValueIterator<String, String> iterator = restrictedWordsStore.all()) {
            while (iterator.hasNext()) {
                words.add(iterator.next().key);
            }
        }
        return words;
    }

    /**
     * Закрытие процессора.
     * Вызывается при завершении работы.
     */
    @Override
    public void close() {
        // Закрытие ресурсов, если требуется
    }
}