# Приложение упрощённого сервиса обмена сообщениями
## Содержание
- [Обзор](#обзор)
- [Архитектура](#архитектура)
- [Классы и Компоненты](#классы-и-компоненты)
- [Используемые Технологии](#используемые-технологии)
- [Настройка и Установка](#настройка-и-установка)
- [Запуск Приложения](#запуск-приложения)
- [API Эндпоинты](#api-эндпоинты)
- [Тестирование Приложения](#тестирование-приложения)
- [Тестовые Данные](#тестовые-данные)
- [Лицензия](#лицензия)

## Обзор

**Приложение упрощённого сервиса обмена сообщениями** — это веб-сервис на базе Spring Boot, разработанный для управления списками запрещённых слов и блокировкой пользователей. Приложение интегрируется с Kafka Streams для обработки сообщений в реальном времени, обеспечивая цензуру контента и блокировку пользователей. Предоставляются REST API для управления запрещёнными словами, а также веб-интерфейс для отправки и просмотра сообщений.

## Архитектура

Приложение следует микросервисной архитектуре с следующими ключевыми компонентами:

- **Контроллеры REST**: Обрабатывают HTTP-запросы для управления запрещёнными словами и заблокированными пользователями.
- **Сервисы**: Содержат бизнес-логику для обработки запрещённых слов и управления заблокированными пользователями.
- **Kafka Streams Topology**: Определяет логику потоковой обработки для фильтрации и цензуры сообщений на основе запрещённых слов.
- **State Stores**: Поддерживают состояние запрещённых слов и заблокированных пользователей с помощью возможностей Kafka Streams.
- **Веб-интерфейс**: Обеспечивает удобный интерфейс для отправки сообщений и просмотра обработанных результатов.
- **Интеграция с Kafka**: Обеспечивает потоковую обработку сообщений и управление состоянием через топики Kafka.

## Классы и Компоненты

### Контроллеры

- **RestrictedWordsController**
  - **Описание**: Предоставляет REST API для получения списка запрещённых слов, добавления новых и удаления существующих.
  - **Эндпоинты**:
    - `GET /api/restricted-words/list`: Получение списка запрещённых слов.
    - `POST /api/restricted-words`: Добавление нового запрещённого слова.
    - `DELETE /api/restricted-words/{key}`: Удаление существующего запрещённого слова.

- **WebController**
  - **Описание**: Управляет веб-интерфейсом для отправки сообщений и просмотра результатов.
  - **Эндпоинты**:
    - `GET /api/web/`: Отображение основной веб-страницы (`index.html`).
    - `POST /api/web/send`: Отправка сообщения в Kafka для обработки.

### Сервисы

- **RestrictedWordsService**
  - **Описание**: Обрабатывает бизнес-логику управления запрещёнными словами, включая добавление, удаление и получение запрещённых слов из Kafka.

- **BlockedUsersService**
  - **Описание**: Управляет заблокированными пользователями, взаимодействуя с state store Kafka Streams для добавления или удаления пользователей из списка блокировки.

### Модели

- **MessageModel**
  - **Описание**: Представляет структуру сообщений, отправляемых и обрабатываемых в приложении.
  - **Поля**:
    - `senderId`: ID отправителя сообщения.
    - `recipientId`: ID получателя сообщения.
    - `content`: Содержимое сообщения.

- **RestrictedWord**
  - **Описание**: Представляет запрещённое слово с его ключом и значением.
  - **Поля**:
    - `key`: Запрещённое слово.
    - `value`: Дополнительная информация или статус (например, активен/неактивен).

- **UserBlockInfo**
  - **Описание**: DTO (Data Transfer Object) для передачи информации о заблокированных пользователях.
  - **Поля**:
    - `userId`: ID пользователя.
    - `blockedUsers`: Список ID пользователей, заблокированных этим пользователем.

### Kafka Streams Topology

- **MyTopologyBuilder**
  - **Описание**: Конфигурирует topology Kafka Streams, определяя источники, процессоры и приёмники для обработки сообщений и управления запрещёнными словами.
  - **Компоненты**:
    - **Sources**: `BlockedUsersSource`, `RestrictedWordsSource`, `MessagesSource`.
    - **Processors**: `BlockedUsersProcessor`, `RestrictedWordsProcessor`, `BlockAndCensorProcessor`.
    - **Sinks**: `FilteredSink`.

### Слушатели

- **RestrictedWordsListener**
  - **Описание**: Слушает Kafka топики для событий, связанных с запрещёнными словами, и обновляет состояние приложения соответствующим образом.



## Настройка и Установка

### Предварительные Требования

- **Java Development Kit (JDK) 17+**
- **Kafka**: 
- **Maven**: 

### Шаги Установки

1. **Клонирование Репозитория**
   ```bash
   git clone https://github.com/neiro/kafkaHomeWorkTwo
   cd kafkaHomeWorkTwo


# Запуск Приложения

# Доступ к Веб-Интерфейсу и Swagger UI

### Веб-Интерфейс

Перейдите по адресу [http://localhost:8081/](http://localhost:8081/) для доступа к HTML-странице, где можно отправлять сообщения.

### Swagger UI

Для изучения и взаимодействия с API эндпоинтами перейдите по адресу:

[http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)


# Тестирование Приложения

## Ручное Тестирование


### Отправка тестовых Сообщений через веб-Интерфейс

1. Откройте основную веб-страницу по адресу [http://localhost:8081/](http://localhost:8081/).
2. Заполните поля `senderId`, `recipientId` и `content`.
3. Отправьте форму  (любую из 4х предложенных).
4. Сообщение будет обработано Kafka Streams, и содержимое будет подвергнуто цензуре, если оно содержит запрещённые слова и заблокировано, если соотвествует условиям блокировки.

### Управление запрещёнными Словами

1. Используйте HTML-форму для добавления или удаления запрещенных слов — это приоритетный вариант тестирования. Также можете использовать Swagger UI или REST-клиенты, такие как Postman, для добавления или удаления запрещённых слов.
св юю
2. В Kafka UI можно просмотреть все топики приложения.

3. **В консоли приложения можно смотреть логи всех операций приложения. Работает через команду Docker:**
   ```bash
   docker logs -f kafka-homework-two-app


# Мониторинг Kafka Топиков

Используйте Kafka UI[http://localhost:8080/](http://localhost:8080/) для мониторинга топиков:
- `messages`
- `filtered-messages`
- `blocked-users`
- `restricted-words`



## Зависимости

- **Java 17**
- **Spring Boot 3**
- **Spring Web**
- **Spring Kafka**
- **Kafka Streams**
- **Lombok**
- **Swagger/OpenAPI**
- **Jackson**
- **SLF4J (Logging)**
