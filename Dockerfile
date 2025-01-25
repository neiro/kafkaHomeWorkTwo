# -------------------------------------------------
# 1) ЭТАП СБОРКИ: Используем официальный образ Maven
# -------------------------------------------------
    FROM maven:3.9.9-eclipse-temurin-17 AS build
    WORKDIR /app
    
    # Копируем pom.xml и загружаем зависимости (кэшируем)
    COPY pom.xml .
    RUN mvn -B dependency:go-offline
    
    # Копируем исходники и собираем
    COPY src ./src
    RUN mvn -B clean package -DskipTests
    
    # -------------------------------------------------------------------
    # 2) ЭТАП РАНТАЙМ: "тонкий" образ с JRE/JDK (eclipse-temurin/alpine)
    # -------------------------------------------------------------------
    FROM eclipse-temurin:23-jdk-alpine
    WORKDIR /app
    
    # Копируем готовый .jar из предыдущего этапа
    COPY --from=build /app/target/kafkaHomeWorkTwo-1.0-SNAPSHOT.jar /app/kafkaHomeWorkTwo.jar

    
    EXPOSE 8080
    
    ENTRYPOINT ["java", "-jar", "/app/kafkaHomeWorkTwo.jar"]
    