-- ============================================================
-- Migration: V001__user_and_rbac.sql
-- Description: USER entity + full RBAC (roles, permissions)
-- ============================================================

-- ============================================================
-- ROLE
-- ============================================================

CREATE TABLE role (
                      id          SERIAL          PRIMARY KEY,
                      name        VARCHAR(50)     NOT NULL UNIQUE,
                      description TEXT,
                      created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  role        IS 'Application roles: STUDENT, TEACHER, POWER_USER, ADMIN';
COMMENT ON COLUMN role.name   IS 'Unique role identifier used in permission checks';

-- ============================================================
-- PERMISSION
-- ============================================================

CREATE TABLE permission (
                            id          SERIAL          PRIMARY KEY,
                            name        VARCHAR(100)    NOT NULL UNIQUE,
                            resource    VARCHAR(50)     NOT NULL,
                            action      VARCHAR(50)     NOT NULL,
                            CONSTRAINT  uq_permission_resource_action UNIQUE (resource, action)
);

COMMENT ON TABLE  permission          IS 'Fine-grained permission records, e.g. subjects:create';
COMMENT ON COLUMN permission.resource IS 'Entity being acted on: subjects, marks, tasks, users …';
COMMENT ON COLUMN permission.action   IS 'Allowed operation: READ, WRITE, DELETE, MANAGE';

-- ============================================================
-- ROLE_PERMISSION  (junction)
-- ============================================================

CREATE TABLE role_permission (
                                 role_id       INT         NOT NULL REFERENCES role(id)       ON DELETE CASCADE,
                                 permission_id INT         NOT NULL REFERENCES permission(id) ON DELETE CASCADE,
                                 granted_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 PRIMARY KEY (role_id, permission_id)
);

COMMENT ON TABLE role_permission IS 'Many-to-many: which permissions each role grants';

-- ============================================================
-- USER
-- ============================================================

CREATE TABLE "user" (
                        id                  SERIAL          PRIMARY KEY,
                        email               VARCHAR(255)    NOT NULL UNIQUE,
                        first_name          VARCHAR(100)    NOT NULL,
                        last_name           VARCHAR(100)    NOT NULL,
                        password_hash       VARCHAR(255)    NOT NULL,
                        is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
                        profile_picture_url VARCHAR(500),
                        created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                        updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  "user"                   IS 'All system users regardless of role';
COMMENT ON COLUMN "user".password_hash     IS 'Bcrypt/Argon2 hash — never store plaintext';
COMMENT ON COLUMN "user".is_active         IS 'Soft-disable account without deleting records';

-- Auto-update updated_at on every row change
CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_user_updated_at
    BEFORE UPDATE ON "user"
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- ============================================================
-- USER_ROLE  (junction)
-- ============================================================

CREATE TABLE user_role (
                           user_id     INT         NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
                           role_id     INT         NOT NULL REFERENCES role(id)   ON DELETE RESTRICT,
                           assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                           assigned_by INT                  REFERENCES "user"(id) ON DELETE SET NULL,
                           PRIMARY KEY (user_id, role_id)
);

COMMENT ON TABLE  user_role             IS 'Many-to-many: users can hold multiple roles';
COMMENT ON COLUMN user_role.assigned_by IS 'NULL = self-registered or system-assigned';

-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_user_email         ON "user"(email);
CREATE INDEX idx_user_role_user_id  ON user_role(user_id);
CREATE INDEX idx_user_role_role_id  ON user_role(role_id);

-- ============================================================
-- SEED — default roles
-- ============================================================

INSERT INTO role (name, description) VALUES
                                         ('STUDENT',    'Enrolled student — can view own data, enroll in subjects, submit tasks'),
                                         ('TEACHER',    'Teacher — can add marks, create tasks, send notifications'),
                                         ('POWER_USER', 'Power user — can manage subjects, events, and syllabus content'),
                                         ('ADMIN',      'System administrator — full access');

-- ============================================================
-- SEED — default permissions
-- ============================================================

INSERT INTO permission (name, resource, action) VALUES
                                                    -- subjects
                                                    ('subjects:read',       'subjects',     'READ'),
                                                    ('subjects:write',      'subjects',     'WRITE'),
                                                    ('subjects:delete',     'subjects',     'DELETE'),
                                                    ('subjects:manage',     'subjects',     'MANAGE'),
                                                    -- enrollments
                                                    ('enrollments:read',    'enrollments',  'READ'),
                                                    ('enrollments:write',   'enrollments',  'WRITE'),
                                                    ('enrollments:manage',  'enrollments',  'MANAGE'),
                                                    -- marks
                                                    ('marks:read',          'marks',        'READ'),
                                                    ('marks:write',         'marks',        'WRITE'),
                                                    ('marks:manage',        'marks',        'MANAGE'),
                                                    -- tasks
                                                    ('tasks:read',          'tasks',        'READ'),
                                                    ('tasks:write',         'tasks',        'WRITE'),
                                                    ('tasks:manage',        'tasks',        'MANAGE'),
                                                    -- events
                                                    ('events:read',         'events',       'READ'),
                                                    ('events:write',        'events',       'WRITE'),
                                                    ('events:manage',       'events',       'MANAGE'),
                                                    -- notifications
                                                    ('notifications:read',  'notifications','READ'),
                                                    ('notifications:write', 'notifications','WRITE'),
                                                    -- users
                                                    ('users:read',          'users',        'READ'),
                                                    ('users:manage',        'users',        'MANAGE');

-- ============================================================
-- SEED — role → permission assignments
-- ============================================================

-- STUDENT: read subjects/enrollments/marks/tasks/events/notifications
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.name = 'STUDENT'
  AND p.name IN (
                 'subjects:read',
                 'enrollments:read', 'enrollments:write',
                 'marks:read',
                 'tasks:read',
                 'events:read',
                 'notifications:read'
    );

-- TEACHER: student perms + write marks/tasks/notifications
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.name = 'TEACHER'
  AND p.name IN (
                 'subjects:read',
                 'enrollments:read',
                 'marks:read',   'marks:write',
                 'tasks:read',   'tasks:write',
                 'events:read',
                 'notifications:read', 'notifications:write',
                 'users:read'
    );

-- POWER_USER: teacher perms + manage subjects/events/enrollments
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.name = 'POWER_USER'
  AND p.name IN (
                 'subjects:read',    'subjects:write',    'subjects:manage',
                 'enrollments:read', 'enrollments:write', 'enrollments:manage',
                 'marks:read',       'marks:write',       'marks:manage',
                 'tasks:read',       'tasks:write',       'tasks:manage',
                 'events:read',      'events:write',      'events:manage',
                 'notifications:read','notifications:write',
                 'users:read'
    );

-- ADMIN: all permissions
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.name = 'ADMIN';

