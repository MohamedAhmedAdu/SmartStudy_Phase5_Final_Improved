package com.smartstudy.model;
import java.time.LocalDateTime;
public record StudySession(int sessionId, LocalDateTime startTime, LocalDateTime endTime,
                           double durationHours, int scheduleId, Integer taskId, String taskTitle) {}
