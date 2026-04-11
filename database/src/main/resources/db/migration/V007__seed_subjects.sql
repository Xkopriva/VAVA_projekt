-- ============================================================
-- Migration: V007__seed_subjects.sql
-- Description: Seed all 24 subjects from data-subjects.csv and
--              data-sylabus.csv, with SK/EN translations,
--              subject groups per semester, and group items.
-- ============================================================

-- ============================================================
-- SUBJECTS
-- Sources: data-subjects.csv + data-sylabus.csv (joined on code)
-- Subjects without syllabus data have NULL for grade stats.
-- ============================================================

INSERT INTO subject (
    code, external_id, faculty, credits,
    is_mandatory, is_profiled, completion_type,
    lecture_hrs_weekly, lab_hrs_weekly, seminar_hrs_weekly, project_hrs_weekly,
    language_of_instruction, assessment_breakdown,
    recommended_semester,
    total_assessed_students,
    grade_a_pct, grade_b_pct, grade_c_pct, grade_d_pct, grade_e_pct, grade_fx_pct,
    last_modified
) VALUES
-- ── WS 2024/2025 ── recommended_semester = 1 ────────────────────────────────
-- ADM_B: no syllabus data
('ADM_B',  NULL,   'FIIT', 7, TRUE,  TRUE,  'EXAM',          NULL, NULL, NULL, NULL, 'sk', NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
-- MA_B: no syllabus data
('MA_B',   NULL,   'FIIT', 7, TRUE,  TRUE,  'EXAM',          NULL, NULL, NULL, NULL, 'sk', NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
-- MIP_B: no syllabus data (GrdCD → GRADED_CREDIT)
('MIP_B',  NULL,   'FIIT', 6, TRUE,  TRUE,  'GRADED_CREDIT', NULL, NULL, NULL, NULL, 'sk', NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
-- AJ1_B: no syllabus data
('AJ1_B',  NULL,   'FIIT', 2, TRUE,  FALSE, 'EXAM',          NULL, NULL, NULL, NULL, 'sk', NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
-- PPI_B: no syllabus data
('PPI_B',  NULL,   'FIIT', 6, TRUE,  TRUE,  'EXAM',          NULL, NULL, NULL, NULL, 'sk', NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
-- PRPR_B: no syllabus data
('PRPR_B', NULL,   'FIIT', 6, TRUE,  TRUE,  'EXAM',          NULL, NULL, NULL, NULL, 'sk', NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

-- ── SS 2024/2025 ── recommended_semester = 2 ────────────────────────────────
-- DSA_B: full syllabus data
('DSA_B',  411220, 'FIIT', 6, TRUE,  TRUE,  'EXAM',          2,    2,    NULL, 1,    'sk or en', 'assignments 50% / final exam 50%', 2, 4723,  7.60, 14.40, 20.40, 20.40, 15.40, 21.80, '2025-02-18'),
-- AJ2_B: partial syllabus data (row truncated in source)
('AJ2_B',  NULL,   'FIIT', 2, TRUE,  FALSE, 'EXAM',          NULL, NULL, NULL, NULL, 'sk and en', 'individual presentation 30% / group presentations 30% / mid-term test 20% / exam 20%', 2, NULL, 33.00, 36.60, NULL, NULL, NULL, NULL, NULL),
-- ML1_B: no syllabus data
('ML1_B',  NULL,   'FIIT', 6, FALSE, FALSE, 'EXAM',          NULL, NULL, NULL, NULL, 'sk', NULL, 2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
-- OOP_B: no syllabus data
('OOP_B',  NULL,   'FIIT', 6, TRUE,  TRUE,  'EXAM',          NULL, NULL, NULL, NULL, 'sk', NULL, 2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
-- TK_L: no syllabus data (PassCD → PASS_CREDIT)
('TK_L',   NULL,   'FIIT', 1, TRUE,  FALSE, 'PASS_CREDIT',   NULL, NULL, NULL, NULL, 'sk', NULL, 2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
-- TZIV_B: no syllabus data
('TZIV_B', NULL,   'FIIT', 6, TRUE,  TRUE,  'EXAM',          NULL, NULL, NULL, NULL, 'sk', NULL, 2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

-- ── WS 2025/2026 ── recommended_semester = 3 ────────────────────────────────
-- UI_B: full syllabus data
('UI_B',   427207, 'FIIT', 6, TRUE,  TRUE,  'EXAM',          2,    2,    NULL, 1,    'sk or en', 'assignments 50% / final exam 50%',         3, 3440,  9.90, 21.50, 28.10, 21.90,  9.50,  9.10, '2025-11-04'),
-- PKS_B: full syllabus data
('PKS_B',  427200, 'FIIT', 6, TRUE,  TRUE,  'EXAM',          2,    2,    NULL, 1,    NULL,       'tests 17% / projects 23% / final test 60%', 3, 3876,  5.40, 12.80, 22.00, 22.10, 12.10, 25.60, '2025-09-05'),
-- VPWA_B: full syllabus data
('VPWA_B', 427210, 'FIIT', 6, FALSE, FALSE, 'EXAM',          2,    2,    NULL, 1,    NULL,       'semester assignments 44% / final exam 56%', 3, 271,  12.20, 29.20, 26.20, 19.20,  8.10,  5.10, '2026-02-24'),
-- PIKT_B: full syllabus data
('PIKT_B', 427201, 'FIIT', 5, TRUE,  FALSE, 'EXAM',          2,    NULL, NULL, NULL, 'sk',       'projects 20% / exam 80%',                  3, 3714, 11.10, 29.90, 37.40, 16.00,  3.40,  2.20, '2025-09-05'),
-- OS_B: full syllabus data
('OS_B',   427199, 'FIIT', 6, TRUE,  TRUE,  'EXAM',          2,    2,    NULL, 1,    'sk and en','semester work 40% / final exam 60%',        3, 4849,  1.90,  6.20, 15.90, 24.10, 24.00, 27.90, '2025-09-22'),
-- TK_Z: full syllabus data (PassCD → PASS_CREDIT; A-pct = 91.1 = PASS, FX = 8.9 = FAIL)
('TK_Z',   428691, 'FIIT', 1, TRUE,  FALSE, 'PASS_CREDIT',   NULL, NULL, 2,    NULL, NULL,       'credit (pass/fail)',                        3, 4999, 91.10,  NULL,  NULL,  NULL,  NULL,  8.90, '2025-03-05'),

-- ── SS 2025/2026 ── recommended_semester = 4 ────────────────────────────────
-- DBS_B: full syllabus data
('DBS_B',  427737, 'FIIT', 6, TRUE,  TRUE,  'EXAM',          2,    2,    NULL, 1,    'sk',       'assignments 20% / midterm tests 30% / final exam 50%', 4, 3837,  3.20, 13.30, 27.60, 25.10, 14.50, 16.30, '2026-02-17'),
-- VAVA_B: full syllabus data
('VAVA_B', 431368, 'FIIT', 6, FALSE, FALSE, 'EXAM',          2,    2,    NULL, 1,    NULL,       'test 30% / prototype implementation & documentation 70%', 4, 838,  28.40, 21.40, 20.00, 16.90, 9.90,  3.40, '2026-02-16'),
-- DMBLOCK_B: full syllabus data
('DMBLOCK_B', 427739, 'FIIT', 6, FALSE, FALSE, 'EXAM',       NULL, NULL, NULL, NULL, 'sk or en', 'projects 45% / assignments 15% / exam 40%', 4, 186,  9.10,  9.10, 20.40, 24.20, 17.70, 19.50, '2026-02-16'),
-- WTECH_B: full syllabus data
('WTECH_B',427761, 'FIIT', 6, FALSE, FALSE, 'EXAM',          2,    2,    NULL, 1,    'sk and en','semester assignments 56% / final exam 44%',  4, 555,  10.80, 18.40, 28.30, 25.90, 11.40,  5.20, '2026-02-24'),
-- PSI_B: full syllabus data
('PSI_B',  427752, 'FIIT', 6, TRUE,  TRUE,  'EXAM',          2,    2,    NULL, 1,    'sk and en','project/coursework 70% / exam 30%',           4, 3444,  5.40, 12.80, 27.70, 25.60, 16.50, 12.00, '2026-02-16'),
-- PAS_B: full syllabus data (language 'en' from subjects CSV; sylabus has no lang listed)
('PAS_B',  427749, 'FIIT', 6, FALSE, FALSE, 'EXAM',          2,    NULL, 2,    1,    'en',       'tests during teaching period 40% / final written exam 60%', 4, 4060, 5.80, 13.50, 20.70, 25.60, 17.80, 16.60, '2026-02-16');

-- ============================================================
-- SUBJECT TRANSLATIONS — Slovak (sk)
-- ============================================================

INSERT INTO subject_translation (subject_id, locale, name, description)
SELECT s.id, 'sk', t.name_sk, t.desc_sk
FROM subject s
JOIN (VALUES
    ('ADM_B',     'Algebra a diskrétna matematika',                     NULL),
    ('MA_B',      'Matematická analýza',                                NULL),
    ('MIP_B',     'Metódy inžinierskej praxe',                          NULL),
    ('AJ1_B',     'Anglický jazyk I',                                   NULL),
    ('PPI_B',     'Princípy počítačovej architektúry',                  NULL),
    ('PRPR_B',    'Procedurálne programovanie',                         NULL),
    ('DSA_B',     'Dátové štruktúry a algoritmy',                       NULL),
    ('AJ2_B',     'Anglický jazyk II',                                  NULL),
    ('ML1_B',     'Matematická logika I',                               NULL),
    ('OOP_B',     'Objektovo orientované programovanie',                NULL),
    ('TK_L',      'Telesná kultúra — leto',                             NULL),
    ('TZIV_B',    'Teoretické základy informatiky a výpočtov',          NULL),
    ('UI_B',      'Umelá inteligencia',                                 NULL),
    ('PKS_B',     'Počítačové a komunikačné siete',                     NULL),
    ('VPWA_B',    'Vývoj progresívnych webových aplikácií',             NULL),
    ('PIKT_B',    'Právo IKT',                                          NULL),
    ('OS_B',      'Operačné systémy',                                   NULL),
    ('TK_Z',      'Telesná kultúra — zima',                             NULL),
    ('DBS_B',     'Databázové systémy',                                 NULL),
    ('VAVA_B',    'Vývoj aplikácií s viacvrstvovou architektúrou',      NULL),
    ('DMBLOCK_B', 'Digitálne meny a blockchain',                        NULL),
    ('WTECH_B',   'Úvod do webových technológií',                       NULL),
    ('PSI_B',     'Princípy softvérového inžinierstva',                 NULL),
    ('PAS_B',     'Pravdepodobnosť a štatistika',                       NULL)
) AS t(code, name_sk, desc_sk) ON s.code = t.code;

-- ============================================================
-- SUBJECT TRANSLATIONS — English (en)
-- ============================================================

INSERT INTO subject_translation (subject_id, locale, name, description)
SELECT s.id, 'en', t.name_en, t.desc_en
FROM subject s
JOIN (VALUES
    ('ADM_B',     'Algebra and Discrete Mathematics',                       NULL),
    ('MA_B',      'Calculus',                                               NULL),
    ('MIP_B',     'Engineering Methods',                                    NULL),
    ('AJ1_B',     'English Language I',                                     NULL),
    ('PPI_B',     'Principles of Computer Engineering',                     NULL),
    ('PRPR_B',    'Procedural Programming',                                 NULL),
    ('DSA_B',     'Data Structures and Algorithms',                         NULL),
    ('AJ2_B',     'English Language II',                                    NULL),
    ('ML1_B',     'Mathematical Logic I',                                   NULL),
    ('OOP_B',     'Object-Oriented Programming',                            NULL),
    ('TK_L',      'Physical Education — Summer',                            NULL),
    ('TZIV_B',    'Theoretical Foundations of Information Sciences',        NULL),
    ('UI_B',      'Artificial Intelligence',                                NULL),
    ('PKS_B',     'Computer and Communication Networks',                    NULL),
    ('VPWA_B',    'Development of Progressive Web Applications',            NULL),
    ('PIKT_B',    'Law of Information and Communications Technologies',     NULL),
    ('OS_B',      'Operating Systems',                                      NULL),
    ('TK_Z',      'Physical Education — Winter',                            NULL),
    ('DBS_B',     'Database Systems',                                       NULL),
    ('VAVA_B',    'Development of Applications With Multilayer Architecture', NULL),
    ('DMBLOCK_B', 'Digital Currencies and Blockchain',                      NULL),
    ('WTECH_B',   'Introduction to Web Technologies',                       NULL),
    ('PSI_B',     'Principles of Software Engineering',                     NULL),
    ('PAS_B',     'Probability and Statistics',                             NULL)
) AS t(code, name_en, desc_en) ON s.code = t.code;

-- ============================================================
-- SUBJECT GROUPS  (2 groups per semester: compulsory + elective)
-- WS 2024/2025: all compulsory → 1 group
-- SS 2024/2025: 5 compulsory + 1 elective → 2 groups
-- WS 2025/2026: 5 compulsory + 1 elective → 2 groups
-- SS 2025/2026: 2 compulsory + 4 elective → 2 groups
-- ============================================================

INSERT INTO subject_group (semester_id, sort_order)
SELECT sem.id, g.sort_order
FROM semester sem
JOIN (VALUES
    ('WS_2024_2025', 1),
    ('SS_2024_2025', 1),
    ('SS_2024_2025', 2),
    ('WS_2025_2026', 1),
    ('WS_2025_2026', 2),
    ('SS_2025_2026', 1),
    ('SS_2025_2026', 2)
) AS g(sem_code, sort_order) ON sem.code = g.sem_code;

-- ── Group translations (SK) ──────────────────────────────────────────────────

INSERT INTO subject_group_translation (subject_group_id, locale, name)
SELECT sg.id, 'sk', t.name_sk
FROM subject_group sg
JOIN semester sem ON sem.id = sg.semester_id
JOIN (VALUES
    ('WS_2024_2025', 1, 'Povinné predmety'),
    ('SS_2024_2025', 1, 'Povinné predmety'),
    ('SS_2024_2025', 2, 'Povinne voliteľné predmety'),
    ('WS_2025_2026', 1, 'Povinné predmety'),
    ('WS_2025_2026', 2, 'Povinne voliteľné predmety'),
    ('SS_2025_2026', 1, 'Povinné predmety'),
    ('SS_2025_2026', 2, 'Povinne voliteľné predmety')
) AS t(sem_code, sort_order, name_sk)
    ON sem.code = t.sem_code AND sg.sort_order = t.sort_order;

-- ── Group translations (EN) ──────────────────────────────────────────────────

INSERT INTO subject_group_translation (subject_group_id, locale, name)
SELECT sg.id, 'en', t.name_en
FROM subject_group sg
JOIN semester sem ON sem.id = sg.semester_id
JOIN (VALUES
    ('WS_2024_2025', 1, 'Mandatory Subjects'),
    ('SS_2024_2025', 1, 'Mandatory Subjects'),
    ('SS_2024_2025', 2, 'Semi-elective Subjects'),
    ('WS_2025_2026', 1, 'Mandatory Subjects'),
    ('WS_2025_2026', 2, 'Semi-elective Subjects'),
    ('SS_2025_2026', 1, 'Mandatory Subjects'),
    ('SS_2025_2026', 2, 'Semi-elective Subjects')
) AS t(sem_code, sort_order, name_en)
    ON sem.code = t.sem_code AND sg.sort_order = t.sort_order;

-- ============================================================
-- SUBJECT GROUP ITEMS
-- ============================================================

-- WS 2024/2025 — mandatory group (sort_order=1), all compulsory
INSERT INTO subject_group_item (subject_group_id, subject_id, is_mandatory)
SELECT sg.id, s.id, TRUE
FROM subject_group sg
JOIN semester sem ON sem.id = sg.semester_id AND sem.code = 'WS_2024_2025' AND sg.sort_order = 1
JOIN subject s ON s.code IN ('ADM_B','MA_B','MIP_B','AJ1_B','PPI_B','PRPR_B');

-- SS 2024/2025 — mandatory group (sort_order=1)
INSERT INTO subject_group_item (subject_group_id, subject_id, is_mandatory)
SELECT sg.id, s.id, TRUE
FROM subject_group sg
JOIN semester sem ON sem.id = sg.semester_id AND sem.code = 'SS_2024_2025' AND sg.sort_order = 1
JOIN subject s ON s.code IN ('DSA_B','AJ2_B','OOP_B','TK_L','TZIV_B');

-- SS 2024/2025 — semi-elective group (sort_order=2)
INSERT INTO subject_group_item (subject_group_id, subject_id, is_mandatory)
SELECT sg.id, s.id, FALSE
FROM subject_group sg
JOIN semester sem ON sem.id = sg.semester_id AND sem.code = 'SS_2024_2025' AND sg.sort_order = 2
JOIN subject s ON s.code IN ('ML1_B');

-- WS 2025/2026 — mandatory group (sort_order=1)
INSERT INTO subject_group_item (subject_group_id, subject_id, is_mandatory)
SELECT sg.id, s.id, TRUE
FROM subject_group sg
JOIN semester sem ON sem.id = sg.semester_id AND sem.code = 'WS_2025_2026' AND sg.sort_order = 1
JOIN subject s ON s.code IN ('UI_B','PKS_B','PIKT_B','OS_B','TK_Z');

-- WS 2025/2026 — semi-elective group (sort_order=2)
INSERT INTO subject_group_item (subject_group_id, subject_id, is_mandatory)
SELECT sg.id, s.id, FALSE
FROM subject_group sg
JOIN semester sem ON sem.id = sg.semester_id AND sem.code = 'WS_2025_2026' AND sg.sort_order = 2
JOIN subject s ON s.code IN ('VPWA_B');

-- SS 2025/2026 — mandatory group (sort_order=1)
INSERT INTO subject_group_item (subject_group_id, subject_id, is_mandatory)
SELECT sg.id, s.id, TRUE
FROM subject_group sg
JOIN semester sem ON sem.id = sg.semester_id AND sem.code = 'SS_2025_2026' AND sg.sort_order = 1
JOIN subject s ON s.code IN ('DBS_B','PSI_B');

-- SS 2025/2026 — semi-elective group (sort_order=2)
INSERT INTO subject_group_item (subject_group_id, subject_id, is_mandatory)
SELECT sg.id, s.id, FALSE
FROM subject_group sg
JOIN semester sem ON sem.id = sg.semester_id AND sem.code = 'SS_2025_2026' AND sg.sort_order = 2
JOIN subject s ON s.code IN ('VAVA_B','DMBLOCK_B','WTECH_B','PAS_B');
