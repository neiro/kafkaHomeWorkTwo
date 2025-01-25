package org.example.controller;

import org.example.model.UserBlockInfo;
import org.example.service.BlockedUsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Контроллер для управления блокировками пользователей.
 * Предоставляет операции для блокировки, разблокировки и получения списков заблокированных пользователей.
 */
@RestController
@RequestMapping("/api/blocking")
@Tag(name = "BlockController", description = "Управление блокировками пользователей")
public class BlockController {

    /**
     * Сервис для работы с заблокированными пользователями.
     */
    private final BlockedUsersService blockedUsersService;

    /**
     * Конструктор контроллера. Инициализирует зависимость сервиса блокировок пользователей.
     * 
     * @param blockedUsersService Сервис, обрабатывающий логику блокировок.
     */
    @Autowired
    public BlockController(BlockedUsersService blockedUsersService) {
        this.blockedUsersService = blockedUsersService;
    }


    /**
     * Получение списка заблокированных пользователей для конкретного пользователя.
     * 
     * @param recipientId Идентификатор пользователя, для которого запрашивается список.
     * @return Список идентификаторов заблокированных пользователей или статус "204 No Content", если список пуст.
     */
    @GetMapping("/list")
    @Operation(summary = "Получение списка заблокированных пользователей")
    public ResponseEntity<List<String>> getBlockedUsers(@RequestParam @NotBlank String recipientId) {
        // Получение списка заблокированных пользователей через сервис
        List<String> blockedUsers = blockedUsersService.getBlockedUsers(recipientId);
        // Если список пуст, вернуть статус 204 No Content
        if (blockedUsers.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        // Возврат списка заблокированных пользователей
        return ResponseEntity.ok(blockedUsers);
    }

    /**
     * Получение всех блокировок в системе.
     * 
     * @return Список всех блокировок в системе (объекты UserBlockInfo) или статус "204 No Content", если список пуст.
     */
    @GetMapping("/all")
    @Operation(summary = "Получение всех блокировок")
    public ResponseEntity<List<UserBlockInfo>> getAllBlockedUsers() {
        // Получение списка всех блокировок через сервис
        List<UserBlockInfo> allBlockedUsers = blockedUsersService.getAllBlockedUsers();
        // Если список пуст, вернуть статус 204 No Content
        if (allBlockedUsers.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        // Возврат списка всех блокировок
        return ResponseEntity.ok(allBlockedUsers);
    }

    /**
     * Обработчик исключений, возникающих в контроллере.
     * 
     * @param e Исключение, возникшее в процессе выполнения запроса.
     * @return Ответ с сообщением об ошибке и статусом 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        // Возврат сообщения об ошибке с соответствующим статусом
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Произошла ошибка: " + e.getMessage());
    }
}