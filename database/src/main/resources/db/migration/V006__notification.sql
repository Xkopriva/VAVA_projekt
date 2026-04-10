-- ============================================================
-- Migration: V006__notification.sql
-- Description: NOTIFICATION table
-- ============================================================

CREATE TABLE notification (
    id                 SERIAL       PRIMARY KEY,
    recipient_id       INT          NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    sender_id          INT          REFERENCES "user"(id) ON DELETE SET NULL,
    type               VARCHAR(25)  NOT NULL CHECK (type IN (
                           'MARK_ADDED','TASK_DUE','ENROLLMENT_OPEN',
                           'ANNOUNCEMENT','EXAM_SCHEDULED','SUBMISSION_GRADED'
                       )),
    title              VARCHAR(255) NOT NULL,
    message            TEXT,
    is_read            BOOLEAN      NOT NULL DEFAULT FALSE,
    related_mark_id    INT          REFERENCES mark(id) ON DELETE SET NULL,
    related_subject_id INT          REFERENCES subject(id) ON DELETE SET NULL,
    related_task_id    INT          REFERENCES task(id) ON DELETE SET NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    read_at            TIMESTAMPTZ,
    CONSTRAINT chk_notification_read_at
        CHECK (read_at IS NULL OR is_read = TRUE)
);

COMMENT ON TABLE  notification         IS 'User notifications — NOT localized, dynamic content at send time';
COMMENT ON COLUMN notification.type    IS 'Frontend maps enum key to i18n label';
COMMENT ON COLUMN notification.message IS 'Generated in recipient locale at send time; not re-translated later';
COMMENT ON COLUMN notification.read_at IS 'Timestamp when user opened/read the notification';

-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_notification_recipient ON notification(recipient_id);
CREATE INDEX idx_notification_is_read   ON notification(is_read);
CREATE INDEX idx_notification_type      ON notification(type);
CREATE INDEX idx_notification_created   ON notification(created_at DESC);
