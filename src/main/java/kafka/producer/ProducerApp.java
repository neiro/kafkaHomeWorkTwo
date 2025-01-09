package kafka.producer;

import com.github.javafaker.Faker;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kafka.common.Message;

/**
 * Класс ProducerApp отвечает за автоматическую отправку сообщений в Kafka топик.
 * Реализует генерацию сообщений с настраиваемой скоростью.
 */
public class ProducerApp {

    // Адреса брокеров Kafka, к которым будет подключаться Producer
    private static final String BOOTSTRAP_SERVERS = "127.0.0.1:9094,127.0.0.1:9095,127.0.0.1:9096";

    // Гарантия доставки сообщений At Least Once
    private static final String ACKS = "all";

    // Максимальное количество ретраев при сбоях
    private static final int RETRIES = 3;

    // Включение идемпотентности для предотвращения дублирования сообщений
    private static final boolean IDEMPOTENCE = true;

    // Задержка между повторными попытками отправки сообщений (в миллисекундах)
    private static final int RETRY_BACKOFF_MS = 1000;

    // Таймаут доставки сообщения (в миллисекундах)
    private static final int DELIVERY_TIMEOUT_MS = 120000;

    // Количество сообщений, отправляемых в секунду
    private static final int MESSAGES_PER_SECOND = 3;

    private static final Logger logger = LoggerFactory.getLogger(ProducerApp.class);

    public static void main(String[] args) {
        // Настройка свойств Kafka Producer
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, ACKS);
        props.put(ProducerConfig.RETRIES_CONFIG, RETRIES);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, String.valueOf(IDEMPOTENCE));
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, RETRY_BACKOFF_MS);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, DELIVERY_TIMEOUT_MS);

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        String topic = "my-topic";
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        AtomicInteger messageCounter = new AtomicInteger(1);

        Faker faker = new Faker();

        Runnable messageTask = () -> {
            String key = "key-" + messageCounter.getAndIncrement();

            // Генерация фейковых данных
            String name = faker.name().fullName(); // Имя
            String address = faker.address().fullAddress(); // Адрес
            String company = faker.company().name(); // Название компании
            String phoneNumber = faker.phoneNumber().phoneNumber(); // Телефон
            String value = String.format("Name: %s, Address: %s, Company: %s, Phone: %s", name, address, company, phoneNumber);

            Message message = new Message(key, value);
            String serializedMessage;
            try {
                serializedMessage = message.serialize();
            } catch (JsonProcessingException e) {
                logger.error("Ошибка сериализации сообщения: {}", e.getMessage());
                return;
            }

            ProducerRecord<String, String> rec = new ProducerRecord<>(topic, key, serializedMessage);
            producer.send(rec, (metadata, exception) -> {
                if (exception != null) {
                    logger.error("Ошибка при отправке сообщения: {}", exception.getMessage());
                } else {
                    logger.info("Отправлено сообщение: key = {}, value = {}, partition = {}, offset = {}",
                        rec.key(), rec.value(), metadata.partition(), metadata.offset());
                }
            });
        };

        // Запуск генерации сообщений с заданной скоростью
        scheduler.scheduleAtFixedRate(messageTask, 0, 1000 / MESSAGES_PER_SECOND, TimeUnit.MILLISECONDS);

        // Добавляем обработчик завершения для корректного закрытия продюсера
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Завершение работы...");
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.error("Задачи завершения не успели выполниться.");
                }
            } catch (InterruptedException e) {
                // Повторно прерываем текущий поток
                Thread.currentThread().interrupt();
                logger.error("Ошибка при завершении: {}", e.getMessage());
            }
            producer.close();
        }));
    }
}
