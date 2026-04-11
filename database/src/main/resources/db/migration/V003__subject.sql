-- ============================================================
-- Migration: V003__subject.sql
-- Description: SUBJECT, SUBJECT_TRANSLATION, SUBJECT_GROUP,
--              SUBJECT_GROUP_TRANSLATION, SUBJECT_GROUP_ITEM
-- ============================================================

-- ============================================================
-- SUBJECT
-- ============================================================

CREATE TABLE subject (
    id                      SERIAL       PRIMARY KEY,
    code                    VARCHAR(20)  NOT NULL UNIQUE,
    external_id             INT,
    faculty                 VARCHAR(20),
    credits                 INT          NOT NULL,
    is_mandatory            BOOLEAN      NOT NULL DEFAULT FALSE,
    is_profiled             BOOLEAN      NOT NULL DEFAULT FALSE,
    completion_type         VARCHAR(20)  NOT NULL CHECK (completion_type IN ('EXAM', 'GRADED_CREDIT', 'PASS_CREDIT')),
    lecture_hrs_weekly      INT,
    lab_hrs_weekly          INT,
    seminar_hrs_weekly      INT,
    project_hrs_weekly      INT,
    language_of_instruction VARCHAR(50),
    assessment_breakdown    TEXT,
    recommended_semester    INT,
    guarantor_id            INT          REFERENCES "user"(id) ON DELETE SET NULL,
    avg_student_rating      DECIMAL(2,1),
    subject_difficulty      DECIMAL(2,1),
    total_assessed_students INT,
    grade_a_pct             DECIMAL(5,2),
    grade_b_pct             DECIMAL(5,2),
    grade_c_pct             DECIMAL(5,2),
    grade_d_pct             DECIMAL(5,2),
    grade_e_pct             DECIMAL(5,2),
    grade_fx_pct            DECIMAL(5,2),
    last_modified           DATE,
    created_by              INT          REFERENCES "user"(id) ON DELETE SET NULL,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  subject                      IS 'Academic subjects / courses';
COMMENT ON COLUMN subject.code                 IS 'Short subject code, e.g. DBS_B, VAVA_B';
COMMENT ON COLUMN subject.external_id          IS 'predmet_id from the university IS';
COMMENT ON COLUMN subject.is_mandatory         IS 'TRUE = compulsory in the study plan';
COMMENT ON COLUMN subject.is_profiled          IS 'TRUE = counted toward profiled credits';
COMMENT ON COLUMN subject.completion_type      IS 'EXAM | GRADED_CREDIT | PASS_CREDIT';
COMMENT ON COLUMN subject.assessment_breakdown IS 'Raw text describing score weights, teacher language';

CREATE TRIGGER trg_subject_updated_at
    BEFORE UPDATE ON subject
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- ============================================================
-- SUBJECT_TRANSLATION
-- ============================================================

CREATE TABLE subject_translation (
    subject_id  INT          NOT NULL REFERENCES subject(id) ON DELETE CASCADE,
    locale      VARCHAR(10)  NOT NULL REFERENCES locale(code) ON DELETE RESTRICT,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    PRIMARY KEY (subject_id, locale)
);

COMMENT ON TABLE  subject_translation             IS 'Localized subject names and descriptions, POWER_USER managed';
COMMENT ON COLUMN subject_translation.description IS 'Full syllabus description in this locale';

-- ============================================================
-- SUBJECT_GROUP
-- ============================================================

CREATE TABLE subject_group (
    id          SERIAL      PRIMARY KEY,
    semester_id INT         NOT NULL REFERENCES semester(id) ON DELETE RESTRICT,
    sort_order  INT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE subject_group IS 'Groups of subjects within a semester (mandatory, semi-elective, etc.)';

-- ============================================================
-- SUBJECT_GROUP_TRANSLATION
-- ============================================================

CREATE TABLE subject_group_translation (
    subject_group_id INT          NOT NULL REFERENCES subject_group(id) ON DELETE CASCADE,
    locale           VARCHAR(10)  NOT NULL REFERENCES locale(code) ON DELETE RESTRICT,
    name             VARCHAR(100) NOT NULL,
    description      TEXT,
    PRIMARY KEY (subject_group_id, locale)
);

COMMENT ON TABLE subject_group_translation IS 'Localized subject group names, POWER_USER managed';

-- ============================================================
-- SUBJECT_GROUP_ITEM
-- ============================================================

CREATE TABLE subject_group_item (
    subject_group_id INT     NOT NULL REFERENCES subject_group(id) ON DELETE CASCADE,
    subject_id       INT     NOT NULL REFERENCES subject(id) ON DELETE CASCADE,
    is_mandatory     BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (subject_group_id, subject_id)
);

COMMENT ON TABLE  subject_group_item              IS 'Links subjects to semester groups';
COMMENT ON COLUMN subject_group_item.is_mandatory IS 'Whether this subject is required within this group';

-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_subject_code                ON subject(code);
CREATE INDEX idx_subject_faculty             ON subject(faculty);
CREATE INDEX idx_subject_guarantor           ON subject(guarantor_id);
CREATE INDEX idx_subject_completion          ON subject(completion_type);
CREATE INDEX idx_subject_translation_locale  ON subject_translation(locale);
CREATE INDEX idx_subject_group_semester      ON subject_group(semester_id);
CREATE INDEX idx_subject_group_item_subject  ON subject_group_item(subject_id);
