-- ============================================================================
-- V3: Наполнение БД тестовыми (seed) данными для ручного тестирования API.
--
-- Цель: после применения миграции можно сразу выполнить POST /v1/check-group
-- с ключами групп ru_rub_payments и vip_high_amount.
--
-- Только DML (INSERT), без PL/pgSQL-блоков.
--
-- Идемпотентность: каждая вставка использует ON CONFLICT DO NOTHING по
-- уникальным бизнес-ключам/первичным ключам:
--   conditions       -> uk_conditions_condition_key (condition_key)
--   condition_groups -> uk_condition_groups_key     (key)
--   group_conditions -> pk_group_conditions         (group_id, condition_id)
--
-- Привязка условий к группам выполняется по бизнес-ключам через подзапросы,
-- поскольку id у таблиц GENERATED ALWAYS AS IDENTITY и не могут быть заданы явно.
--
-- Семантика containment (@>): payload каждого условия должен быть подмножеством
-- входящего JSON-объекта. Пример:
--   POST /v1/check-group
--   { "group_key": "ru_rub_payments",
--     "fields":    { "country": "RU", "currency": "RUB", "amount": 1000 } }
--   -> matched=true (все условия группы содержатся во входящем объекте).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Условия (conditions)
-- ----------------------------------------------------------------------------
INSERT INTO conditions (condition_key, payload)
VALUES
    ('country_ru',      '{"country": "RU"}'),
    ('currency_rub',    '{"currency": "RUB"}'),
    ('min_amount_1000', '{"minAmount": 1000}'),
    ('vip_client',      '{"clientType": "VIP"}')
ON CONFLICT (condition_key) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 2. Группы условий (condition_groups)
-- ----------------------------------------------------------------------------
INSERT INTO condition_groups (key, name, description)
VALUES
    ('ru_rub_payments',
     'Платежи в рублях из РФ',
     'Условия: страна RU и валюта RUB'),
    ('vip_high_amount',
     'VIP-клиенты с крупными суммами',
     'Условия: тип клиента VIP и сумма от 1000')
ON CONFLICT (key) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 3. Связи условий с группами (group_conditions)
-- ----------------------------------------------------------------------------
-- Группа ru_rub_payments -> country_ru + currency_rub
INSERT INTO group_conditions (group_id, condition_id)
SELECT g.id, c.id
FROM condition_groups g
JOIN conditions c ON c.condition_key IN ('country_ru', 'currency_rub')
WHERE g.key = 'ru_rub_payments'
ON CONFLICT (group_id, condition_id) DO NOTHING;

-- Группа vip_high_amount -> vip_client + min_amount_1000
INSERT INTO group_conditions (group_id, condition_id)
SELECT g.id, c.id
FROM condition_groups g
JOIN conditions c ON c.condition_key IN ('vip_client', 'min_amount_1000')
WHERE g.key = 'vip_high_amount'
ON CONFLICT (group_id, condition_id) DO NOTHING;