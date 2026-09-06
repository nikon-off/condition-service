# Condition Service

Сервис для выполнения сложных вычислений, вынесенных из конфигурации 1С для снижения нагрузки на платформу 1С:Предприятие.

## Технологический стек
- Java 21 / Spring Boot 3.x
- PostgreSQL 17.4
- Flyway (миграции БД)
- JPA/Hibernate
- Helm (развертывание в Kubernetes)

## Быстрый старт

### Требования
- JDK 21+
- Maven 3.9+
- Docker & Docker Compose
- Kind (для локального K8s кластера)
- Helm 3.x

### Запуск локально
1. Клонировать репозиторий
2. Запустить PostgreSQL: `docker-compose up -d`
3. Применить миграции: `mvn flyway:migrate`
4. Собрать проект: `mvn clean package`
5. Запустить: `java -jar target/condition-service.jar`

## API Overview
*(будет заполнено по мере стабилизации эндпоинтов)*

## Развертывание в Kubernetes
```bash
helm install condition-service ./helm/condition-service
```

## Конфигурация
Основные параметры в `application.yml`:
- Подключение к БД
- Порт сервиса (по умолчанию: 8080)
- Настройки подключения к 1С (если есть)

## Структура проекта
```
src/main/java/com/nikonoff/conditionservice/
├── controller/     # REST контроллеры
├── service/        # Бизнес-логика вычислений
├── repository/     # JPA репозитории
├── model/          # Entity классы
└── config/         # Конфигурация приложения
```

## Миграции БД
Миграции Flyway находятся в `src/main/resources/db/migration/`
