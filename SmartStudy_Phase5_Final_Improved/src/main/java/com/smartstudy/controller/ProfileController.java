package com.smartstudy.controller;

import com.smartstudy.model.Student;
import com.smartstudy.service.SessionManager;
import com.smartstudy.service.SmartStudyService;
import com.smartstudy.util.InputFormatters;
import com.smartstudy.util.Validation;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.mindrot.jbcrypt.BCrypt;

public final class ProfileController {
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField hoursField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox emailNotifications;
    @FXML private CheckBox inAppNotifications;
    @FXML private CheckBox weeklySummary;
    @FXML private Label status;

    private final SmartStudyService service = new SmartStudyService();
    private Student current;

    @FXML
    private void initialize() {
        InputFormatters.decimal(hoursField);
        try {
            current = service.students.findById(SessionManager.get().requireActive().id()).orElseThrow();
            nameField.setText(current.fullName());
            emailField.setText(current.email());
            emailField.setDisable(true);
            hoursField.setText(compact(current.availableHours()));
            emailNotifications.setSelected(current.emailNotifications());
            inAppNotifications.setSelected(current.inAppNotifications());
            weeklySummary.setSelected(current.weeklySummary());
        } catch (Exception e) {
            status.setText(e.getMessage());
        }
    }

    @FXML
    private void save() {
        try {
            Validation.requireText(nameField.getText(), "Full name");
            Validation.requireMaxLength(nameField.getText(), 120, "Full name");
            double hours = InputFormatters.parseRequiredDouble(hoursField, "Available hours");
            Validation.requirePositive(hours, "Available hours");

            String passwordHash = current.passwordHash();
            if (!passwordField.getText().isBlank()) {
                Validation.requirePassword(passwordField.getText());
                passwordHash = BCrypt.hashpw(passwordField.getText(), BCrypt.gensalt(12));
            }

            Student updated = new Student(
                    current.studentId(),
                    nameField.getText().trim(),
                    current.email(),
                    passwordHash,
                    hours,
                    current.active(),
                    emailNotifications.isSelected(),
                    inAppNotifications.isSelected(),
                    weeklySummary.isSelected()
            );
            service.students.updateProfile(updated);
            current = updated;
            SessionManager.get().updateName(updated.fullName());
            service.regenerateCurrentWeek(updated.studentId());
            passwordField.clear();
            status.setText("Profile saved. Available hours and reminders were applied to the study plan.");
        } catch (Exception e) {
            status.setText(e.getMessage());
        }
    }

    private String compact(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
