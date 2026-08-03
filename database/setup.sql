CREATE DATABASE IF NOT EXISTS smartstudy CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smartstudy;

CREATE TABLE IF NOT EXISTS students (
 student_id INT AUTO_INCREMENT PRIMARY KEY,
 full_name VARCHAR(120) NOT NULL,
 email VARCHAR(160) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL,
 available_hours DECIMAL(5,2) NOT NULL DEFAULT 10,
 is_active BOOLEAN NOT NULL DEFAULT TRUE,
 email_notifications BOOLEAN NOT NULL DEFAULT TRUE,
 in_app_notifications BOOLEAN NOT NULL DEFAULT TRUE,
 weekly_summary BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS administrators (
 admin_id INT AUTO_INCREMENT PRIMARY KEY,
 full_name VARCHAR(120) NOT NULL,
 email VARCHAR(160) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL
);
CREATE TABLE IF NOT EXISTS courses (
 course_id INT AUTO_INCREMENT PRIMARY KEY,
 course_code VARCHAR(30) NOT NULL,
 course_name VARCHAR(160) NOT NULL,
 instructor VARCHAR(120), semester VARCHAR(60),
 student_id INT NOT NULL,
 CONSTRAINT uq_student_course UNIQUE(student_id,course_code),
 CONSTRAINT fk_course_student FOREIGN KEY(student_id) REFERENCES students(student_id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS tasks (
 task_id INT AUTO_INCREMENT PRIMARY KEY,
 title VARCHAR(200) NOT NULL,
 due_date DATETIME NOT NULL,
 grade_weight DECIMAL(5,2) NOT NULL DEFAULT 0,
 estimated_hours DECIMAL(5,2) NOT NULL DEFAULT 1,
 status ENUM('PENDING','IN_PROGRESS','COMPLETED') NOT NULL DEFAULT 'PENDING',
 task_type ENUM('ASSIGNMENT','EXAM','QUIZ') NOT NULL,
 submit_type VARCHAR(80), allow_late BOOLEAN, location VARCHAR(160), duration_min INT, is_online BOOLEAN, attempts INT,
 course_id INT NOT NULL,
 CONSTRAINT fk_task_course FOREIGN KEY(course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
 INDEX idx_task_due(due_date), INDEX idx_task_status(status)
);
CREATE TABLE IF NOT EXISTS syllabi (
 syllabus_id INT AUTO_INCREMENT PRIMARY KEY,
 file_name VARCHAR(255) NOT NULL, file_format VARCHAR(20), upload_date DATETIME NOT NULL,
 stored_path VARCHAR(500) NOT NULL, course_id INT NOT NULL UNIQUE,
 CONSTRAINT fk_syllabus_course FOREIGN KEY(course_id) REFERENCES courses(course_id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS extracted_items (
 item_id INT AUTO_INCREMENT PRIMARY KEY,
 title VARCHAR(200) NOT NULL, item_type ENUM('ASSIGNMENT','EXAM','QUIZ') NOT NULL,
 due_date DATETIME, extracted_date DATETIME NOT NULL, weight DECIMAL(5,2) NOT NULL DEFAULT 0,
 confirmed BOOLEAN NOT NULL DEFAULT FALSE, syllabus_id INT NOT NULL, task_id INT UNIQUE NULL,
 CONSTRAINT fk_item_syllabus FOREIGN KEY(syllabus_id) REFERENCES syllabi(syllabus_id) ON DELETE CASCADE,
 CONSTRAINT fk_item_task FOREIGN KEY(task_id) REFERENCES tasks(task_id) ON DELETE SET NULL
);
CREATE TABLE IF NOT EXISTS schedules (
 schedule_id INT AUTO_INCREMENT PRIMARY KEY, generated_on DATETIME NOT NULL,
 week_start DATE NOT NULL, student_id INT NOT NULL,
 CONSTRAINT uq_student_week UNIQUE(student_id,week_start),
 CONSTRAINT fk_schedule_student FOREIGN KEY(student_id) REFERENCES students(student_id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS study_sessions (
 session_id INT AUTO_INCREMENT PRIMARY KEY, start_time DATETIME NOT NULL, end_time DATETIME NOT NULL,
 duration_hours DECIMAL(5,2) NOT NULL, schedule_id INT NOT NULL, task_id INT NULL,
 CONSTRAINT fk_session_schedule FOREIGN KEY(schedule_id) REFERENCES schedules(schedule_id) ON DELETE CASCADE,
 CONSTRAINT fk_session_task FOREIGN KEY(task_id) REFERENCES tasks(task_id) ON DELETE SET NULL
);
CREATE TABLE IF NOT EXISTS notifications (
 notification_id INT AUTO_INCREMENT PRIMARY KEY, message VARCHAR(500) NOT NULL, send_at DATETIME NOT NULL,
 channel VARCHAR(40) NOT NULL DEFAULT 'IN_APP', sent BOOLEAN NOT NULL DEFAULT FALSE, student_id INT NOT NULL,
 CONSTRAINT fk_notification_student FOREIGN KEY(student_id) REFERENCES students(student_id) ON DELETE CASCADE,
 INDEX idx_notification_due(send_at,sent)
);
USE smartstudy;
-- Default administrator credentials: admin@smartstudy.local / Admin123!
INSERT INTO administrators(full_name,email,password_hash)
VALUES ('System Administrator','admin@smartstudy.local','$2a$10$mU7xU2VOkfI7SDW845LaeO5/d1AKC.RxjCQGFDSwULHVDr.TD5MTq')
ON DUPLICATE KEY UPDATE full_name='System Administrator';
