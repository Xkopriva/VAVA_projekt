-- ============================================================
-- Migration: V008__seed_student.sql
-- Description: Seed system admin user + student Jožko Mrkvička
--              with all enrollments and final marks from
--              data-subjects.csv.
-- ============================================================

-- ============================================================
-- USERS
-- Password hashes use bcrypt cost 12.
-- Plaintext for both accounts: "heslo"
-- ============================================================

INSERT INTO "user" (email, first_name, last_name, password_hash, is_active) VALUES
    -- System admin used as recorded_by for seeded index records
    ('admin@fiit.stuba.sk', 'System', 'Admin',
     '$2a$12$dl3d/QgrPtMXSInE71JJduqMjpfUZY6iL1ZxsqvrxUu.OPr5DdqDS',
     TRUE),
    -- Student: Jožko Mrkvička
    ('jozko.mrkvicka@stuba.sk', 'Jožko', 'Mrkvička',
     '$2a$12$dl3d/QgrPtMXSInE71JJduqMjpfUZY6iL1ZxsqvrxUu.OPr5DdqDS',
     TRUE);

-- ============================================================
-- ROLE ASSIGNMENTS
-- ============================================================

-- Admin gets ADMIN role
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM "user" u, role r
WHERE u.email = 'admin@fiit.stuba.sk' AND r.name = 'ADMIN';

-- Student gets STUDENT role
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM "user" u, role r
WHERE u.email = 'jozko.mrkvicka@stuba.sk' AND r.name = 'STUDENT';

-- ============================================================
-- ENROLLMENTS + INDEX RECORDS
--
-- Mapping from data-subjects.csv result column:
--   excellent (A) → A
--   very good (B) → B
--   good (C)      → C
--   laudable (D)  → D
--   passed (pass.)→ PASS
--
-- PKS_B and ADM_B each had 2 attempts:
--   attempt 1 → FAILED / FX
--   attempt 2 → PASSED / final mark
-- ============================================================

-- ── WS 2024/2025 — all PASSED ────────────────────────────────────────────────

INSERT INTO enrollment (student_id, subject_id, semester_id, attempt_number, enrolled_at, status)
SELECT
    (SELECT id FROM "user" WHERE email = 'jozko.mrkvicka@stuba.sk'),
    s.id,
    sem.id,
    1,
    '2024-09-23 08:00:00+00',
    'PASSED'
FROM subject s, semester sem
WHERE s.code IN ('MA_B','MIP_B','AJ1_B','PPI_B','PRPR_B')
  AND sem.code = 'WS_2024_2025';

-- ADM_B attempt 1 — FAILED (took 2 attempts total)
INSERT INTO enrollment (student_id, subject_id, semester_id, attempt_number, enrolled_at, status)
SELECT
    (SELECT id FROM "user" WHERE email = 'jozko.mrkvicka@stuba.sk'),
    s.id,
    sem.id,
    1,
    '2024-09-23 08:00:00+00',
    'FAILED'
FROM subject s, semester sem
WHERE s.code = 'ADM_B' AND sem.code = 'WS_2024_2025';

-- ADM_B attempt 2 — PASSED
INSERT INTO enrollment (student_id, subject_id, semester_id, attempt_number, enrolled_at, status)
SELECT
    (SELECT id FROM "user" WHERE email = 'jozko.mrkvicka@stuba.sk'),
    s.id,
    sem.id,
    2,
    '2025-02-17 08:00:00+00',
    'PASSED'
FROM subject s, semester sem
WHERE s.code = 'ADM_B' AND sem.code = 'WS_2024_2025';

-- Index records for WS 2024/2025 (final passing attempts)
INSERT INTO index_record (enrollment_id, recorded_by, final_mark, recorded_at, exam_date)
SELECT
    e.id,
    (SELECT id FROM "user" WHERE email = 'admin@fiit.stuba.sk'),
    t.mark,
    '2025-01-31 12:00:00+00',
    t.exam_date::DATE
FROM enrollment e
JOIN subject s ON s.id = e.subject_id
JOIN semester sem ON sem.id = e.semester_id
JOIN (VALUES
    ('ADM_B',  'D', '2025-01-28'),
    ('MA_B',   'D', '2025-01-20'),
    ('MIP_B',  'A', '2025-01-15'),
    ('AJ1_B',  'B', '2025-01-10'),
    ('PPI_B',  'D', '2025-01-22'),
    ('PRPR_B', 'A', '2025-01-17')
) AS t(code, mark, exam_date) ON s.code = t.code
WHERE sem.code = 'WS_2024_2025'
  AND e.status = 'PASSED';

-- FX record for ADM_B failed attempt
INSERT INTO index_record (enrollment_id, recorded_by, final_mark, recorded_at, exam_date)
SELECT
    e.id,
    (SELECT id FROM "user" WHERE email = 'admin@fiit.stuba.sk'),
    'FX',
    '2025-01-28 12:00:00+00',
    '2025-01-28'
FROM enrollment e
JOIN subject s ON s.id = e.subject_id
JOIN semester sem ON sem.id = e.semester_id
WHERE s.code = 'ADM_B'
  AND sem.code = 'WS_2024_2025'
  AND e.status = 'FAILED';

-- ── SS 2024/2025 — all PASSED ────────────────────────────────────────────────

INSERT INTO enrollment (student_id, subject_id, semester_id, attempt_number, enrolled_at, status)
SELECT
    (SELECT id FROM "user" WHERE email = 'jozko.mrkvicka@stuba.sk'),
    s.id,
    sem.id,
    1,
    '2025-02-17 08:00:00+00',
    'PASSED'
FROM subject s, semester sem
WHERE s.code IN ('DSA_B','AJ2_B','ML1_B','OOP_B','TK_L','TZIV_B')
  AND sem.code = 'SS_2024_2025';

-- Index records for SS 2024/2025
INSERT INTO index_record (enrollment_id, recorded_by, final_mark, recorded_at, exam_date)
SELECT
    e.id,
    (SELECT id FROM "user" WHERE email = 'admin@fiit.stuba.sk'),
    t.mark,
    '2025-06-30 12:00:00+00',
    t.exam_date::DATE
FROM enrollment e
JOIN subject s ON s.id = e.subject_id
JOIN semester sem ON sem.id = e.semester_id
JOIN (VALUES
    ('DSA_B',  'A',    '2025-06-16'),
    ('AJ2_B',  'B',    '2025-06-10'),
    ('ML1_B',  'B',    '2025-06-18'),
    ('OOP_B',  'C',    '2025-06-20'),
    ('TK_L',   'PASS', '2025-05-30'),
    ('TZIV_B', 'C',    '2025-06-24')
) AS t(code, mark, exam_date) ON s.code = t.code
WHERE sem.code = 'SS_2024_2025'
  AND e.status = 'PASSED';

-- ── WS 2025/2026 — all PASSED ────────────────────────────────────────────────

INSERT INTO enrollment (student_id, subject_id, semester_id, attempt_number, enrolled_at, status)
SELECT
    (SELECT id FROM "user" WHERE email = 'jozko.mrkvicka@stuba.sk'),
    s.id,
    sem.id,
    1,
    '2025-09-22 08:00:00+00',
    'PASSED'
FROM subject s, semester sem
WHERE s.code IN ('UI_B','VPWA_B','PIKT_B','OS_B','TK_Z')
  AND sem.code = 'WS_2025_2026';

-- PKS_B attempt 1 — FAILED
INSERT INTO enrollment (student_id, subject_id, semester_id, attempt_number, enrolled_at, status)
SELECT
    (SELECT id FROM "user" WHERE email = 'jozko.mrkvicka@stuba.sk'),
    s.id,
    sem.id,
    1,
    '2025-09-22 08:00:00+00',
    'FAILED'
FROM subject s, semester sem
WHERE s.code = 'PKS_B' AND sem.code = 'WS_2025_2026';

-- PKS_B attempt 2 — PASSED
INSERT INTO enrollment (student_id, subject_id, semester_id, attempt_number, enrolled_at, status)
SELECT
    (SELECT id FROM "user" WHERE email = 'jozko.mrkvicka@stuba.sk'),
    s.id,
    sem.id,
    2,
    '2025-11-01 08:00:00+00',
    'PASSED'
FROM subject s, semester sem
WHERE s.code = 'PKS_B' AND sem.code = 'WS_2025_2026';

-- Index records for WS 2025/2026 (final passing attempts)
INSERT INTO index_record (enrollment_id, recorded_by, final_mark, recorded_at, exam_date)
SELECT
    e.id,
    (SELECT id FROM "user" WHERE email = 'admin@fiit.stuba.sk'),
    t.mark,
    '2026-01-30 12:00:00+00',
    t.exam_date::DATE
FROM enrollment e
JOIN subject s ON s.id = e.subject_id
JOIN semester sem ON sem.id = e.semester_id
JOIN (VALUES
    ('UI_B',   'C',    '2026-01-15'),
    ('PKS_B',  'C',    '2026-01-20'),
    ('VPWA_B', 'B',    '2026-01-12'),
    ('PIKT_B', 'A',    '2026-01-08'),
    ('OS_B',   'C',    '2026-01-22'),
    ('TK_Z',   'PASS', '2025-12-15')
) AS t(code, mark, exam_date) ON s.code = t.code
WHERE sem.code = 'WS_2025_2026'
  AND e.status = 'PASSED';

-- FX record for PKS_B failed attempt
INSERT INTO index_record (enrollment_id, recorded_by, final_mark, recorded_at, exam_date)
SELECT
    e.id,
    (SELECT id FROM "user" WHERE email = 'admin@fiit.stuba.sk'),
    'FX',
    '2025-11-01 12:00:00+00',
    '2025-11-01'
FROM enrollment e
JOIN subject s ON s.id = e.subject_id
JOIN semester sem ON sem.id = e.semester_id
WHERE s.code = 'PKS_B'
  AND sem.code = 'WS_2025_2026'
  AND e.status = 'FAILED';

-- ── SS 2025/2026 — all ACTIVE (no results yet) ───────────────────────────────

INSERT INTO enrollment (student_id, subject_id, semester_id, attempt_number, enrolled_at, status)
SELECT
    (SELECT id FROM "user" WHERE email = 'jozko.mrkvicka@stuba.sk'),
    s.id,
    sem.id,
    1,
    '2026-02-16 08:00:00+00',
    'ACTIVE'
FROM subject s, semester sem
WHERE s.code IN ('DBS_B','VAVA_B','DMBLOCK_B','WTECH_B','PSI_B','PAS_B')
  AND sem.code = 'SS_2025_2026';
