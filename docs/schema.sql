-- =====================================================================
-- KeepCoding - Schema PostgreSQL (referencia)
-- O backend cria estas tabelas automaticamente via JPA (ddl-auto: update).
-- Este arquivo serve como documentacao e como base para migrations
-- (Flyway/Liquibase) em producao.
-- =====================================================================

-- ---------- Usuarios ----------
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT GENERATED ALWAYS AS IDENTITY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,             -- hash BCrypt
    xp          INTEGER      NOT NULL DEFAULT 0,
    tier_plan   VARCHAR(20)  NOT NULL DEFAULT 'FREE', -- FREE | PRO | TEAM
    PRIMARY KEY (id)
);

-- ---------- Problemas ----------
CREATE TABLE IF NOT EXISTS problems (
    id           BIGINT GENERATED ALWAYS AS IDENTITY,
    title        VARCHAR(255) NOT NULL,
    description  TEXT         NOT NULL,
    difficulty   VARCHAR(10)  NOT NULL,             -- EASY | MEDIUM | HARD
    time_limit   INTEGER      NOT NULL DEFAULT 2000,    -- ms
    memory_limit INTEGER      NOT NULL DEFAULT 128000,  -- kb
    PRIMARY KEY (id)
);

-- ---------- Casos de teste ----------
CREATE TABLE IF NOT EXISTS test_cases (
    id              BIGINT GENERATED ALWAYS AS IDENTITY,
    problem_id      BIGINT  NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    input           TEXT    NOT NULL,
    expected_output TEXT    NOT NULL,
    is_sample       BOOLEAN NOT NULL DEFAULT FALSE,  -- TRUE = exemplo visivel
    PRIMARY KEY (id)
);

-- ---------- Submissoes ----------
CREATE TABLE IF NOT EXISTS submissions (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    user_id             BIGINT      NOT NULL REFERENCES users (id),
    problem_id          BIGINT      NOT NULL REFERENCES problems (id),
    language            VARCHAR(20) NOT NULL,        -- JAVA | TYPESCRIPT
    code                TEXT        NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                    -- PENDING | ACCEPTED | WRONG_ANSWER | TIME_LIMIT | ERROR
    coach_feedback_json JSONB,                       -- feedback estruturado do DevCoach
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id)
);

-- ---------- Indices uteis ----------
CREATE INDEX IF NOT EXISTS idx_test_cases_problem  ON test_cases  (problem_id);
CREATE INDEX IF NOT EXISTS idx_submissions_user    ON submissions (user_id);
CREATE INDEX IF NOT EXISTS idx_submissions_problem ON submissions (problem_id);
