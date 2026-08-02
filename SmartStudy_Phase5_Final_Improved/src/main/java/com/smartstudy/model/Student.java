package com.smartstudy.model;
public record Student(int studentId, String fullName, String email, String passwordHash, double availableHours,
                      boolean active, boolean emailNotifications, boolean inAppNotifications, boolean weeklySummary) {}
