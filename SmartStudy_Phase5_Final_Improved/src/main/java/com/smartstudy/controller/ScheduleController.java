package com.smartstudy.controller;

import com.smartstudy.model.StudySession;
import com.smartstudy.service.SessionManager;
import com.smartstudy.service.SmartStudyService;
import com.smartstudy.util.Alerts;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.format.DateTimeFormatter;

public final class ScheduleController {
    private static final DateTimeFormatter START_FORMAT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  HH:mm");
    private static final DateTimeFormatter END_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private TableView<StudySession> table;
    @FXML private TableColumn<StudySession, String> taskCol;
    @FXML private TableColumn<StudySession, String> startCol;
    @FXML private TableColumn<StudySession, String> endCol;
    @FXML private TableColumn<StudySession, String> durationCol;
    @FXML private Label status;

    private final SmartStudyService service = new SmartStudyService();

    @FXML
    private void initialize() {
        taskCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().taskTitle()));
        startCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().startTime().format(START_FORMAT)));
        endCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().endTime().format(END_FORMAT)));
        durationCol.setCellValueFactory(row -> new SimpleStringProperty(String.format("%.1f h", row.getValue().durationHours())));
        table.setPlaceholder(new Label("No future study sessions. Select Regenerate Plan after adding tasks."));
        load();
    }

    private void load() {
        try {
            var sessions = service.schedules.findCurrent(SessionManager.get().requireActive().id());
            table.setItems(FXCollections.observableArrayList(sessions));
            if (sessions.isEmpty()) {
                status.setText("No future study sessions are stored for this week.");
            } else {
                status.setText(sessions.size() + " future study block(s) scheduled. Past sessions are hidden.");
            }
        } catch (Exception e) {
            status.setText("Unable to load the schedule: " + e.getMessage());
        }
    }

    @FXML
    private void regenerate() {
        long start = System.nanoTime();
        try {
            var result = service.regenerateCurrentWeek(SessionManager.get().requireActive().id());
            load();
            double seconds = (System.nanoTime() - start) / 1_000_000_000.0;
            String warningText = result.warnings().isEmpty()
                    ? ""
                    : " " + String.join(" ", result.warnings());
            status.setText("Generated " + result.sessions().size() + " future study block(s) in "
                    + String.format("%.2f", seconds) + " seconds." + warningText);
        } catch (Exception e) {
            Alerts.error("Schedule generation failed", e.getMessage());
        }
    }
}
