package com.smartstudy.model;
public record Course(int courseId, String courseCode, String courseName, String instructor, String semester, int studentId) {
    @Override public String toString() { return courseCode + " - " + courseName; }
}
