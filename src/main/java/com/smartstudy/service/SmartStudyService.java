package com.smartstudy.service;

import com.smartstudy.config.AppConfig;
import com.smartstudy.dao.CourseDao;
import com.smartstudy.dao.NotificationDao;
import com.smartstudy.dao.ScheduleDao;
import com.smartstudy.dao.StudentDao;
import com.smartstudy.dao.SyllabusDao;
import com.smartstudy.dao.TaskDao;
import com.smartstudy.model.AcademicTask;
import com.smartstudy.model.Course;
import com.smartstudy.model.ExtractedItem;
import com.smartstudy.model.Notification;
import com.smartstudy.model.Schedule;
import com.smartstudy.model.Student;
import com.smartstudy.model.StudySession;
import com.smartstudy.model.Syllabus;
import com.smartstudy.model.TaskStatus;
import com.smartstudy.model.TaskType;
import com.smartstudy.util.Validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SmartStudyService {
    private static final long MAX_SYLLABUS_BYTES = 15L * 1024 * 1024;
    private static final DateTimeFormatter REMINDER_DATE =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy 'at' h:mm a", Locale.ENGLISH);

    public final StudentDao students = new StudentDao();
    public final CourseDao courses = new CourseDao();
    public final TaskDao tasks = new TaskDao();
    public final SyllabusDao syllabi = new SyllabusDao();
    public final ScheduleDao schedules = new ScheduleDao();
    public final NotificationDao notifications = new NotificationDao();

    private final ScheduleGenerator generator = new ScheduleGenerator();

    public Course saveCourse(Course course) throws Exception {
        Validation.requireText(course.courseCode(), "Course code");
        Validation.requireText(course.courseName(), "Course name");
        Validation.requireMaxLength(course.courseCode(), 30, "Course code");
        Validation.requireMaxLength(course.courseName(), 160, "Course name");
        Validation.requireMaxLength(course.instructor(), 120, "Instructor");
        Validation.requireMaxLength(course.semester(), 60, "Semester");

        Course normalized = new Course(
                course.courseId(),
                course.courseCode().trim().toUpperCase(Locale.ROOT),
                course.courseName().trim(),
                clean(course.instructor()),
                clean(course.semester()),
                course.studentId()
        );
        if (normalized.courseId() == 0) {
            return courses.insert(normalized);
        }
        courses.update(normalized);
        return normalized;
    }

    public AcademicTask saveTask(AcademicTask task) throws Exception {
        Validation.requireText(task.title(), "Task title");
        Validation.requireMaxLength(task.title(), 180, "Task title");
        Validation.requirePercentage(task.gradeWeight(), "Grade weight");
        Validation.requirePositive(task.estimatedHours(), "Estimated hours");
        if (task.dueDate() == null) {
            throw new IllegalArgumentException("Due date is required.");
        }
        if (task.taskType() == null || task.status() == null) {
            throw new IllegalArgumentException("Task type and status are required.");
        }

        AcademicTask normalized = new AcademicTask(
                task.taskId(),
                task.title().trim(),
                task.dueDate(),
                task.gradeWeight(),
                task.estimatedHours(),
                task.status(),
                task.taskType(),
                clean(task.submitType()),
                task.allowLate(),
                clean(task.location()),
                task.durationMin(),
                task.online(),
                task.attempts(),
                task.courseId()
        );

        AcademicTask saved;
        if (normalized.taskId() == 0) {
            saved = tasks.insert(normalized);
        } else {
            tasks.update(normalized);
            saved = normalized;
        }
        regenerateCurrentWeek(SessionManager.get().requireActive().id());
        return saved;
    }

    public ScheduleGenerator.Result regenerateCurrentWeek(int studentId) throws Exception {
        Student student = students.findById(studentId).orElseThrow();
        LocalDate today = LocalDate.now();
        // Use a rolling seven-day plan beginning today instead of the calendar
        // Monday-Sunday week. This prevents a Sunday-evening regeneration from
        // producing only one short block and incorrectly reporting that the rest
        // of the student's weekly hours are unavailable.
        LocalDate planStart = today;
        List<AcademicTask> allTasks = tasks.findByStudent(studentId);
        ScheduleGenerator.Result result = generator.generate(allTasks, student.availableHours(), planStart);
        schedules.replace(studentId, planStart, result.sessions());
        refreshAutomaticNotifications(student, allTasks, result.sessions());
        return result;
    }

    private void refreshAutomaticNotifications(Student student,
                                               List<AcademicTask> tasksForStudent,
                                               List<StudySession> sessions) throws Exception {
        List<Notification> automatic = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if (student.inAppNotifications()) {
            for (StudySession session : sessions) {
                LocalDateTime sendAt = session.startTime().minusMinutes(30);
                if (sendAt.isBefore(now)) {
                    sendAt = now.plusMinutes(1);
                }
                String message = "[AUTO] Study session: " + session.taskTitle()
                        + " — " + session.startTime().format(REMINDER_DATE);
                automatic.add(new Notification(0, message, sendAt, "IN_APP", false, student.studentId()));
            }

            for (AcademicTask task : tasksForStudent) {
                if (task.status() == TaskStatus.COMPLETED || task.dueDate() == null || !task.dueDate().isAfter(now)) {
                    continue;
                }
                LocalDateTime sendAt = task.dueDate().minusHours(24);
                if (sendAt.isBefore(now)) {
                    sendAt = now.plusMinutes(2);
                }
                String message = "[AUTO] Deadline: " + task.title()
                        + " — " + task.dueDate().format(REMINDER_DATE);
                automatic.add(new Notification(0, message, sendAt, "IN_APP", false, student.studentId()));
            }
        }
        notifications.replaceAutomatic(student.studentId(), automatic);
    }

    public List<ExtractedItem> uploadAndExtract(Course course, Path source) throws Exception {
        if (source == null || !Files.isRegularFile(source)) {
            throw new IllegalArgumentException("Choose a valid syllabus file.");
        }
        String extension = extension(source);
        if (!extension.equals("PDF") && !extension.equals("DOCX")) {
            throw new IllegalArgumentException("Only PDF and DOCX files are supported.");
        }
        if (Files.size(source) > MAX_SYLLABUS_BYTES) {
            throw new IllegalArgumentException("The syllabus file must be 15 MB or smaller.");
        }

        Path directory = AppConfig.syllabusStorage();
        Files.createDirectories(directory);
        String safeName = course.courseId() + "_" + source.getFileName().toString()
                .replaceAll("[^A-Za-z0-9._-]", "_");
        Path target = directory.resolve(safeName);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        List<ExtractedItem> parsedItems = new SyllabusParser().parse(target);
        int syllabusId = syllabi.upsert(new Syllabus(
                0,
                source.getFileName().toString(),
                extension,
                LocalDateTime.now(),
                target.toString(),
                course.courseId()
        ));

        List<ExtractedItem> normalized = parsedItems.stream()
                .map(item -> new ExtractedItem(
                        0,
                        item.title(),
                        item.itemType(),
                        item.dueDate(),
                        item.extractedDate(),
                        item.weight(),
                        false,
                        syllabusId,
                        null
                ))
                .toList();

        syllabi.replaceExtractedItems(syllabusId, normalized);
        return syllabi.findPendingItems(syllabusId);
    }

    public AcademicTask confirmItem(ExtractedItem item, int courseId) throws Exception {
        if (item.confirmed() || item.taskId() != null) {
            throw new IllegalArgumentException("This extracted item has already been confirmed.");
        }
        Validation.requireText(item.title(), "Extracted item title");
        Validation.requireMaxLength(item.title(), 180, "Extracted item title");
        Validation.requirePercentage(item.weight(), "Weight");
        if (item.dueDate() == null) {
            throw new IllegalArgumentException("Enter a due date before confirming this item.");
        }
        if (tasks.existsEquivalent(courseId, item.title().trim(), item.dueDate())) {
            throw new IllegalArgumentException("A task with the same title and deadline already exists for this course.");
        }

        double estimatedHours = switch (item.itemType()) {
            case EXAM -> Math.max(2.0, item.weight() / 8.0);
            case QUIZ -> Math.max(0.5, item.weight() / 15.0);
            case ASSIGNMENT -> Math.max(1.0, item.weight() / 10.0);
        };
        estimatedHours = Math.min(15.0, estimatedHours);

        AcademicTask task = new AcademicTask(
                0,
                item.title().trim(),
                item.dueDate(),
                item.weight(),
                estimatedHours,
                TaskStatus.PENDING,
                item.itemType(),
                null,
                null,
                null,
                null,
                null,
                null,
                courseId
        );
        AcademicTask saved = tasks.insert(task);
        syllabi.confirmAndLink(item.itemId(), saved.taskId());
        regenerateCurrentWeek(SessionManager.get().requireActive().id());
        return saved;
    }

    private String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toUpperCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
