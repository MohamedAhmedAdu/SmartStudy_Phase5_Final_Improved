package com.smartstudy.model;
import java.time.LocalDateTime;
public record Syllabus(int syllabusId, String fileName, String fileFormat, LocalDateTime uploadDate, String storedPath, int courseId) {}
