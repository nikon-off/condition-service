# syntax=docker/dockerfile:1

# ============================================================================
# Stage 1: Build — компиляция и упаковка приложения (JDK 21 + Maven)
# ============================================================================
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Копируем pom.xml отдельно, чтобы слой зависимостей кэшировался между сборками
COPY pom.xml .
# Предзагрузка всех зависимостей в локальный репозиторий Maven (кэш-слой Docker)
RUN mvn dependency:go-offline -B

# Копируем исходники и собираем fat-jar без тестов
# (тесты выполняются отдельно в CI; в образ они не попадают)
COPY src ./src
RUN mvn package -DskipTests -B

# ============================================================================
# Stage 2: Run — минимальный рантайм (JRE 21 alpine), non-root пользователь
# Запуск напрямую через `java -jar` (fat-jar, уже содержит spring-boot-loader).
# ============================================================================
FROM eclipse-temurin:21-jre-alpine

# Создаём non-root пользователя (UID 1000) для безопасности в Kubernetes
# (совместим с securityContext.runAsUser=1000 в Helm chart)
RUN addgroup -S spring && adduser -S spring -G spring -u 1000

WORKDIR /app

# Копируем собранный fat-jar
COPY --from=build /app/target/*.jar app.jar

# Порт по умолчанию (server.port не задан в application.yml -> 8080)
EXPOSE 8080

# Таймзона UTC + слот для дополнительных JVM-опций из ConfigMap/Secret
ENV TZ=UTC \
    JAVA_OPTS=""

# Запуск от non-root пользователя
USER spring:spring

# Старт приложения (JarLauncher вызывается внутри fat-jar автоматически)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]