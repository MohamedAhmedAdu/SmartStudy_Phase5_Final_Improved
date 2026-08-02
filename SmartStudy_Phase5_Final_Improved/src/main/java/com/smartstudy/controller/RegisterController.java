package com.smartstudy.controller;

import com.smartstudy.config.Database;
import com.smartstudy.service.AuthService;
import com.smartstudy.util.Alerts;
import com.smartstudy.util.InputFormatters;
import com.smartstudy.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public final class RegisterController {
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField hoursField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label statusLabel;

    private final AuthService auth = new AuthService();

    @FXML
    private void initialize() {
        InputFormatters.decimal(hoursField);
        hoursField.setText("10");
        confirmField.setOnAction(event -> create());
    }

    @FXML
    private void create() {
        statusLabel.setText("");
        try {
            Database.assertAvailable();
            if (!passwordField.getText().equals(confirmField.getText())) {
                throw new IllegalArgumentException("Passwords do not match.");
            }
            double hours = InputFormatters.parseRequiredDouble(hoursField, "Available hours");
            auth.register(nameField.getText(), emailField.getText(), passwordField.getText(), hours);
            Alerts.info("Account created", "Registration was successful. You can now log in.");
            SceneNavigator.showLogin();
        } catch (Exception e) {
            statusLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void back() {
        SceneNavigator.showLogin();
    }
}
