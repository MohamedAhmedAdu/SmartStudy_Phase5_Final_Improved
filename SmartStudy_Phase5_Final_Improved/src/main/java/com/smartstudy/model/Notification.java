package com.smartstudy.model;
import java.time.LocalDateTime;
public record Notification(int notificationId, String message, LocalDateTime sendAt, String channel, boolean sent, int studentId) {}
