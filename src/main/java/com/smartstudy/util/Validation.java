package com.smartstudy.util;

import java.util.regex.Pattern;

public final class Validation {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private Validation() {}

    public static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
    }

    public static void requireMaxLength(String value, int max, String label) {
        if (value != null && value.trim().length() > max) {
            throw new IllegalArgumentException(label + " must not exceed " + max + " characters.");
        }
    }

    public static void requireEmail(String email) {
        requireText(email, "Email");
        if (!EMAIL.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
    }

    public static void requirePassword(String password) {
        requireText(password, "Password");
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must contain at least 8 characters.");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain an uppercase letter.");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("Password must contain a number.");
        }
    }

    public static void requirePositive(double value, String label) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(label + " must be greater than zero.");
        }
    }

    public static void requirePercentage(double value, String label) {
        if (!Double.isFinite(value) || value < 0 || value > 100) {
            throw new IllegalArgumentException(label + " must be between 0 and 100.");
        }
    }
}
