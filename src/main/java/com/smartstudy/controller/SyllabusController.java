package com.smartstudy.controller;

import com.smartstudy.model.Course;
import com.smartstudy.model.ExtractedItem;
import com.smartstudy.model.TaskType;
import com.smartstudy.service.SessionManager;
import com.smartstudy.service.SmartStudyService;
import com.smartstudy.util.Alerts;
import com.smartstudy.util.InputFormatters;
import com.smartstudy.util.Validation;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class SyllabusController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @FXML private ComboBox<Course> courseBox;
    @FXML private TableView<ExtractedItem> itemTable;
    @FXML private TableColumn<ExtractedItem, String> titleCol;
    @FXML private TableColumn<ExtractedItem, String> typeCol;
    @FXML private TableColumn<ExtractedItem, String> dueCol;
    @FXML private TableColumn<ExtractedItem, String> weightCol;
    @FXML private Label fileLabel;
    @FXML private Label statusLabel;

    private final SmartStudyService service = new SmartStudyService();

    @FXML
    private void initialize() {
        titleCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().title()));
        typeCol.setCellValueFactory(row -> new SimpleStringProperty(readable(row.getValue().itemType().name())));
        dueCol.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().dueDate() == null
                        ? "Needs manual entry"
                        : row.getValue().dueDate().format(DATE_FORMAT)));
        weightCol.setCellValueFactory(row -> new SimpleStringProperty(
                String.format("%.1f%%", row.getValue().weight())));
        itemTable.setPlaceholder(new Label("Upload a PDF or DOCX syllabus to review extracted assessments."));
        itemTable.setRowFactory(table -> {
            TableRow<ExtractedItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    itemTable.getSelectionModel().select(row.getItem());
                    editSelected();
                }
            });
            return row;
        });

        courseBox.valueProperty().addListener((observable, previous, selected) -> loadPendingItems(selected));
        loadCourses();
    }

    private void loadCourses() {
        try {
            List<Course> courses = service.courses.findByStudent(SessionManager.get().requireActive().id());
            courseBox.setItems(FXCollections.observableArrayList(courses));
            if (!courses.isEmpty()) {
                courseBox.getSelectionModel().selectFirst();
            } else {
                statusLabel.setText("Add a course before uploading a syllabus.");
            }
        } catch (Exception e) {
            statusLabel.setText("Unable to load courses: " + e.getMessage());
        }
    }

    private void loadPendingItems(Course course) {
        if (course == null) {
            itemTable.getItems().clear();
            return;
        }
        try {
            List<ExtractedItem> items = service.syllabi.findPendingItemsByCourse(course.courseId());
            showItems(items);
            if (!items.isEmpty()) {
                statusLabel.setText(items.size() + " unconfirmed item(s) are ready for review.");
            }
        } catch (Exception e) {
            statusLabel.setText("Unable to load extracted items: " + e.getMessage());
        }
    }

    @FXML
    private void upload() {
        Course course = courseBox.getValue();
        if (course == null) {
            Alerts.info("Select a course", "Choose a course before uploading its syllabus.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a course syllabus");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Syllabus files (PDF or DOCX)", "*.pdf", "*.docx"));
        File file = chooser.showOpenDialog(itemTable.getScene().getWindow());
        if (file == null) {
            return;
        }

        fileLabel.setText(file.getName());
        statusLabel.setText("Reading the syllabus…");
        try {
            List<ExtractedItem> items = service.uploadAndExtract(course, file.toPath());
            showItems(items);
            if (items.isEmpty()) {
                statusLabel.setText("No reliable assessment rows were detected. Add tasks manually or use a clearer assessment table.");
            } else {
                statusLabel.setText(items.size() + " likely assessment item(s) found. Edit or discard mistakes before confirming.");
            }
        } catch (Exception e) {
            Alerts.error("Upload failed", e.getMessage());
            statusLabel.setText("Upload failed. The existing course data was not changed.");
        }
    }

    private void showItems(List<ExtractedItem> items) {
        itemTable.setItems(FXCollections.observableArrayList(items));
        if (!items.isEmpty()) {
            itemTable.getSelectionModel().selectFirst();
            itemTable.scrollTo(0);
        }
    }

    @FXML
    private void editSelected() {
        ExtractedItem original = itemTable.getSelectionModel().getSelectedItem();
        if (original == null) {
            Alerts.info("Select an item", "Select an extracted item to review it.");
            return;
        }

        Dialog<ExtractedItem> dialog = new Dialog<>();
        dialog.setTitle("Review Extracted Item");
        dialog.setHeaderText("Correct the extracted information before creating a real task.");
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        TextField title = new TextField(original.title());
        ComboBox<TaskType> type = new ComboBox<>(FXCollections.observableArrayList(TaskType.values()));
        DatePicker due = new DatePicker(original.dueDate() == null
                ? LocalDate.now().plusDays(7)
                : original.dueDate().toLocalDate());
        TextField weight = new TextField(compact(original.weight()));
        type.setValue(original.itemType());
        weight.setPromptText("0–100, for example 20");
        InputFormatters.decimal(weight);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(new Label("Title"), 0, 0);
        grid.add(title, 1, 0);
        grid.add(new Label("Type"), 0, 1);
        grid.add(type, 1, 1);
        grid.add(new Label("Due date"), 0, 2);
        grid.add(due, 1, 2);
        grid.add(new Label("Grade weight (0–100)"), 0, 3);
        grid.add(weight, 1, 3);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(560);

        AtomicReference<ExtractedItem> validated = new AtomicReference<>();
        Node saveButton = dialog.getDialogPane().lookupButton(save);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                Validation.requireText(title.getText(), "Title");
                Validation.requireMaxLength(title.getText(), 180, "Title");
                if (due.getValue() == null) {
                    throw new IllegalArgumentException("Due date is required before confirmation.");
                }
                double parsedWeight = InputFormatters.parseRequiredDouble(weight, "Grade weight");
                Validation.requirePercentage(parsedWeight, "Grade weight");
                validated.set(new ExtractedItem(
                        original.itemId(),
                        title.getText().trim(),
                        type.getValue(),
                        due.getValue().atTime(23, 59),
                        original.extractedDate(),
                        parsedWeight,
                        false,
                        original.syllabusId(),
                        original.taskId()
                ));
            } catch (Exception e) {
                event.consume();
                Alerts.error("Check extracted information", e.getMessage());
            }
        });
        dialog.setResultConverter(button -> button == save ? validated.get() : null);

        dialog.showAndWait().ifPresent(updated -> {
            try {
                service.syllabi.updateItem(updated);
                int index = itemTable.getItems().indexOf(original);
                itemTable.getItems().set(index, updated);
                itemTable.getSelectionModel().select(updated);
                statusLabel.setText("Extracted item updated. Confirm it when the information is correct.");
            } catch (Exception e) {
                Alerts.error("Update failed", e.getMessage());
            }
        });
    }

    @FXML
    private void discardSelected() {
        ExtractedItem selected = itemTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alerts.info("Select an item", "Select an incorrect extracted item to discard it.");
            return;
        }
        if (!Alerts.confirm("Discard extracted item",
                "Discard “" + selected.title() + "”? This will not delete any confirmed task.")) {
            return;
        }
        try {
            service.syllabi.deletePendingItem(selected.itemId());
            removeAndSelectNext(selected);
            statusLabel.setText("Incorrect extracted item discarded.");
        } catch (Exception e) {
            Alerts.error("Discard failed", e.getMessage());
        }
    }

    @FXML
    private void confirmSelected() {
        ExtractedItem selected = itemTable.getSelectionModel().getSelectedItem();
        Course course = courseBox.getValue();
        if (selected == null) {
            Alerts.info("Select an item", "Select the item you want to confirm.");
            return;
        }
        if (course == null) {
            Alerts.info("Select a course", "Choose the course for this task.");
            return;
        }
        if (selected.dueDate() == null) {
            Alerts.error("Missing deadline", "Edit this item and enter a due date before confirming it.");
            return;
        }
        try {
            service.confirmItem(selected, course.courseId());
            removeAndSelectNext(selected);
            statusLabel.setText("Item confirmed and added to Courses & Tasks. The schedule was regenerated.");
        } catch (Exception e) {
            Alerts.error("Confirmation failed", e.getMessage());
        }
    }

    @FXML
    private void confirmAllValid() {
        Course course = courseBox.getValue();
        List<ExtractedItem> valid = itemTable.getItems().stream()
                .filter(item -> item.dueDate() != null)
                .toList();
        if (course == null || valid.isEmpty()) {
            Alerts.info("Nothing to confirm", "No extracted items with valid due dates are available.");
            return;
        }
        if (!Alerts.confirm("Confirm valid items",
                "Create " + valid.size() + " task(s) from the reviewed extracted items?")) {
            return;
        }

        List<String> failures = new ArrayList<>();
        int confirmed = 0;
        for (ExtractedItem item : List.copyOf(valid)) {
            try {
                service.confirmItem(item, course.courseId());
                itemTable.getItems().remove(item);
                confirmed++;
            } catch (Exception e) {
                failures.add(item.title() + ": " + e.getMessage());
            }
        }
        if (!itemTable.getItems().isEmpty()) {
            itemTable.getSelectionModel().selectFirst();
        }
        statusLabel.setText(confirmed + " item(s) confirmed."
                + (failures.isEmpty() ? "" : " " + failures.size() + " item(s) need attention."));
        if (!failures.isEmpty()) {
            Alerts.error("Some items were not confirmed", String.join("\n", failures));
        }
    }

    private void removeAndSelectNext(ExtractedItem item) {
        int index = itemTable.getItems().indexOf(item);
        itemTable.getItems().remove(item);
        if (!itemTable.getItems().isEmpty()) {
            itemTable.getSelectionModel().select(Math.min(index, itemTable.getItems().size() - 1));
        }
    }

    private String readable(String value) {
        String lower = value.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private String compact(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
