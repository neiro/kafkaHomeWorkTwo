package org.example.serde;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.util.List;

/**
 * Реализация интерфейса Serde для работы со списками строк (List<String>) в Kafka.
 * 
 * Данный класс предоставляет методы сериализации и десериализации списков строк
 * с использованием библиотеки Jackson для преобразования в JSON и обратно.
 */
public class ListSerde implements Serde<List<String>> {

    /**
     * Экземпляр ObjectMapper для работы с JSON.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Метод для получения сериализатора.
     * 
     * @return Сериализатор для списков строк (List<String>).
     */
    @Override
    public Serializer<List<String>> serializer() {
        return (topic, data) -> {
            try {
                // Проверка на null для предотвращения ошибок
                if (data == null) {
                    return null;
                }
                // Преобразование списка в JSON-байты
                return objectMapper.writeValueAsBytes(data);
            } catch (Exception e) {
                // Генерация исключения в случае ошибки сериализации
                throw new RuntimeException("Ошибка сериализации списка: " + e.getMessage(), e);
            }
        };
    }

    /**
     * Метод для получения десериализатора.
     * 
     * @return Десериализатор для списков строк (List<String>).
     */
    @Override
    public Deserializer<List<String>> deserializer() {
        return (topic, data) -> {
            try {
                // Проверка на null или пустые данные
                if (data == null || data.length == 0) {
                    return null;
                }
                // Преобразование JSON-байтов обратно в список строк
                return objectMapper.readValue(data, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                // Генерация исключения в случае ошибки десериализации
                throw new RuntimeException("Ошибка десериализации списка: " + e.getMessage(), e);
            }
        };
    }
}
