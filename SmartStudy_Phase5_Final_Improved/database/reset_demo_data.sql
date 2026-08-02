-- OPTIONAL: removes student/demo data but keeps the default administrator account.
-- Use this only when you intentionally want a clean demonstration database.
USE smartstudy;
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE notifications;
TRUNCATE TABLE study_sessions;
TRUNCATE TABLE schedules;
TRUNCATE TABLE extracted_items;
TRUNCATE TABLE syllabi;
TRUNCATE TABLE tasks;
TRUNCATE TABLE courses;
TRUNCATE TABLE students;
SET FOREIGN_KEY_CHECKS = 1;
