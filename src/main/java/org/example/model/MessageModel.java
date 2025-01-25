package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Модель данных для представления сообщения.
 * Содержит информацию об отправителе, получателе и содержимом сообщения.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageModel {

    /**
     * Идентификатор отправителя сообщения.
     * Используется для указания, от кого отправлено сообщение.
     */
    private String senderId;

    /**
     * Идентификатор получателя сообщения.
     * Используется для указания, кому адресовано сообщение.
     */
    private String recipientId;

    /**
     * Содержимое сообщения.
     * Хранит текст или другую информацию, передаваемую от отправителя к получателю.
     */
    private String content;
}
