package org.example.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.config.KafkaProducerConfig;
import org.example.model.MessageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired; 
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для работы с веб-интерфейсом и отправкой сообщений в Kafka.
 * Предоставляет методы для отображения главной страницы и отправки сообщений.
 */
@Controller
@RequestMapping("/api/web")
public class WebController {

    /**
     * Логгер для записи информации о действиях в контроллере.
     */
    private static final Logger log = LoggerFactory.getLogger(WebController.class);

    /**
     * Название топика Kafka для отправки сообщений.
     */
    private static final String MESSAGES_TOPIC = KafkaProducerConfig.MESSAGES_TOPIC;

    /**
     * Шаблон Kafka для отправки сообщений.
     */
    @Autowired
    private KafkaTemplate<String, MessageModel> kafkaTemplate;

    /**
     * Отображение главной страницы веб-приложения.
     * 
     * @return Имя шаблона главной страницы (index.html).
     */
    @GetMapping("/")
    public String indexPage() {
        // Возвращает шаблон главной страницы
        return "index";  // Рендерит index.html из resources/templates
    }

    /**
     * Обработка отправки сообщения в Kafka.
     * 
     * @param senderId    Идентификатор отправителя (обязательное поле).
     * @param recipientId Идентификатор получателя (обязательное поле).
     * @param content     Содержимое сообщения (обязательное поле, не более 500 символов).
     * @return Редирект на главную страницу после успешной отправки.
     */
    @PostMapping("/send")
    public String sendMessage(
            @RequestParam @NotBlank(message = "ID отправителя обязателен.") String senderId,
            @RequestParam @NotBlank(message = "ID получателя обязателен.") String recipientId,
            @RequestParam @NotBlank(message = "Содержимое сообщения не может быть пустым.") 
            @Size(max = 500, message = "Содержимое сообщения не может превышать 500 символов.") String content) {

        // Создание объекта сообщения
        MessageModel msg = new MessageModel();
        msg.setSenderId(senderId);
        msg.setRecipientId(recipientId);
        msg.setContent(content);

        // Отправка сообщения в Kafka, где ключом выступает senderId
        kafkaTemplate.send(MESSAGES_TOPIC, senderId, msg);

        // Логирование информации об отправке сообщения
        log.info("Сообщение отправлено в топик '{}' => {}", MESSAGES_TOPIC, msg);

        // Перенаправление пользователя на главную страницу
        return "redirect:/";
    }
}
