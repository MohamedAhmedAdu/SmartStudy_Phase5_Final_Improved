package com.smartstudy.model;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
public record Schedule(int scheduleId, LocalDateTime generatedOn, LocalDate weekStart, int studentId,
                       List<StudySession> sessions) {}
