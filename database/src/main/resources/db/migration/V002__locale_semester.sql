-- ============================================================
-- Migration: V002__locale_semester.sql
-- Description: LOCALE + SEMESTER tables with i18n support
-- ============================================================

-- ============================================================
-- LOCALE
-- ============================================================

CREATE TABLE locale (
    code       VARCHAR(10) PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    is_default BOOLEAN     NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE  locale            IS 'Supported UI locales';
COMMENT ON COLUMN locale.code       IS 'BCP-47 language tag, e.g. sk, en';
COMMENT ON COLUMN locale.is_default IS 'Exactly one locale should be TRUE (sk by default)';

-- ============================================================
-- SEMESTER
-- ============================================================

CREATE TABLE semester (
    id            SERIAL      PRIMARY KEY,
    code          VARCHAR(20) NOT NULL UNIQUE,
    type          VARCHAR(10) NOT NULL CHECK (type IN ('WINTER', 'SUMMER')),
    academic_year VARCHAR(9)  NOT NULL,
    start_date    DATE,
    end_date      DATE,
    status        VARCHAR(10) NOT NULL CHECK (status IN ('PLANNED', 'ACTIVE', 'FINISHED')),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  semester               IS 'Academic semesters';
COMMENT ON COLUMN semester.code          IS 'e.g. WS_2024_2025, SS_2025_2026';
COMMENT ON COLUMN semester.academic_year IS 'e.g. 2024/2025';

-- ============================================================
-- SEMESTER_TRANSLATION
-- ============================================================

CREATE TABLE semester_translation (
    semester_id INT          NOT NULL REFERENCES semester(id) ON DELETE CASCADE,
    locale      VARCHAR(10)  NOT NULL REFERENCES locale(code) ON DELETE RESTRICT,
    name        VARCHAR(100) NOT NULL,
    PRIMARY KEY (semester_id, locale)
);

COMMENT ON TABLE semester_translation IS 'Localized semester names, POWER_USER managed';

-- INDEXES
CREATE INDEX idx_semester_status             ON semester(status);
CREATE INDEX idx_semester_translation_locale ON semester_translation(locale);

-- ============================================================
-- SEED — locales
-- ============================================================

INSERT INTO locale (code, name, is_default) VALUES
    ('sk', 'Slovenčina', TRUE),
    ('en', 'English',    FALSE);

-- ============================================================
-- SEED — semesters
-- ============================================================

INSERT INTO semester (code, type, academic_year, start_date, end_date, status) VALUES
    ('WS_2024_2025', 'WINTER', '2024/2025', '2024-09-23', '2025-01-31', 'FINISHED'),
    ('SS_2024_2025', 'SUMMER', '2024/2025', '2025-02-17', '2025-06-30', 'FINISHED'),
    ('WS_2025_2026', 'WINTER', '2025/2026', '2025-09-22', '2026-01-30', 'FINISHED'),
    ('SS_2025_2026', 'SUMMER', '2025/2026', '2026-02-16', '2026-06-28', 'ACTIVE');

-- ============================================================
-- SEED — semester translations
-- ============================================================

INSERT INTO semester_translation (semester_id, locale, name)
SELECT s.id, 'sk', t.name_sk
FROM semester s
JOIN (VALUES
    ('WS_2024_2025', 'Zimný semester 2024/2025'),
    ('SS_2024_2025', 'Letný semester 2024/2025'),
    ('WS_2025_2026', 'Zimný semester 2025/2026'),
    ('SS_2025_2026', 'Letný semester 2025/2026')
) AS t(code, name_sk) ON s.code = t.code;

INSERT INTO semester_translation (semester_id, locale, name)
SELECT s.id, 'en', t.name_en
FROM semester s
JOIN (VALUES
    ('WS_2024_2025', 'Winter Semester 2024/2025'),
    ('SS_2024_2025', 'Summer Semester 2024/2025'),
    ('WS_2025_2026', 'Winter Semester 2025/2026'),
    ('SS_2025_2026', 'Summer Semester 2025/2026')
) AS t(code, name_en) ON s.code = t.code;
