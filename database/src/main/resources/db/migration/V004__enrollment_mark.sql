-- ============================================================
-- Migration: V004__enrollment_mark.sql
-- Description: ENROLLMENT, INDEX_RECORD, MARK tables
-- ============================================================

-- ============================================================
-- ENROLLMENT
-- ============================================================

CREATE TABLE enrollment (
    id             SERIAL      PRIMARY KEY,
    student_id     INT         NOT NULL REFERENCES "user"(id) ON DELETE RESTRICT,
    subject_id     INT         NOT NULL REFERENCES subject(id) ON DELETE RESTRICT,
    semester_id    INT         NOT NULL REFERENCES semester(id) ON DELETE RESTRICT,
    attempt_number INT         NOT NULL DEFAULT 1,
    enrolled_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status         VARCHAR(10) NOT NULL CHECK (status IN ('ACTIVE', 'PASSED', 'FAILED', 'WITHDRAWN')),
    CONSTRAINT uq_enrollment_student_subject_attempt
        UNIQUE (student_id, subject_id, attempt_number)
);

COMMENT ON TABLE  enrollment                IS 'Student enrollment in a subject for a given attempt';
COMMENT ON COLUMN enrollment.attempt_number IS '1 = first attempt, 2 = re-attempt after FAILED';
COMMENT ON COLUMN enrollment.status         IS 'ACTIVE (in-progress) | PASSED | FAILED | WITHDRAWN';

-- ============================================================
-- INDEX_RECORD
-- ============================================================

CREATE TABLE index_record (
    id            SERIAL      PRIMARY KEY,
    enrollment_id INT         NOT NULL UNIQUE REFERENCES enrollment(id) ON DELETE RESTRICT,
    recorded_by   INT         REFERENCES "user"(id) ON DELETE SET NULL,
    final_mark    VARCHAR(5)  NOT NULL CHECK (final_mark IN ('A','B','C','D','E','FX','PASS','FAIL')),
    recorded_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    exam_date     DATE,
    notes         TEXT
);

COMMENT ON TABLE  index_record            IS 'Official final grade record for one enrollment';
COMMENT ON COLUMN index_record.final_mark IS 'A/B/C/D/E/FX for exams, PASS/FAIL for pass-credit subjects';
COMMENT ON COLUMN index_record.exam_date  IS 'Date of the exam or credit assessment';

-- ============================================================
-- MARK
-- ============================================================

CREATE TABLE mark (
    id                 SERIAL       PRIMARY KEY,
    enrollment_id      INT          NOT NULL REFERENCES enrollment(id) ON DELETE RESTRICT,
    -- FKs to event and task_submission are added in V005 after those tables exist
    event_id           INT,
    task_submission_id INT,
    title              VARCHAR(255) NOT NULL,
    points             DECIMAL(6,2) NOT NULL,
    max_points         DECIMAL(6,2) NOT NULL,
    given_by           INT          REFERENCES "user"(id) ON DELETE SET NULL,
    given_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    notes              TEXT,
    CONSTRAINT chk_mark_points CHECK (points >= 0 AND points <= max_points)
);

COMMENT ON TABLE  mark              IS 'Individual scored items (assignments, tests) within an enrollment';
COMMENT ON COLUMN mark.title        IS 'Written by teacher in their own language — not localized';
COMMENT ON COLUMN mark.event_id     IS 'Nullable FK to event — constraint added in V005';
COMMENT ON COLUMN mark.task_submission_id IS 'Nullable FK to task_submission — constraint added in V005';

-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_enrollment_student  ON enrollment(student_id);
CREATE INDEX idx_enrollment_subject  ON enrollment(subject_id);
CREATE INDEX idx_enrollment_semester ON enrollment(semester_id);
CREATE INDEX idx_enrollment_status   ON enrollment(status);
CREATE INDEX idx_index_record_mark   ON index_record(final_mark);
CREATE INDEX idx_mark_enrollment     ON mark(enrollment_id);
CREATE INDEX idx_mark_given_by       ON mark(given_by);
