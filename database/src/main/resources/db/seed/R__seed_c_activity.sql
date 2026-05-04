-- ============================================================
-- Seed: R__seed_c_activity.sql
-- Description: Seed event, event_translation, task, task_submission,
--              mark, and notification tables with sample data.
-- ============================================================

-- ============================================================
-- EVENT (10 entries)
-- ============================================================

INSERT INTO event (
  subject_id, type, week_number, room, scheduled_at,
  duration_minutes, is_published, created_by
)
SELECT s.id, v.type, v.week_number, v.room, v.scheduled_at,
     v.duration_minutes, v.is_published, u.id
FROM (VALUES
  ('VAVA_B',    'PREDNASKA',   3,  'A-301', TIMESTAMPTZ '2026-03-02 09:00:00+00', 90,  TRUE),
  ('VAVA_B',    'CVICENIE',    4,  'B-201', TIMESTAMPTZ '2026-03-09 10:00:00+00', 90,  TRUE),
  ('VAVA_B',    'ODOVZDANIE',  6,  'B-201', TIMESTAMPTZ '2026-03-23 23:59:00+00', 15,  TRUE),
  ('DBS_B',     'PREDNASKA',   2,  'D-105', TIMESTAMPTZ '2026-02-23 08:00:00+00', 90,  TRUE),
  ('DBS_B',     'ZAPOCET',     8,  'D-105', TIMESTAMPTZ '2026-04-06 08:00:00+00', 60,  TRUE),
  ('DBS_B',     'ODOVZDANIE',  4,  'D-105', TIMESTAMPTZ '2026-03-08 23:59:00+00', 15,  TRUE),
  ('PSI_B',     'PREDNASKA',   5,  'C-110', TIMESTAMPTZ '2026-03-16 12:00:00+00', 90,  TRUE),
  ('WTECH_B',   'CVICENIE',    6,  'E-210', TIMESTAMPTZ '2026-03-23 14:00:00+00', 90,  TRUE),
  ('DMBLOCK_B', 'PISOMKA',     7,  'F-101', TIMESTAMPTZ '2026-03-30 09:00:00+00', 60,  FALSE),
  ('PAS_B',     'EXAM',        12, 'A-105', TIMESTAMPTZ '2026-05-11 09:00:00+00', 120, FALSE)
) AS v(subject_code, type, week_number, room, scheduled_at, duration_minutes, is_published)
JOIN subject s ON s.code = v.subject_code
JOIN "user" u ON u.email = 'jan.novak@stuba.sk';

-- ============================================================
-- EVENT TRANSLATION (20 entries)
-- ============================================================

INSERT INTO event_translation (event_id, locale, title, description)
SELECT e.id, 'sk', v.title, v.description
FROM (VALUES
  ('VAVA_B',    'PREDNASKA',  3,  'Prednaska 3',      'Uvod do viacvrstvovej architektury'),
  ('VAVA_B',    'CVICENIE',   4,  'Cvicenie 4',       'Priprava na prototyp'),
  ('VAVA_B',    'ODOVZDANIE', 6,  'Odovzdanie',       'Terminy odovzdania prototypu'),
  ('DBS_B',     'PREDNASKA',  2,  'Prednaska 2',      'Normalizacia a indexy'),
  ('DBS_B',     'ZAPOCET',    8,  'Zapocet',          'Prakticky test z SQL'),
  ('DBS_B',     'ODOVZDANIE', 4,  'Odovzdanie',       'SQL zadanie 1'),
  ('PSI_B',     'PREDNASKA',  5,  'Prednaska 5',      'Analyticke techniky'),
  ('WTECH_B',   'CVICENIE',   6,  'Cvicenie 6',       'HTML a CSS layout'),
  ('DMBLOCK_B', 'PISOMKA',    7,  'Pisomka',          'Kontrola pojmov a terminov'),
  ('PAS_B',     'EXAM',       12, 'Skuska',           'Finalna skuska')
) AS v(subject_code, type, week_number, title, description)
JOIN subject s ON s.code = v.subject_code
JOIN event e ON e.subject_id = s.id AND e.type = v.type AND e.week_number = v.week_number;

INSERT INTO event_translation (event_id, locale, title, description)
SELECT e.id, 'en', v.title, v.description
FROM (VALUES
  ('VAVA_B',    'PREDNASKA',  3,  'Lecture 3',        'Intro to multilayer architecture'),
  ('VAVA_B',    'CVICENIE',   4,  'Lab 4',            'Prototype preparation'),
  ('VAVA_B',    'ODOVZDANIE', 6,  'Submission',       'Prototype submission deadline'),
  ('DBS_B',     'PREDNASKA',  2,  'Lecture 2',        'Normalization and indexes'),
  ('DBS_B',     'ZAPOCET',    8,  'Credit test',      'Practical SQL test'),
  ('DBS_B',     'ODOVZDANIE', 4,  'Submission',       'SQL assignment 1'),
  ('PSI_B',     'PREDNASKA',  5,  'Lecture 5',        'Analysis techniques'),
  ('WTECH_B',   'CVICENIE',   6,  'Lab 6',            'HTML and CSS layout'),
  ('DMBLOCK_B', 'PISOMKA',    7,  'Written test',     'Terminology check'),
  ('PAS_B',     'EXAM',       12, 'Final exam',       'Final exam session')
) AS v(subject_code, type, week_number, title, description)
JOIN subject s ON s.code = v.subject_code
JOIN event e ON e.subject_id = s.id AND e.type = v.type AND e.week_number = v.week_number;

-- ============================================================
-- TASK (7 entries)
-- ============================================================

INSERT INTO task (
  event_id, subject_id, title, description, due_at,
  max_points, is_published, created_by
)
SELECT e.id, s.id, v.title, v.description, v.due_at,
     v.max_points, v.is_published, u.id
FROM (VALUES
  ('VAVA_B',    'ODOVZDANIE', 6,  'Prototype milestone', 'Submit the first working prototype and short report.', TIMESTAMPTZ '2026-03-15 23:59:00+00', 20, TRUE),
  ('VAVA_B',    NULL,        NULL, 'Final report',       'Architecture report and demo notes.',                  TIMESTAMPTZ '2026-05-05 23:59:00+00', 30, TRUE),
  ('DBS_B',     'ODOVZDANIE', 4,  'SQL assignment 1',   'DDL + indexes for the given domain.',                  TIMESTAMPTZ '2026-03-08 23:59:00+00', 15, TRUE),
  ('PSI_B',     NULL,        NULL, 'Requirements draft', 'Initial functional and non-functional requirements.',  TIMESTAMPTZ '2026-03-20 23:59:00+00', 10, TRUE),
  ('WTECH_B',   'CVICENIE',   6,  'Landing page',       'Single-page layout with responsive breakpoints.',     TIMESTAMPTZ '2026-03-25 23:59:00+00', 15, TRUE),
  ('DMBLOCK_B', NULL,        NULL, 'Paper critique',    'Short review of a recent blockchain paper.',           TIMESTAMPTZ '2026-04-02 23:59:00+00', 10, TRUE),
  ('PAS_B',     NULL,        NULL, 'Problem set 1',     'Probability and statistics exercises.',                TIMESTAMPTZ '2026-03-18 23:59:00+00', 12, TRUE)
) AS v(subject_code, event_type, week_number, title, description, due_at, max_points, is_published)
JOIN subject s ON s.code = v.subject_code
LEFT JOIN event e ON e.subject_id = s.id AND e.type = v.event_type AND e.week_number = v.week_number
JOIN "user" u ON u.email = 'jan.novak@stuba.sk';

-- ============================================================
-- TASK SUBMISSION (6 entries)
-- ============================================================

INSERT INTO task_submission (
  task_id, student_id, submitted_at, content, status
)
SELECT t.id, u.id, v.submitted_at, v.content, v.status
FROM (VALUES
  ('VAVA_B',    'Prototype milestone', TIMESTAMPTZ '2026-03-14 18:30:00+00', 'Prototype zip uploaded to shared drive.', 'SUBMITTED'),
  ('DBS_B',     'SQL assignment 1',    TIMESTAMPTZ '2026-03-07 20:10:00+00', 'DDL scripts and indexes attached.',       'SUBMITTED'),
  ('PSI_B',     'Requirements draft', TIMESTAMPTZ '2026-03-19 21:00:00+00', 'Draft uploaded as PDF.',                  'GRADED'),
  ('WTECH_B',   'Landing page',       TIMESTAMPTZ '2026-03-26 00:05:00+00', 'Late upload after deadline.',             'LATE'),
  ('DMBLOCK_B', 'Paper critique',     TIMESTAMPTZ '2026-04-01 19:00:00+00', 'Critique document in PDF.',               'SUBMITTED'),
  ('PAS_B',     'Problem set 1',      NULL,                                       'Not submitted yet.',                      'PENDING')
) AS v(subject_code, task_title, submitted_at, content, status)
JOIN task t ON t.title = v.task_title
JOIN subject s ON s.id = t.subject_id AND s.code = v.subject_code
JOIN "user" u ON u.email = 'jozko.mrkvicka@stuba.sk';

-- ============================================================
-- MARK (6 entries)
-- ============================================================

INSERT INTO mark (
  enrollment_id, event_id, task_submission_id,
  title, points, max_points, given_by, given_at, notes
)
SELECT e.id, t.event_id, ts.id,
       v.task_title, v.points, v.max_points, teacher.id, v.given_at, v.notes
FROM (VALUES
  ('VAVA_B',    'Prototype milestone', 17.5, 20, TIMESTAMPTZ '2026-03-16 10:00:00+00', 'Solid architecture and docs.'),
  ('DBS_B',     'SQL assignment 1',    13.0, 15, TIMESTAMPTZ '2026-03-10 09:15:00+00', 'Good indexes, minor naming issues.'),
  ('PSI_B',     'Requirements draft',  8.5, 10, TIMESTAMPTZ '2026-03-22 11:00:00+00', 'Clear scope and priorities.'),
  ('WTECH_B',   'Landing page',        12.0, 15, TIMESTAMPTZ '2026-03-27 08:30:00+00', 'Late but acceptable layout.'),
  ('DMBLOCK_B', 'Paper critique',      9.0, 10, TIMESTAMPTZ '2026-04-03 10:30:00+00', 'Concise and well-argued.' )
) AS v(subject_code, task_title, points, max_points, given_at, notes)
JOIN subject s ON s.code = v.subject_code
JOIN enrollment e ON e.subject_id = s.id
JOIN "user" student ON student.id = e.student_id AND student.email = 'jozko.mrkvicka@stuba.sk'
JOIN "user" teacher ON teacher.email = 'jan.novak@stuba.sk'
JOIN task t ON t.subject_id = s.id AND t.title = v.task_title
JOIN task_submission ts ON ts.task_id = t.id AND ts.student_id = student.id;

INSERT INTO mark (
  enrollment_id, event_id, task_submission_id,
  title, points, max_points, given_by, given_at, notes
)
SELECT e.id, ev.id, NULL,
    'Midterm test', 18.0, 25, teacher.id, TIMESTAMPTZ '2026-03-30 10:10:00+00', 'Average result.'
FROM enrollment e
JOIN subject s ON s.id = e.subject_id AND s.code = 'DMBLOCK_B'
JOIN "user" student ON student.id = e.student_id AND student.email = 'jozko.mrkvicka@stuba.sk'
JOIN "user" teacher ON teacher.email = 'jan.novak@stuba.sk'
JOIN event ev ON ev.subject_id = s.id AND ev.type = 'PISOMKA' AND ev.week_number = 7;

-- ============================================================
-- NOTIFICATIONS (9 entries)
-- ============================================================

INSERT INTO notification (
  recipient_id, sender_id, type, title, message,
  related_subject_id, related_task_id, created_at
)
SELECT student.id, teacher.id, 'TASK_DUE', v.title, v.message,
     s.id, t.id, v.created_at
FROM (VALUES
  ('VAVA_B',  'Prototype milestone', 'Task due soon',   'Prototype milestone is due on 2026-03-15.', TIMESTAMPTZ '2026-03-10 08:00:00+00'),
  ('DBS_B',   'SQL assignment 1',    'Task due soon',   'SQL assignment 1 is due on 2026-03-08.',    TIMESTAMPTZ '2026-03-05 08:00:00+00'),
  ('PSI_B',   'Requirements draft',  'Task due soon',   'Requirements draft is due on 2026-03-20.',  TIMESTAMPTZ '2026-03-15 08:00:00+00'),
  ('WTECH_B', 'Landing page',        'Task due soon',   'Landing page is due on 2026-03-25.',        TIMESTAMPTZ '2026-03-20 08:00:00+00')
) AS v(subject_code, task_title, title, message, created_at)
JOIN "user" student ON student.email = 'jozko.mrkvicka@stuba.sk'
JOIN "user" teacher ON teacher.email = 'jan.novak@stuba.sk'
JOIN subject s ON s.code = v.subject_code
JOIN task t ON t.subject_id = s.id AND t.title = v.task_title;

INSERT INTO notification (
  recipient_id, sender_id, type, title, message,
  related_mark_id, related_subject_id, related_task_id, created_at
)
SELECT student.id, teacher.id, 'MARK_ADDED', v.title, v.message,
     m.id, s.id, t.id, v.created_at
FROM (VALUES
  ('VAVA_B',  'Prototype milestone', 'Mark added', 'Your prototype milestone was graded.', TIMESTAMPTZ '2026-03-16 10:05:00+00'),
  ('DBS_B',   'SQL assignment 1',    'Mark added', 'SQL assignment 1 was graded.',         TIMESTAMPTZ '2026-03-10 09:20:00+00'),
  ('PSI_B',   'Requirements draft',  'Mark added', 'Requirements draft was graded.',       TIMESTAMPTZ '2026-03-22 11:05:00+00')
) AS v(subject_code, task_title, title, message, created_at)
JOIN "user" student ON student.email = 'jozko.mrkvicka@stuba.sk'
JOIN "user" teacher ON teacher.email = 'jan.novak@stuba.sk'
JOIN subject s ON s.code = v.subject_code
JOIN task t ON t.subject_id = s.id AND t.title = v.task_title
JOIN enrollment e ON e.subject_id = s.id AND e.student_id = student.id
JOIN mark m ON m.enrollment_id = e.id AND m.title = v.task_title;

INSERT INTO notification (
  recipient_id, sender_id, type, title, message,
  related_subject_id, created_at
)
SELECT student.id, teacher.id, 'EXAM_SCHEDULED',
     'Exam scheduled', 'Final exam is scheduled for 2026-05-11.',
    s.id, TIMESTAMPTZ '2026-04-20 08:00:00+00'
FROM "user" student
JOIN "user" teacher ON teacher.email = 'jan.novak@stuba.sk'
JOIN subject s ON s.code = 'PAS_B'
WHERE student.email = 'jozko.mrkvicka@stuba.sk';

INSERT INTO notification (
  recipient_id, sender_id, type, title, message,
  related_subject_id, created_at
)
SELECT student.id, teacher.id, 'ANNOUNCEMENT',
     'Project scope', 'Project scope updated, see new requirements.',
    s.id, TIMESTAMPTZ '2026-03-01 08:00:00+00'
FROM "user" student
JOIN "user" teacher ON teacher.email = 'jan.novak@stuba.sk'
JOIN subject s ON s.code = 'VAVA_B'
WHERE student.email = 'jozko.mrkvicka@stuba.sk';
