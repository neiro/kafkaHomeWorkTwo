package org.example.topology;

import org.example.serde.*;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.common.serialization.Serde;
import org.springframework.kafka.support.serializer.JsonSerde;
import java.util.Collections;
import java.util.List;

/**
 * Фабрика для создания хранилищ (State Stores) в Kafka Streams.
 * 
 * Содержит методы для создания KeyValueStore, используемых для хранения данных о заблокированных пользователях и запрещённых словах.
 */
public class StoresFactory {

    /**
     * Создаёт StoreBuilder для хранилища заблокированных пользователей.
     * 
     * Хранилище предназначено для хранения информации о заблокированных пользователях.
     * Ключ: String (recipientId - ID пользователя, который блокирует).
     * Значение: List<String> (список blockedId - ID пользователей, которых заблокировали).
     * 
     * @param storeName Имя хранилища.
     * @return StoreBuilder для KeyValueStore.
     */
    public static StoreBuilder<KeyValueStore<String, List<String>>> blockedUsersStoreBuilder(String storeName) {
        // Настройка JSON Serde для сериализации/десериализации списка заблокированных пользователей
        JsonSerde<List<String>> blockedUsersSerde = new JsonSerde<>();
        blockedUsersSerde.configure(
            Collections.singletonMap("spring.json.value.default.type", "java.util.List"), 
            false
        );

        // Создание и возврат StoreBuilder для KeyValueStore
        return Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(storeName), // Определяем хранилище как персистентное
            Serdes.String(), // Ключ: String
            blockedUsersSerde // Значение: JSON Serde для списка пользователей
        );
    }

    /**
     * Создаёт StoreBuilder для хранилища запрещённых слов.
     * 
     * Хранилище предназначено для хранения запрещённых слов.
     * Ключ: String (запрещённое слово).
     * Значение: String (запрещённое слово - хранится в виде строки для простоты обработки).
     * 
     * @param storeName Имя хранилища.
     * @return StoreBuilder для KeyValueStore.
     */
    public static StoreBuilder<KeyValueStore<String, String>> restrictedWordsStoreBuilder(String storeName) {
        // Создание и возврат StoreBuilder для KeyValueStore
        return Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(storeName), // Определяем хранилище как персистентное
            Serdes.String(), // Ключ: String
            Serdes.String()  // Значение: String
        );
    }
}