package kafka.consumer;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kafka.common.Message;

/**
 * Класс ConsumerPull отвечает за получение сообщений из Kafka топика с использованием pull-модели.
 * Реализует ручной коммит смещений и обработку ретраев при обработке сообщений.
 */
public class ConsumerPull {

    // Адреса брокеров Kafka, к которым будет подключаться Consumer
    private static final String BOOTSTRAP_SERVERS = "127.0.0.1:9094,127.0.0.1:9095,127.0.0.1:9096";

    // Уникальный идентификатор группы консьюмеров для совместного чтения топика
    private static final String GROUP_ID = "consumer-pull-group";

    // Указывает, с какого места начинать чтение, если не существует сохранённого смещения
    private static final String OFFSET_RESET = "earliest";

    // Отключение автоматического коммита смещений
    private static final boolean AUTO_COMMIT = false;

    // Минимальный объём данных (в байтах), который должен быть доступен для чтения за один запрос
    private static final int FETCH_MIN_BYTES = 1 * 1024 * 1024; // 1 MB

    // Максимальное время ожидания новых данных до выполнения poll()
    private static final int FETCH_MAX_WAIT_MS = 5000; // 5 секунд

    // Таймаут для метода poll, который определяет, как долго ждать новых данных
    private static final Duration POLL_TIMEOUT = Duration.ofMillis(5000);

    // Максимальное количество попыток повторной обработки сообщения при ошибке
    private static final int MAX_RETRIES = 3;

    // Задержка между повторными попытками обработки сообщения (в миллисекундах)
    private static final int RETRY_DELAY_MS = 1000;

    private static final Logger logger = LoggerFactory.getLogger(ConsumerPull.class);

    public static void main(String[] args) {
        // Настройка свойств Kafka Consumer
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OFFSET_RESET);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, String.valueOf(AUTO_COMMIT));
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, FETCH_MIN_BYTES);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, FETCH_MAX_WAIT_MS);

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        // Подписка на Kafka топик
        consumer.subscribe(Collections.singletonList("my-topic"));

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
                            // для этого необходимо заслать сообщение с Value = "test_retries"
                            if (rec.value().contains("test_retries")) {
                                throw new RuntimeException("Симуляция ошибки обработки");
                            }

                            // Логирование обработанного сообщения
                            logger.info("Pull Consumer получил сообщение: key = {}, value = {}, partition = {}, offset = {}",
                                    rec.key(), message.getValue(), rec.partition(), rec.offset());

                            

                            success = true; // Если обработка успешна, завершаем цикл ретраев
                        } catch (Exception e) {
                            attempt++;
                            logger.error("Ошибка обработки сообщения (попытка {}/{}): {}", attempt, MAX_RETRIES, e.getMessage());

                            if (attempt >= MAX_RETRIES) {
                                // Если количество попыток превышает лимит, сообщение может быть отправлено в DLQ
                                logger.error("Не удалось обработать сообщение после {} попыток: key = {}, value = {}",
                                        MAX_RETRIES, rec.key(), rec.value());
                              
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

                // Ручной коммит смещений после успешной обработки сообщений
                if (!records.isEmpty()) {
                    try {
                        consumer.commitSync();
                        logger.info("Смещения успешно зафиксированы.");
                    } catch (Exception e) {
                        logger.error("Ошибка коммита смещений: " + e.getMessage());
                    }
                }
            }
        } finally {
            // Закрытие консьюмера для освобождения ресурсов
            consumer.close();
        }
    }
}
