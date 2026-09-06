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
# Stage 2: Extract — извлечение слоёв из fat-jar (spring-boot jarmode tools)
# Для эффективного кэширования слоёв образа: стабильные слои (dependencies,
# spring-boot-loader) переиспользуются, меняется только слой application.
# ============================================================================
FROM eclipse-temurin:21-jdk-alpine AS extractor
WORKDIR /app

# jarmode=tools: извлекает слои (dependencies, snapshot-dependencies,
# spring-boot-loader, application) в отдельные директории под /extracted
COPY --from=build /app/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# ============================================================================
# Stage 3: Run — минимальный рантайм (JRE 21 alpine), non-root пользователь
# ============================================================================
FROM eclipse-temurin:21-jre-alpine

# Создаём non-root пользователя (UID 1000) для безопасности в Kubernetes
# (совместим с securityContext.runAsUser=1000 в Helm chart)
RUN addgroup -S spring && adduser -S spring -G spring -u 1000

WORKDIR /app

# Копируем слои в порядке от наименее изменяемых к наиболее изменяемым
COPY --from=extractor /app/extracted/dependencies/ ./
COPY --from=extractor /app/extracted/spring-boot-loader/ ./
COPY --from=extractor /app/extracted/snapshot-dependencies/ ./
COPY --from=extractor /app/extracted/application/ ./

# Порт по умолчанию (server.port не задан в application.yml -> 8080)
EXPOSE 8080

# Таймзона UTC + слот для дополнительных JVM-опций из ConfigMap/Secret
ENV TZ=UTC \
    JAVA_OPTS=""

# Запуск от non-root пользователя
USER spring:spring

# Старт приложения из извлечённых слоёв через официальный Spring Boot JarLauncher
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]