-- ============================================================
-- Migration: V005__event_task.sql
-- Description: EVENT, EVENT_TRANSLATION, TASK, TASK_SUBMISSION
--              + deferred FK constraints on MARK
-- ============================================================

-- ============================================================
-- EVENT
-- ============================================================

CREATE TABLE event (
    id               SERIAL       PRIMARY KEY,
    subject_id       INT          NOT NULL REFERENCES subject(id) ON DELETE CASCADE,
    type             VARCHAR(15)  NOT NULL CHECK (type IN (
                         'PREDNASKA','CVICENIE','ZAPOCET',
                         'ODOVZDANIE','TASK','EXAM','PISOMKA'
                     )),
    week_number      INT,
    room             VARCHAR(100),
    scheduled_at     TIMESTAMPTZ,
    duration_minutes INT,
    is_published     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by       INT          REFERENCES "user"(id) ON DELETE SET NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  event      IS 'Scheduled subject events: lectures, labs, exams, etc.';
COMMENT ON COLUMN event.type IS 'PREDNASKA=lecture, CVICENIE=lab, ZAPOCET=credit-test, ODOVZDANIE=submission, EXAM=final exam, PISOMKA=written test';

CREATE TRIGGER trg_event_updated_at
    BEFORE UPDATE ON event
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- ============================================================
-- EVENT_TRANSLATION
-- ============================================================

CREATE TABLE event_translation (
    event_id    INT          NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    locale      VARCHAR(10)  NOT NULL REFERENCES locale(code) ON DELETE RESTRICT,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    PRIMARY KEY (event_id, locale)
);

COMMENT ON TABLE event_translation IS 'Localized event titles/descriptions, POWER_USER managed for syllabus events';

-- ============================================================
-- TASK
-- ============================================================

CREATE TABLE task (
    id           SERIAL       PRIMARY KEY,
    event_id     INT          REFERENCES event(id) ON DELETE SET NULL,
    subject_id   INT          NOT NULL REFERENCES subject(id) ON DELETE CASCADE,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    due_at       TIMESTAMPTZ,
    max_points   DECIMAL(6,2),
    is_published BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by   INT          REFERENCES "user"(id) ON DELETE SET NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  task             IS 'Teacher-created assignments — title/description NOT localized';
COMMENT ON COLUMN task.event_id    IS 'Optional link to a scheduled event (e.g. submission deadline event)';

CREATE TRIGGER trg_task_updated_at
    BEFORE UPDATE ON task
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- ============================================================
-- TASK_SUBMISSION
-- ============================================================

CREATE TABLE task_submission (
    id           SERIAL       PRIMARY KEY,
    task_id      INT          NOT NULL REFERENCES task(id) ON DELETE CASCADE,
    student_id   INT          NOT NULL REFERENCES "user"(id) ON DELETE RESTRICT,
    submitted_at TIMESTAMPTZ,
    content      TEXT,
    file_url     VARCHAR(500),
    status       VARCHAR(10)  NOT NULL CHECK (status IN ('PENDING','SUBMITTED','GRADED','LATE','MISSING')),
    graded_by    INT          REFERENCES "user"(id) ON DELETE SET NULL,
    graded_at    TIMESTAMPTZ,
    CONSTRAINT uq_task_submission_task_student UNIQUE (task_id, student_id)
);

COMMENT ON TABLE  task_submission        IS 'Student submissions for tasks';
COMMENT ON COLUMN task_submission.status IS 'PENDING=not yet submitted, SUBMITTED=awaiting grading, GRADED, LATE, MISSING';

-- ============================================================
-- Patch MARK — add deferred FK constraints
-- ============================================================

ALTER TABLE mark
    ADD CONSTRAINT fk_mark_event
        FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_mark_task_submission
        FOREIGN KEY (task_submission_id) REFERENCES task_submission(id) ON DELETE SET NULL;

-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_event_subject      ON event(subject_id);
CREATE INDEX idx_event_type         ON event(type);
CREATE INDEX idx_event_scheduled    ON event(scheduled_at);
CREATE INDEX idx_task_subject       ON task(subject_id);
CREATE INDEX idx_task_event         ON task(event_id);
CREATE INDEX idx_task_sub_task      ON task_submission(task_id);
CREATE INDEX idx_task_sub_student   ON task_submission(student_id);
CREATE INDEX idx_task_sub_status    ON task_submission(status);
