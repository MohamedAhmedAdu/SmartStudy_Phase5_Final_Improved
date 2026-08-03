package com.smartstudy.model;
import java.time.LocalDateTime;
public record AcademicTask(int taskId, String title, LocalDateTime dueDate, double gradeWeight, double estimatedHours,
                           TaskStatus status, TaskType taskType, String submitType, Boolean allowLate,
                           String location, Integer durationMin, Boolean online, Integer attempts, int courseId) {}
