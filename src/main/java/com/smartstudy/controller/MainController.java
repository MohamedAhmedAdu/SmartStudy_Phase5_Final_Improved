package com.smartstudy.controller;

import com.smartstudy.model.Role;
import com.smartstudy.service.SessionManager;
import com.smartstudy.util.Alerts;
import com.smartstudy.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

public final class MainController {
    @FXML private BorderPane root;
    @FXML private Label userLabel;
    @FXML private Button dashboardButton;
    @FXML private Button coursesButton;
    @FXML private Button syllabusButton;
    @FXML private Button scheduleButton;
    @FXML private Button profileButton;
    @FXML private Button adminButton;

    @FXML
    private void initialize() {
        var principal = SessionManager.get().requireActive();
        boolean administrator = principal.role() == Role.ADMINISTRATOR;
        String roleText = administrator ? "Administrator" : "Student";
        userLabel.setText(principal.name() + "  •  " + roleText);
        userLabel.getStyleClass().add(administrator ? "admin-user-label" : "student-user-label");

        setNavigationVisible(dashboardButton, !administrator);
        setNavigationVisible(coursesButton, !administrator);
        setNavigationVisible(syllabusButton, !administrator);
        setNavigationVisible(scheduleButton, !administrator);
        setNavigationVisible(profileButton, !administrator);
        setNavigationVisible(adminButton, administrator);

        show(administrator ? "/fxml/admin.fxml" : "/fxml/dashboard.fxml");
    }

    @FXML private void dashboard() { show("/fxml/dashboard.fxml"); }
    @FXML private void courses() { show("/fxml/courses.fxml"); }
    @FXML private void syllabus() { show("/fxml/syllabus.fxml"); }
    @FXML private void schedule() { show("/fxml/schedule.fxml"); }
    @FXML private void profile() { show("/fxml/profile.fxml"); }
    @FXML private void admin() { show("/fxml/admin.fxml"); }

    @FXML
    private void logout() {
        if (Alerts.confirm("Log out", "End the current SmartStudy session?")) {
            SessionManager.get().logout();
            SceneNavigator.showLogin();
        }
    }

    private void show(String resource) {
        try {
            SessionManager.get().requireActive();
            Parent content = FXMLLoader.load(getClass().getResource(resource));
            root.setCenter(content);
        } catch (IllegalStateException e) {
            SessionManager.get().logout();
            Alerts.info("Session ended", e.getMessage());
            SceneNavigator.showLogin();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open " + resource, e);
        }
    }

    private void setNavigationVisible(Button button, boolean visible) {
        button.setVisible(visible);
        button.setManaged(visible);
    }
}
