package com.smartstudy.util;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public final class InputFormatters {
    private static final Pattern DECIMAL = Pattern.compile("\\d{0,4}(?:\\.\\d{0,2})?");
    private static final Pattern INTEGER = Pattern.compile("\\d{0,5}");

    private InputFormatters() {}

    public static void decimal(TextField field) {
        apply(field, DECIMAL);
    }

    public static void integer(TextField field) {
        apply(field, INTEGER);
    }

    private static void apply(TextField field, Pattern allowed) {
        UnaryOperator<TextFormatter.Change> filter = change ->
                allowed.matcher(change.getControlNewText()).matches() ? change : null;
        field.setTextFormatter(new TextFormatter<>(filter));
    }

    public static double parseRequiredDouble(TextField field, String label) {
        String value = field.getText() == null ? "" : field.getText().trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a number.");
        }
    }

    public static Integer parseOptionalInteger(TextField field, String label) {
        String value = field.getText() == null ? "" : field.getText().trim();
        if (value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a whole number.");
        }
    }
}
