package com.smartstudy.controller;

import com.smartstudy.dao.StudentDao;
import com.smartstudy.model.Role;
import com.smartstudy.model.Student;
import com.smartstudy.service.SessionManager;
import com.smartstudy.util.Alerts;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class AdminController {
    @FXML private TableView<Student> table;
    @FXML private TableColumn<Student, String> idCol;
    @FXML private TableColumn<Student, String> nameCol;
    @FXML private TableColumn<Student, String> emailCol;
    @FXML private TableColumn<Student, String> hoursCol;
    @FXML private TableColumn<Student, String> statusCol;
    @FXML private Label statusLabel;

    private final StudentDao dao = new StudentDao();

    @FXML
    private void initialize() {
        if (SessionManager.get().requireActive().role() != Role.ADMINISTRATOR) {
            throw new IllegalStateException("Administrator access is required.");
        }
        idCol.setCellValueFactory(row -> new SimpleStringProperty(String.valueOf(row.getValue().studentId())));
        nameCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().fullName()));
        emailCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().email()));
        hoursCol.setCellValueFactory(row -> new SimpleStringProperty(String.format("%.1f", row.getValue().availableHours())));
        statusCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().active() ? "Active" : "Deactivated"));
        statusCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : value);
                getStyleClass().removeAll("status-active", "status-inactive");
                if (!empty && value != null) {
                    getStyleClass().add("Active".equals(value) ? "status-active" : "status-inactive");
                }
            }
        });
        table.setPlaceholder(new Label("No student accounts have been registered."));
        refresh();
    }

    @FXML
    private void refresh() {
        try {
            var students = dao.findAll();
            table.setItems(FXCollections.observableArrayList(students));
            statusLabel.setText(students.size() + " student account(s) loaded.");
        } catch (Exception e) {
            Alerts.error("Unable to load accounts", e.getMessage());
        }
    }

    @FXML private void activate() { setActive(true); }
    @FXML private void deactivate() { setActive(false); }

    private void setActive(boolean active) {
        Student selected = selectedStudent();
        if (selected == null) {
            return;
        }
        if (selected.active() == active) {
            Alerts.info("No change needed", selected.fullName() + " is already " + (active ? "active." : "deactivated."));
            return;
        }
        try {
            dao.setActive(selected.studentId(), active);
            refresh();
            statusLabel.setText(selected.fullName() + " was " + (active ? "activated." : "deactivated."));
        } catch (Exception e) {
            Alerts.error("Update failed", e.getMessage());
        }
    }

    @FXML
    private void delete() {
        Student selected = selectedStudent();
        if (selected == null) {
            return;
        }
        if (!Alerts.confirm("Delete student account",
                "Permanently delete " + selected.fullName() + " and all related academic data?")) {
            return;
        }
        try {
            dao.delete(selected.studentId());
            refresh();
            statusLabel.setText("Student account deleted.");
        } catch (Exception e) {
            Alerts.error("Delete failed", e.getMessage());
        }
    }

    private Student selectedStudent() {
        Student selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alerts.info("Select a student", "Select a student account first.");
        }
        return selected;
    }
}
