package com.smartstudy.model;
import java.time.LocalDateTime;
public record ExtractedItem(int itemId, String title, TaskType itemType, LocalDateTime dueDate,
                            LocalDateTime extractedDate, double weight, boolean confirmed,
                            int syllabusId, Integer taskId) {}
