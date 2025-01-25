package org.example.topology;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Процессор для управления списками заблокированных пользователей.
 * 
 * Основные функции:
 * - Обработка входящих записей для добавления или удаления пользователей из списка заблокированных.
 * - Сохранение обновлений в State Store.
 */
public class BlockedUsersProcessor implements Processor<String, List<String>, Void, Void> {

    /** Логгер для записи действий процессора. */
    private static final Logger logger = LoggerFactory.getLogger(BlockedUsersProcessor.class);

    /** Имя State Store для хранения списков заблокированных пользователей. */
    private final String stateStoreName;

    /** State Store для хранения списков заблокированных пользователей. */
    private KeyValueStore<String, List<String>> stateStore;

    /**
     * Конструктор процессора.
     * 
     * @param stateStoreName Имя State Store для хранения заблокированных пользователей.
     */
    public BlockedUsersProcessor(String stateStoreName) {
        this.stateStoreName = stateStoreName;
    }

    /**
     * Инициализация процессора.
     * 
     * @param context Контекст процессора, предоставляющий доступ к State Store.
     */
    @Override
    public void init(ProcessorContext<Void, Void> context) {
        this.stateStore = context.getStateStore(stateStoreName);
    }

    /**
     * Обработка входящих записей.
     * 
     * @param record Запись, содержащая ключ и список изменений (добавление или удаление пользователей).
     */
    @Override
    public void process(Record<String, List<String>> record) {
        if (record.key() == null) {
            logger.warn("Получена запись с null ключом. Пропускаем.");
            return;
        }

        String key = record.key();
        List<String> incomingUsers = record.value();

        if (incomingUsers == null || incomingUsers.isEmpty()) {
            logger.warn("Получен пустой или null список для ключа '{}'. Пропускаем запись.", key);
            return;
        }

        // Получаем текущий список заблокированных пользователей
        List<String> blockedUsersList = getBlockedUsersFromStore(key);

        // Флаг для проверки, были ли изменения
        boolean isUpdated = false;

        for (String user : incomingUsers) {
            if (user.startsWith("DELETE:")) {
                // Удаление пользователя из списка
                String userToRemove = user.replaceFirst("DELETE:", "").trim();
                if (blockedUsersList.remove(userToRemove)) {
                    logger.info("Пользователь '{}' удалён из списка заблокированных для ключа '{}'.", userToRemove, key);
                    isUpdated = true;
                } else {
                    logger.info("Пользователь '{}' не найден в списке для ключа '{}'.", userToRemove, key);
                }
            } else {
                // Добавление пользователя в список
                if (!blockedUsersList.contains(user)) {
                    blockedUsersList.add(user);
                    logger.info("Пользователь '{}' добавлен в список заблокированных для ключа '{}'.", user, key);
                    isUpdated = true;
                } else {
                    logger.info("Пользователь '{}' уже существует в списке для ключа '{}'.", user, key);
                }
            }
        }

        // Сохраняем изменения только если были обновления
        if (isUpdated) {
            saveBlockedUsersToStore(key, blockedUsersList);
        }
    }

    /**
     * Закрытие процессора.
     * Вызывается при завершении работы.
     */
    @Override
    public void close() {
        // Освобождение ресурсов, если требуется
    }

    /**
     * Получение текущего списка заблокированных пользователей из State Store.
     * 
     * @param key Ключ для идентификации списка пользователей.
     * @return Список заблокированных пользователей для указанного ключа.
     */
    private List<String> getBlockedUsersFromStore(String key) {
        List<String> blockedUsers = stateStore.get(key);
        if (blockedUsers == null) {
            return new ArrayList<>();
        }
        return blockedUsers;
    }

    /**
     * Сохранение обновлённого списка заблокированных пользователей в State Store.
     * 
     * @param key              Ключ для идентификации списка пользователей.
     * @param blockedUsersList Обновлённый список заблокированных пользователей.
     */
    private void saveBlockedUsersToStore(String key, List<String> blockedUsersList) {
        stateStore.put(key, blockedUsersList);
        logger.debug("Обновлён список заблокированных пользователей для ключа '{}': {}", key, blockedUsersList);
    }
}