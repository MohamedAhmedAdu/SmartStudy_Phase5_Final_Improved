package com.smartstudy.controller;

import com.smartstudy.config.Database;
import com.smartstudy.service.AuthService;
import com.smartstudy.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public final class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    private final AuthService auth = new AuthService();

    @FXML
    private void initialize() {
        emailField.setOnAction(event -> passwordField.requestFocus());
        passwordField.setOnAction(event -> login());
    }

    @FXML
    private void login() {
        statusLabel.setText("");
        try {
            Database.assertAvailable();
            auth.login(emailField.getText(), passwordField.getText());
            SceneNavigator.showMain();
        } catch (Exception e) {
            statusLabel.setText(e.getMessage());
            passwordField.clear();
            passwordField.requestFocus();
        }
    }

    @FXML
    private void register() {
        SceneNavigator.showRegister();
    }
}
