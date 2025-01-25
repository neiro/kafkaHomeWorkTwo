package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.service.RestrictedWordsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * Контроллер для управления списком запрещённых слов.
 * Предоставляет методы для получения, добавления и удаления запрещённых слов.
 */
@RestController
@RequestMapping("/api/restricted-words")
@Tag(name = "Restricted Words", description = "Управление списком запрещённых слов")
public class RestrictedWordsController {

    /**
     * Сервис для работы с запрещёнными словами.
     */
    private final RestrictedWordsService restrictedWordsService;

    /**
     * Конструктор контроллера.
     * Инициализирует зависимость сервиса для управления списком запрещённых слов.
     *
     * @param restrictedWordsService Сервис для работы с запрещёнными словами.
     */
    @Autowired
    public RestrictedWordsController(RestrictedWordsService restrictedWordsService) {
        this.restrictedWordsService = restrictedWordsService;
    }

    /**
     * Получение списка запрещённых слов.
     *
     * @return Список всех запрещённых слов или сообщение об ошибке, если сервер недоступен.
     */
    @Operation(
        summary = "Получение списка запрещённых слов",
        description = "Возвращает список всех запрещённых слов, хранящихся в системе.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Список запрещённых слов", 
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = Set.class))),
            @ApiResponse(responseCode = "503", description = "Сервер недоступен", 
                         content = @Content(mediaType = "text/plain"))
        }
    )
    @GetMapping("/list")
    public ResponseEntity<?> getRestrictedWordsList() {
        try {
            // Получение списка запрещённых слов через сервис
            Set<String> words = restrictedWordsService.getRestrictedWords();
            return ResponseEntity.ok(words);
        } catch (IllegalStateException e) {
            // Обработка случая, когда сервер недоступен
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                 .body("Сервер временно недоступен. Попробуйте позже.");
        }
    }

    /**
     * Добавление нового запрещённого слова в список.
     *
     * @param key Слово, которое нужно добавить в список запрещённых.
     * @return Подтверждение успешного добавления или сообщение об ошибке.
     */
    @Operation(
        summary = "Добавление запрещённого слова",
        description = "Добавляет новое слово в список запрещённых.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Слово добавлено успешно", 
                         content = @Content(mediaType = "text/plain")),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации параметра", 
                         content = @Content(mediaType = "text/plain"))
        }
    )
    @PostMapping
    public ResponseEntity<String> addWord(@RequestParam String key) {
        if (key == null || key.isBlank()) {
            // Проверка валидности параметра key
            return ResponseEntity.badRequest().body("Параметр 'key' обязателен и не может быть пустым.");
        }

        // Нормализация ключа перед добавлением в список
        String normalizedKey = normalizeKey(key);
        restrictedWordsService.addWord(normalizedKey);
        return ResponseEntity.ok("Запрещённое слово добавлено: " + normalizedKey);
    }

    /**
     * Удаление слова из списка запрещённых.
     *
     * @param key Слово, которое нужно удалить из списка запрещённых.
     * @return Подтверждение успешного удаления или сообщение об ошибке.
     */
    @Operation(
        summary = "Удаление запрещённого слова",
        description = "Удаляет слово из списка запрещённых.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Слово удалено успешно", 
                         content = @Content(mediaType = "text/plain")),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации параметра", 
                         content = @Content(mediaType = "text/plain"))
        }
    )
    @DeleteMapping("/{key}")
    public ResponseEntity<String> deleteWord(@PathVariable String key) {
        if (key == null || key.isBlank()) {
            // Проверка валидности параметра key
            return ResponseEntity.badRequest().body("Параметр 'key' обязателен для удаления.");
        }

        // Нормализация ключа перед удалением из списка
        String normalizedKey = normalizeKey(key);
        restrictedWordsService.deleteWord(normalizedKey);
        return ResponseEntity.ok("Запрещённое слово удалено: " + normalizedKey);
    }

    /**
     * Нормализует ключ (слово) для приведения к нижнему регистру.
     *
     * @param key Исходный ключ.
     * @return Нормализованный ключ.
     */
    private String normalizeKey(String key) {
        // Удаление лишних пробелов и преобразование строки к нижнему регистру
        return key.trim().toLowerCase();
    }
}
