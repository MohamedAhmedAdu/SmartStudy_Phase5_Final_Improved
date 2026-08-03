package com.smartstudy.controller;

import com.smartstudy.model.AcademicTask;
import com.smartstudy.model.Notification;
import com.smartstudy.model.StudySession;
import com.smartstudy.model.TaskStatus;
import com.smartstudy.service.SessionManager;
import com.smartstudy.service.SmartStudyService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public final class DashboardController {
    private static final DateTimeFormatter DUE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final DateTimeFormatter REMINDER_FORMAT = DateTimeFormatter.ofPattern("dd MMM, HH:mm");

    @FXML private Label courseCount;
    @FXML private Label studyHours;
    @FXML private Label progressLabel;
    @FXML private Label completedCount;
    @FXML private TableView<AcademicTask> taskTable;
    @FXML private TableColumn<AcademicTask, String> titleCol;
    @FXML private TableColumn<AcademicTask, String> dueCol;
    @FXML private TableColumn<AcademicTask, String> statusCol;
    @FXML private ListView<String> reminders;
    @FXML private Label dashboardStatus;

    private final SmartStudyService service = new SmartStudyService();

    @FXML
    private void initialize() {
        titleCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().title()));
        dueCol.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().dueDate() == null ? "—" : row.getValue().dueDate().format(DUE_FORMAT)));
        statusCol.setCellValueFactory(row -> new SimpleStringProperty(readable(row.getValue().status().name())));
        taskTable.setPlaceholder(new Label("No upcoming tasks. Add a task or upload a syllabus."));
        reminders.setPlaceholder(new Label("No reminders are currently scheduled."));
        reminders.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setWrapText(true);
            }
        });
        refresh();
    }

    @FXML
    private void refresh() {
        try {
            int studentId = SessionManager.get().requireActive().id();
            var courses = service.courses.findByStudent(studentId);
            var allTasks = service.tasks.findByStudent(studentId);
            long completed = allTasks.stream().filter(task -> task.status() == TaskStatus.COMPLETED).count();

            courseCount.setText(String.valueOf(courses.size()));
            completedCount.setText(String.valueOf(completed));
            progressLabel.setText(allTasks.isEmpty()
                    ? "0%"
                    : String.format("%.0f%%", completed * 100.0 / allTasks.size()));

            double scheduledHours = service.schedules.findCurrent(studentId).stream()
                    .mapToDouble(StudySession::durationHours)
                    .sum();
            studyHours.setText(String.format("%.1f hrs", scheduledHours));

            List<AcademicTask> upcoming = allTasks.stream()
                    .filter(task -> task.status() != TaskStatus.COMPLETED)
                    .filter(task -> task.dueDate() != null)
                    .sorted(Comparator.comparing(AcademicTask::dueDate))
                    .limit(10)
                    .toList();
            taskTable.setItems(FXCollections.observableArrayList(upcoming));

            List<String> reminderText = service.notifications.findUpcoming(studentId, 10).stream()
                    .map(this::displayNotification)
                    .toList();
            reminders.setItems(FXCollections.observableArrayList(reminderText));
            dashboardStatus.setText("Updated " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + ".");
        } catch (Exception e) {
            dashboardStatus.setText("Unable to load dashboard: " + e.getMessage());
        }
    }

    private String displayNotification(Notification notification) {
        String message = notification.message().replaceFirst("^\\[AUTO]\\s*", "");
        return notification.sendAt().format(REMINDER_FORMAT) + "  •  " + message;
    }

    private String readable(String value) {
        String lower = value.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
