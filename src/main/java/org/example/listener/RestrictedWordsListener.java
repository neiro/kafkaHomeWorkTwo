package org.example.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.kafka.support.KafkaHeaders;
import org.example.service.RestrictedWordsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RestrictedWordsListener {

    private static final Logger log = LoggerFactory.getLogger(RestrictedWordsListener.class);
    private final RestrictedWordsService restrictedWordsService;

    public RestrictedWordsListener(RestrictedWordsService restrictedWordsService) {
        this.restrictedWordsService = restrictedWordsService;
    }

    @KafkaListener(topics = "${kafka.topic.restrictedWords}")
    public void listenRestrictedWordsTopic(
            @Header(KafkaHeaders.RECEIVED_KEY) String word,
            @Payload(required = false) String event
    ) {
        // Обработка null или пустого значения для word
        if (word == null || word.isEmpty()) {
            log.warn("Получено сообщение без ключа (word). Отклонено.");
            return;
        }

        // Если event == null, устанавливаем значение "DELETE"
        String effectiveEvent = (event == null || event.isEmpty()) ? "DELETE" : event;

        log.debug("Обработка сообщения. Слово: {}, событие: {}", word, effectiveEvent);

        // Передача данных в сервис
        restrictedWordsService.handleKafkaEvent(word, effectiveEvent);
    }
}
