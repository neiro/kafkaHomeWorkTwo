package org.example.controller;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

/**
 * Контроллер для обработки ошибок приложения.
 * Отвечает за перехват ошибок и отображение страницы с подробностями ошибки.
 */
@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    /**
     * Сервис для извлечения атрибутов ошибки.
     */
    private final ErrorAttributes errorAttributes;

    /**
     * Конструктор контроллера ошибок.
     * Инициализирует зависимость для получения информации об ошибках.
     *
     * @param errorAttributes Объект для извлечения атрибутов ошибки.
     */
    public ErrorController(@NotNull ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    /**
     * Метод для обработки ошибок.
     *
     * @param webRequest Запрос, в котором произошла ошибка.
     * @param model      Модель для передачи данных об ошибке в представление.
     * @return Имя представления для отображения страницы ошибки.
     */
    @RequestMapping("/error")
    public String handleError(WebRequest webRequest, Model model) {
        // Извлечение атрибутов ошибки с включением сообщения об ошибке
        Map<String, Object> errorDetails = errorAttributes.getErrorAttributes(
            webRequest, ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE)
        );

        // Добавление статуса HTTP-ответа в модель. По умолчанию используется статус 500.
        model.addAttribute("status", errorDetails.getOrDefault("status", 500));

        // Добавление текстового описания ошибки в модель. Например, "Internal Server Error".
        model.addAttribute("error", errorDetails.getOrDefault("error", "Неизвестная ошибка"));

        // Добавление сообщения об ошибке в модель. Если сообщение отсутствует, указывается общее сообщение.
        model.addAttribute("message", errorDetails.getOrDefault("message", "Произошла ошибка"));

        // Добавление пути, на котором произошла ошибка. Если путь неизвестен, указывается значение по умолчанию.
        model.addAttribute("path", errorDetails.getOrDefault("path", "Неизвестный путь"));

        // Возврат имени представления для отображения страницы ошибки.
        return "error"; // Шаблон error.html в директории templates.
    }
}
