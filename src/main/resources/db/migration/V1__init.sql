-- ============================================================================
-- V1: Инициализация схемы «Сервиса проверки условий отбора» (Condition Checking Service)
-- БД: PostgreSQL 17
-- Тип миграции: DDL (без PL/pgSQL-блоков)
--
-- Двухуровневая модель:
--   conditions        (Условия)        -- payload JSONB
--   condition_groups  (Группы условий) -- агрегация условий
--   group_conditions  (M2M-связь)      -- условие <-> группа
--
-- Ключевое требование: для оператора containment (@>) по JSONB-колонке
-- payload создаётся GIN-индекс с операторным классом jsonb_path_ops.
-- Данный класс индексов оптимизирован именно под оператор @> и занимает
-- меньше места, чем стандартный jsonb_ops (поддерживает только @>, без ?/?|/?&/@?/@@).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Таблица условий отбора
-- ----------------------------------------------------------------------------
CREATE TABLE conditions (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- JSONB-структура условия (объект). Верифицируется триггерами/API при вставке.
    payload    JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- payload должен быть JSON-объектом: containment (@>) определён для объектов
    CONSTRAINT chk_conditions_payload_object CHECK (jsonb_typeof(payload) = 'object')
);

-- 4. GIN-индекс для поддержки оператора containment (@>) на колонке payload
CREATE INDEX idx_conditions_payload_gin
    ON conditions USING GIN (payload jsonb_path_ops);

-- ----------------------------------------------------------------------------
-- 2. Таблица групп условий отбора
-- ----------------------------------------------------------------------------
CREATE TABLE condition_groups (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- Бизнес-ключ группы (используется в контракте API, напр. check-group)
    key         VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_condition_groups_key UNIQUE (key)
);

-- ----------------------------------------------------------------------------
-- 3. Связующая таблица (M2M): условия <-> группы условий
--    ON DELETE CASCADE: удаление условия/группы автоматически удаляет связи.
-- ----------------------------------------------------------------------------
CREATE TABLE group_conditions (
    group_id     BIGINT NOT NULL,
    condition_id BIGINT NOT NULL,
    CONSTRAINT pk_group_conditions PRIMARY KEY (group_id, condition_id),
    CONSTRAINT fk_group_conditions_group
        FOREIGN KEY (group_id) REFERENCES condition_groups (id) ON DELETE CASCADE,
    CONSTRAINT fk_group_conditions_condition
        FOREIGN KEY (condition_id) REFERENCES conditions (id) ON DELETE CASCADE
);

-- Индекс для обратной выборки (условие -> его группы) и ускорения JOIN'ов
CREATE INDEX idx_group_conditions_condition_id
    ON group_conditions (condition_id);