package org.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конфигурационный класс для работы с пользовательскими свойствами Kafka.
 *
 * Этот класс связывает свойства из файла конфигурации с Java-объектом.
 * Свойства должны быть описаны в файле конфигурации с префиксом "kafka".
 */
@Component
@ConfigurationProperties(prefix = "kafka")
public class KafkaProperties {

    /** Список запрещённых слов, задаётся в конфигурации. */
    private List<String> restrictedWords;

    /** Список пользователей, которые находятся в блокировке, задаётся в конфигурации. */
    private List<String> blockedUsers;

    /**
     * Возвращает список запрещённых слов.
     *
     * @return Список запрещённых слов.
     */
    public List<String> getRestrictedWords() {
        return restrictedWords;
    }

    /**
     * Устанавливает список запрещённых слов.
     *
     * @param restrictedWords Список запрещённых слов.
     */
    public void setRestrictedWords(List<String> restrictedWords) {
        this.restrictedWords = restrictedWords;
    }

    /**
     * Возвращает список заблокированных пользователей.
     *
     * @return Список заблокированных пользователей.
     */
    public List<String> getBlockedUsers() {
        return blockedUsers;
    }

    /**
     * Устанавливает список заблокированных пользователей.
     *
     * @param blockedUsers Список заблокированных пользователей.
     */
    public void setBlockedUsers(List<String> blockedUsers) {
        this.blockedUsers = blockedUsers;
    }
}
