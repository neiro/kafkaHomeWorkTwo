package kafka.consumer;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import kafka.common.Message;

/**
 * Класс ConsumerPush эмулирует push-модель для получения сообщений из Kafka топика.
 * Использует автоматический коммит смещений и обработку сообщений в отдельном потоке.
 * Реализует ретраи при обработке сообщений.
 */
public class ConsumerPush {

    // Адреса брокеров Kafka, к которым будет подключаться Consumer
    private static final String BOOTSTRAP_SERVERS = "127.0.0.1:9094,127.0.0.1:9095,127.0.0.1:9096";

    // Уникальный идентификатор группы консьюмеров для совместного чтения топика
    private static final String GROUP_ID = "consumer-push-group";

    // Указывает, с какого места начинать чтение, если не существует сохранённого смещения
    private static final String OFFSET_RESET = "earliest";

    // Включение автоматического коммита смещений
    private static final boolean AUTO_COMMIT = true;

    // Таймаут для метода poll, который определяет, как долго ждать новых данных
    private static final Duration POLL_TIMEOUT = Duration.ofMillis(100);

    // Максимальное количество попыток повторной обработки сообщения при ошибке
    private static final int MAX_RETRIES = 3;

    // Задержка между повторными попытками обработки сообщения (в миллисекундах)
    private static final int RETRY_DELAY_MS = 1000;

    private static final Logger logger = LoggerFactory.getLogger(ConsumerPush.class);

    public static void main(String[] args) {
        // Настройка свойств Kafka Consumer
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OFFSET_RESET);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, String.valueOf(AUTO_COMMIT));

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("my-topic"));

        // Создание потока для обработки сообщений
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> {
            try {
                while (true) {
                    // Извлечение сообщений из топика с использованием pull-модели
                    ConsumerRecords<String, String> records = consumer.poll(POLL_TIMEOUT);

                    // Итерация по всем полученным сообщениям
                    for (ConsumerRecord<String, String> rec : records) {
                        int attempt = 0;
                        boolean success = false;

                        // Ретрай логика для обработки сообщений
                        while (attempt < MAX_RETRIES && !success) {
                            try {
                                // Десериализация сообщения
                                Message message = Message.deserialize(rec.value());

                                // Сымитировать ошибку для тестирования ретраев
                                if (rec.value().contains("test_retries")) {
                                    throw new RuntimeException("Симуляция ошибки обработки");
                                }

                                // Логирование обработанного сообщения
                                logger.info("Push Consumer получил сообщение: key = {}, value = {}, partition = {}, offset = {}",
                                        rec.key(), message.getValue(), rec.partition(), rec.offset());

                                success = true; // Если обработка успешна, завершаем цикл ретраев

                            } catch (Exception e) {
                                attempt++;
                                logger.error("Ошибка обработки сообщения (попытка {}/{}): {}", attempt, MAX_RETRIES, e.getMessage());

                                if (attempt >= MAX_RETRIES) {
                                    // Если количество попыток превышает лимит, сообщение ДОЛЖНО быть отправлено в DLQ иначе оно потеряется
                                    logger.error("Не удалось обработать сообщение после {} попыток: message = {}",
                                            MAX_RETRIES, rec.value());
                                 
                                } else {
                                    try {
                                        // Задержка перед следующей попыткой
                                        Thread.sleep(RETRY_DELAY_MS);
                                    } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            } finally {
                // Закрытие консьюмера для освобождения ресурсов
                consumer.close();
            }
        });
    }
}
