package org.example.topology;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.example.model.MessageModel;

import java.util.Collections;
import java.util.List;

/**
 * Конфигурационный класс для сборки топологии Kafka Streams.
 * 
 * Топология включает в себя источники данных, процессоры, хранилища состояний (State Stores) и конечные точки (Sinks).
 * Обрабатывает сообщения, блокирует отправителей, проверяет наличие запрещённых слов и цензурирует сообщения.
 */
@Configuration
public class MyTopologyBuilder {

    /** Топик с исходными сообщениями. */
    @Value("${kafka.topic.messages}")
    private String messagesTopic;

    /** Топик для фильтрованных сообщений. */
    @Value("${kafka.topic.filteredMessages}")
    private String filteredMessagesTopic;

    /** Топик с информацией о заблокированных пользователях. */
    @Value("${kafka.topic.blockedUsers}")
    private String blockedUsersTopic;

    /** Топик с запрещёнными словами. */
    @Value("${kafka.topic.restrictedWords}")
    private String restrictedWordsTopic;

    /** Имя State Store для хранения заблокированных пользователей. */
    @Value("${kafka.stateStore.blockedUsersStore}")
    private String blockedUsersStore;

    /** Имя State Store для хранения запрещённых слов. */
    @Value("${kafka.stateStore.restrictedWordsStore}")
    private String restrictedWordsStore;

    /**
     * Сборка топологии Kafka Streams.
     * 
     * @return Сконфигурированная топология.
     */
    @Bean("myProcessorTopology")
    public Topology buildTopology() {
        Topology topology = new Topology();

        // Настройка Serde для сериализации/десериализации List<String>
        JsonSerde<List<String>> blockedUsersSerde = new JsonSerde<>();
        blockedUsersSerde.configure(
            Collections.singletonMap("spring.json.value.default.type", "java.util.List"),
            false
        );

        // Источники (Sources): добавляем топики, откуда поступают данные
        topology.addSource("BlockedUsersSource", Serdes.String().deserializer(), blockedUsersSerde.deserializer(), blockedUsersTopic);
        topology.addSource("RestrictedWordsSource", Serdes.String().deserializer(), Serdes.String().deserializer(), restrictedWordsTopic);
        topology.addSource("MessagesSource", Serdes.String().deserializer(), new JsonSerde<>(MessageModel.class).deserializer(), messagesTopic);

        // Процессоры (Processors): добавляем обработчики для источников
        topology.addProcessor("BlockedUsersProcessor", () -> new BlockedUsersProcessor(blockedUsersStore), "BlockedUsersSource");
        topology.addProcessor("RestrictedWordsProcessor", () -> new RestrictedWordsProcessor(restrictedWordsStore), "RestrictedWordsSource");
        topology.addProcessor("BlockAndCensorProcessor", () -> new BlockAndCensorProcessor(blockedUsersStore, restrictedWordsStore), "MessagesSource");

        // Хранилища состояний (State Stores): добавляем State Store для обработки данных в процессорах
        StoreBuilder<KeyValueStore<String, List<String>>> blockedUsersStoreBuilder = StoresFactory.blockedUsersStoreBuilder(blockedUsersStore);
        StoreBuilder<KeyValueStore<String, String>> restrictedWordsStoreBuilder = StoresFactory.restrictedWordsStoreBuilder(restrictedWordsStore);

        topology.addStateStore(blockedUsersStoreBuilder, "BlockedUsersProcessor", "BlockAndCensorProcessor");
        topology.addStateStore(restrictedWordsStoreBuilder, "RestrictedWordsProcessor", "BlockAndCensorProcessor");

        // Синк (Sink): добавляем конечную точку для фильтрованных сообщений
        topology.addSink("FilteredSink", filteredMessagesTopic, Serdes.String().serializer(), new JsonSerde<>(MessageModel.class).serializer(), "BlockAndCensorProcessor");

        return topology;
    }
}
