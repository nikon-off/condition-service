-- ============================================================================
-- V2: Добавление бизнес-ключа условия (condition_key) в таблицу conditions.
--
-- Причина: контракт API (CreateConditionDto) содержит поле conditionKey,
-- но в Entity Condition и таблице conditions оно отсутствовало (рассинхрон).
-- Колонка используется как уникальный бизнес-ключ условия, аналогично
-- business-ключу группы (condition_groups.key).
-- ============================================================================

ALTER TABLE conditions
    ADD COLUMN condition_key VARCHAR(255);

-- Для существующих строк заполняем ключ детерминированным значением,
-- чтобы можно было безопасно наложить NOT NULL (миграция идемпотентна
-- для уже наполненной БД, хотя на этапе dev таблица обычно пустая).
UPDATE conditions
SET condition_key = 'condition_' || id
WHERE condition_key IS NULL;

ALTER TABLE conditions
    ALTER COLUMN condition_key SET NOT NULL;

-- Уникальный индекс на бизнес-ключ (как uk_condition_groups_key у групп).
CREATE UNIQUE INDEX uk_conditions_condition_key
    ON conditions (condition_key);