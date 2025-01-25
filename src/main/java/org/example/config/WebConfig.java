package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Конфигурационный класс для настройки CORS (Cross-Origin Resource Sharing).
 * 
 * CORS позволяет управлять доступом к ресурсам сервера из других доменов.
 * Этот класс определяет правила для обработки CORS-запросов.
 */
@Configuration
public class WebConfig {

    /**
     * Создаёт бин {@link WebMvcConfigurer} для настройки CORS.
     * 
     * @return Экземпляр {@link WebMvcConfigurer} с определёнными правилами CORS.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {

            /**
             * Настройка правил для обработки CORS-запросов.
             * 
             * @param registry Реестр для добавления правил CORS.
             */
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Применяется ко всем путям на сервере
                    .allowedOrigins("http://localhost:8081") // Разрешённый источник запросов (локальный для тестирования)
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Разрешённые HTTP-методы
                    .allowedHeaders("*") // Разрешённые заголовки запросов
                    .allowCredentials(true); // Разрешение на передачу учетных данных (например, куки или авторизационные заголовки)
            }
        };
    }
}