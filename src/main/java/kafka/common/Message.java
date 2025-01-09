package kafka.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Класс Message представляет собой структуру сообщения, отправляемого в Kafka.
 * Реализует методы сериализации и десериализации в JSON формат.
 */
public class Message {

    // Поле ключа сообщения
    private String key;

    // Поле значения сообщения
    private String value;

    // Конструктор по умолчанию для сериализации/десериализации
    public Message() {}

    // Конструктор для создания объекта Message с заданными ключом и значением
    public Message(String key, String value) {
        this.key = key;
        this.value = value;
    }

    // Геттеры и сеттеры для полей key и value

    /**
     * Получить ключ сообщения.
     * @return ключ сообщения.
     */
    public String getKey() {
        return key;
    }

    /**
     * Установить ключ сообщения.
     * @param key ключ сообщения.
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Получить значение сообщения.
     * @return значение сообщения.
     */
    public String getValue() {
        return value;
    }

    /**
     * Установить значение сообщения.
     * @param value значение сообщения.
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Сериализует объект Message в JSON строку.
     * @return JSON представление объекта Message.
     * @throws JsonProcessingException если сериализация не удалась.
     */
    public String serialize() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    /**
     * Десериализует JSON строку в объект Message.
     * @param json JSON строка.
     * @return Объект Message.
     * @throws JsonProcessingException если десериализация не удалась.
     */
    public static Message deserialize(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Message.class);
    }

    /**
     * Переопределённый метод toString для удобного отображения объекта Message в строковом формате.
     * @return строковое представление объекта Message.
     * Пока не использую, но может понадобиться если начнет разрастаться код
     */
    @Override
    public String toString() {
        return "Message {" +
               "key='" + key + '\'' +
               ", value='" + value + '\'' +
               '}';
    }

}
