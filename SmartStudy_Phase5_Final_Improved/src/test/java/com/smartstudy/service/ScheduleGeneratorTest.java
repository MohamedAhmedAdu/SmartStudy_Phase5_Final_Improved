package com.smartstudy.service;

import com.smartstudy.model.AcademicTask;
import com.smartstudy.model.TaskStatus;
import com.smartstudy.model.TaskType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleGeneratorTest {
    @Test
    void prioritizesHigherWeightAndNearerDeadline() {
        ScheduleGenerator generator = new ScheduleGenerator();
        LocalDateTime now = LocalDateTime.of(2026, 8, 2, 10, 0);
        AcademicTask high = task(1, "High priority", now.plusDays(2), 40, 4, TaskStatus.PENDING, TaskType.EXAM);
        AcademicTask low = task(2, "Low priority", now.plusDays(20), 10, 2, TaskStatus.PENDING, TaskType.ASSIGNMENT);

        var result = generator.generate(List.of(low, high), 8, LocalDate.of(2026, 8, 2), now);

        assertFalse(result.sessions().isEmpty());
        assertEquals("High priority", result.sessions().getFirst().taskTitle());
    }

    @Test
    void excludesCompletedTasks() {
        ScheduleGenerator generator = new ScheduleGenerator();
        LocalDateTime now = LocalDateTime.of(2026, 8, 2, 10, 0);
        AcademicTask done = task(1, "Done", now.plusDays(2), 30, 2, TaskStatus.COMPLETED, TaskType.QUIZ);

        assertTrue(generator.generate(List.of(done), 5, LocalDate.of(2026, 8, 2), now).sessions().isEmpty());
    }

    @Test
    void neverCreatesSessionsInThePast() {
        ScheduleGenerator generator = new ScheduleGenerator();
        LocalDateTime now = LocalDateTime.of(2026, 8, 2, 20, 10);
        AcademicTask task = task(1, "Future exam", LocalDateTime.of(2026, 8, 2, 23, 59), 35, 1,
                TaskStatus.PENDING, TaskType.EXAM);

        var result = generator.generate(List.of(task), 6, LocalDate.of(2026, 8, 2), now);

        assertTrue(result.sessions().stream().allMatch(session -> !session.startTime().isBefore(now)));
        assertTrue(result.sessions().stream().allMatch(session -> !session.endTime().isAfter(task.dueDate())));
    }

    @Test
    void usesRollingSevenDayAvailabilityOnSundayEvening() {
        ScheduleGenerator generator = new ScheduleGenerator();
        LocalDateTime now = LocalDateTime.of(2026, 8, 2, 20, 14);
        LocalDateTime deadline = LocalDateTime.of(2026, 8, 9, 23, 59);
        List<AcademicTask> tasks = List.of(
                task(1, "Final exam", deadline, 35, 4.4, TaskStatus.PENDING, TaskType.EXAM),
                task(2, "Group project", deadline, 30, 3.0, TaskStatus.PENDING, TaskType.ASSIGNMENT),
                task(3, "Mid-term exam", deadline, 20, 2.5, TaskStatus.PENDING, TaskType.EXAM),
                task(4, "Quizzes", deadline, 15, 1.0, TaskStatus.PENDING, TaskType.QUIZ)
        );

        var result = generator.generate(tasks, 12, LocalDate.of(2026, 8, 2), now);
        double totalHours = result.sessions().stream().mapToDouble(session -> session.durationHours()).sum();

        assertTrue(result.warnings().isEmpty());
        assertEquals(11.0, totalHours, 0.001); // 4.4 h is rounded up to a practical 4.5 h.
        assertTrue(result.sessions().stream().allMatch(session -> !session.startTime().isBefore(now)));
        assertTrue(result.sessions().stream().allMatch(session -> !session.endTime().isAfter(deadline)));
    }

    private AcademicTask task(int id, String title, LocalDateTime due, double weight, double hours,
                              TaskStatus status, TaskType type) {
        return new AcademicTask(id, title, due, weight, hours, status, type,
                null, null, null, null, null, null, 1);
    }
}
