package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Основной класс приложения KafkaHomeWorkTwo.
 * 
 * Этот класс запускает Spring Boot приложение и является точкой входа в программу.
 * Аннотация {@link SpringBootApplication} автоматически конфигурирует приложение, включая компоненты, сканирование пакетов и настройки.
 */
@SpringBootApplication
public class KafkaHomeWorkTwoApplication {

    /**
     * Главный метод приложения.
     * 
     * Этот метод используется для запуска Spring Boot приложения. 
     * Метод вызывает {@link SpringApplication#run(Class, String...)} для инициализации контекста Spring и запуска встроенного сервера.
     * 
     * @param args Аргументы командной строки, которые могут быть переданы в приложение при запуске.
     */
    public static void main(String[] args) {
        SpringApplication.run(KafkaHomeWorkTwoApplication.class, args);
    }
}
