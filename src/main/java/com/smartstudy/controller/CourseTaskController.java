package com.smartstudy.controller;

import com.smartstudy.model.AcademicTask;
import com.smartstudy.model.Course;
import com.smartstudy.model.TaskStatus;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class CourseTaskController {
    private static final DateTimeFormatter DUE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @FXML private TableView<Course> courseTable;
    @FXML private TableColumn<Course, String> codeCol;
    @FXML private TableColumn<Course, String> nameCol;
    @FXML private TableColumn<Course, String> instructorCol;
    @FXML private TableColumn<Course, String> semesterCol;

    @FXML private TableView<AcademicTask> taskTable;
    @FXML private TableColumn<AcademicTask, String> taskTitleCol;
    @FXML private TableColumn<AcademicTask, String> typeCol;
    @FXML private TableColumn<AcademicTask, String> dueCol;
    @FXML private TableColumn<AcademicTask, String> weightCol;
    @FXML private TableColumn<AcademicTask, String> hoursCol;
    @FXML private TableColumn<AcademicTask, String> statusCol;
    @FXML private Label statusLabel;

    private final SmartStudyService service = new SmartStudyService();

    @FXML
    private void initialize() {
        codeCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().courseCode()));
        nameCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().courseName()));
        instructorCol.setCellValueFactory(row -> new SimpleStringProperty(blankAsDash(row.getValue().instructor())));
        semesterCol.setCellValueFactory(row -> new SimpleStringProperty(blankAsDash(row.getValue().semester())));

        taskTitleCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().title()));
        typeCol.setCellValueFactory(row -> new SimpleStringProperty(readable(row.getValue().taskType().name())));
        dueCol.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().dueDate() == null ? "—" : row.getValue().dueDate().format(DUE_FORMAT)));
        weightCol.setCellValueFactory(row -> new SimpleStringProperty(
                String.format("%.1f%%", row.getValue().gradeWeight())));
        hoursCol.setCellValueFactory(row -> new SimpleStringProperty(
                String.format("%.1f h", row.getValue().estimatedHours())));
        statusCol.setCellValueFactory(row -> new SimpleStringProperty(readable(row.getValue().status().name())));

        courseTable.setPlaceholder(new Label("No courses yet. Select Add Course to begin."));
        taskTable.setPlaceholder(new Label("Select a course, then add or import its tasks."));
        courseTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, previous, selected) -> loadTasks(selected, null));
        refreshCourses(null);
    }

    private void refreshCourses(Integer preferredCourseId) {
        try {
            List<Course> courses = service.courses.findByStudent(SessionManager.get().requireActive().id());
            courseTable.setItems(FXCollections.observableArrayList(courses));
            Course selection = courses.stream()
                    .filter(course -> preferredCourseId != null && course.courseId() == preferredCourseId)
                    .findFirst()
                    .orElse(courses.isEmpty() ? null : courses.getFirst());
            if (selection != null) {
                courseTable.getSelectionModel().select(selection);
                courseTable.scrollTo(selection);
            } else {
                taskTable.getItems().clear();
            }
            statusLabel.setText(courses.isEmpty()
                    ? "Add your first course to start planning."
                    : courses.size() + " course(s) loaded.");
        } catch (Exception e) {
            Alerts.error("Unable to load courses", e.getMessage());
        }
    }

    private void loadTasks(Course course, Integer preferredTaskId) {
        try {
            List<AcademicTask> tasks = course == null
                    ? List.of()
                    : service.tasks.findByCourse(course.courseId());
            taskTable.setItems(FXCollections.observableArrayList(tasks));
            if (preferredTaskId != null) {
                tasks.stream()
                        .filter(task -> task.taskId() == preferredTaskId)
                        .findFirst()
                        .ifPresent(task -> {
                            taskTable.getSelectionModel().select(task);
                            taskTable.scrollTo(task);
                        });
            }
            if (course != null) {
                statusLabel.setText(tasks.size() + " task(s) in " + course.courseCode() + ".");
            }
        } catch (Exception e) {
            Alerts.error("Unable to load tasks", e.getMessage());
        }
    }

    @FXML private void addCourse() { editCourse(null); }

    @FXML
    private void editCourse() {
        Course selected = courseTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alerts.info("Select a course", "Select the course you want to edit.");
            return;
        }
        editCourse(selected);
    }

    @FXML
    private void deleteCourse() {
        Course selected = courseTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alerts.info("Select a course", "Select the course you want to delete.");
            return;
        }
        if (!Alerts.confirm("Delete course",
                "Delete " + selected + " and all of its tasks, syllabus data and study sessions?")) {
            return;
        }
        try {
            service.courses.delete(selected.courseId(), selected.studentId());
            service.regenerateCurrentWeek(SessionManager.get().requireActive().id());
            refreshCourses(null);
        } catch (Exception e) {
            Alerts.error("Delete failed", e.getMessage());
        }
    }

    private void editCourse(Course original) {
        Dialog<Course> dialog = new Dialog<>();
        dialog.setTitle(original == null ? "Add Course" : "Edit Course");
        dialog.setHeaderText(original == null
                ? "Enter the course information."
                : "Update the selected course.");
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        TextField code = new TextField(original == null ? "" : original.courseCode());
        TextField name = new TextField(original == null ? "" : original.courseName());
        TextField instructor = new TextField(original == null ? "" : original.instructor());
        TextField semester = new TextField(original == null ? "" : original.semester());
        code.setPromptText("Example: SWE401");
        name.setPromptText("Example: Software Engineering");
        instructor.setPromptText("Instructor name (optional)");
        semester.setPromptText("Example: Summer 2025–2026");

        GridPane grid = formGrid();
        addRow(grid, 0, "Course code", code);
        addRow(grid, 1, "Course name", name);
        addRow(grid, 2, "Instructor", instructor);
        addRow(grid, 3, "Semester", semester);
        dialog.getDialogPane().setContent(grid);

        AtomicReference<Course> validated = new AtomicReference<>();
        Node saveButton = dialog.getDialogPane().lookupButton(save);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                Validation.requireText(code.getText(), "Course code");
                Validation.requireText(name.getText(), "Course name");
                validated.set(new Course(
                        original == null ? 0 : original.courseId(),
                        code.getText(),
                        name.getText(),
                        instructor.getText(),
                        semester.getText(),
                        SessionManager.get().requireActive().id()
                ));
            } catch (Exception e) {
                event.consume();
                Alerts.error("Check course information", e.getMessage());
            }
        });
        dialog.setResultConverter(button -> button == save ? validated.get() : null);

        dialog.showAndWait().ifPresent(course -> {
            try {
                Course saved = service.saveCourse(course);
                refreshCourses(saved.courseId());
                statusLabel.setText("Course saved successfully.");
            } catch (Exception e) {
                String message = e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate")
                        ? "This course code is already used in your course list."
                        : e.getMessage();
                Alerts.error("Unable to save course", message);
            }
        });
    }

    @FXML
    private void addTask() {
        Course course = courseTable.getSelectionModel().getSelectedItem();
        if (course == null) {
            Alerts.info("Select a course", "Select a course before adding a task.");
            return;
        }
        editTask(null, course);
    }

    @FXML
    private void editTask() {
        AcademicTask task = taskTable.getSelectionModel().getSelectedItem();
        Course course = courseTable.getSelectionModel().getSelectedItem();
        if (task == null || course == null) {
            Alerts.info("Select a task", "Select the task you want to edit.");
            return;
        }
        editTask(task, course);
    }

    @FXML
    private void deleteTask() {
        AcademicTask task = taskTable.getSelectionModel().getSelectedItem();
        if (task == null) {
            Alerts.info("Select a task", "Select the task you want to delete.");
            return;
        }
        if (!Alerts.confirm("Delete task", "Delete “" + task.title() + "”?")) {
            return;
        }
        try {
            service.tasks.delete(task.taskId());
            service.regenerateCurrentWeek(SessionManager.get().requireActive().id());
            loadTasks(courseTable.getSelectionModel().getSelectedItem(), null);
            statusLabel.setText("Task deleted and the study plan was updated.");
        } catch (Exception e) {
            Alerts.error("Delete failed", e.getMessage());
        }
    }

    @FXML private void markInProgress() { updateTaskStatus(TaskStatus.IN_PROGRESS); }
    @FXML private void markCompleted() { updateTaskStatus(TaskStatus.COMPLETED); }

    private void updateTaskStatus(TaskStatus newStatus) {
        AcademicTask task = taskTable.getSelectionModel().getSelectedItem();
        if (task == null) {
            Alerts.info("Select a task", "Select a task before changing its status.");
            return;
        }
        try {
            service.tasks.updateStatus(task.taskId(), newStatus);
            service.regenerateCurrentWeek(SessionManager.get().requireActive().id());
            loadTasks(courseTable.getSelectionModel().getSelectedItem(), task.taskId());
            statusLabel.setText("Task marked " + readable(newStatus.name()).toLowerCase() + ". The plan was updated.");
        } catch (Exception e) {
            Alerts.error("Update failed", e.getMessage());
        }
    }

    private void editTask(AcademicTask original, Course course) {
        Dialog<AcademicTask> dialog = new Dialog<>();
        dialog.setTitle(original == null ? "Add Task" : "Edit Task");
        dialog.setHeaderText("Use numbers only for grade weight and estimated hours.");
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        TextField title = new TextField(original == null ? "" : original.title());
        ComboBox<TaskType> type = new ComboBox<>(FXCollections.observableArrayList(TaskType.values()));
        DatePicker due = new DatePicker(original == null
                ? LocalDate.now().plusDays(7)
                : original.dueDate().toLocalDate());
        TextField weight = new TextField(original == null ? "" : compact(original.gradeWeight()));
        TextField hours = new TextField(original == null ? "" : compact(original.estimatedHours()));
        ComboBox<TaskStatus> status = new ComboBox<>(FXCollections.observableArrayList(TaskStatus.values()));

        TextField submitType = new TextField(original == null ? "" : nullToEmpty(original.submitType()));
        CheckBox allowLate = new CheckBox("Late submission is allowed");
        TextField location = new TextField(original == null ? "" : nullToEmpty(original.location()));
        TextField duration = new TextField(original == null || original.durationMin() == null
                ? "" : String.valueOf(original.durationMin()));
        CheckBox online = new CheckBox("Online quiz");
        TextField attempts = new TextField(original == null || original.attempts() == null
                ? "" : String.valueOf(original.attempts()));

        type.setValue(original == null ? TaskType.ASSIGNMENT : original.taskType());
        status.setValue(original == null ? TaskStatus.PENDING : original.status());
        allowLate.setSelected(original != null && Boolean.TRUE.equals(original.allowLate()));
        online.setSelected(original != null && Boolean.TRUE.equals(original.online()));

        title.setPromptText("Task title");
        weight.setPromptText("0–100, for example 15");
        hours.setPromptText("Hours, for example 3.5");
        submitType.setPromptText("Blackboard, paper, presentation…");
        location.setPromptText("Exam room or location");
        duration.setPromptText("Minutes");
        attempts.setPromptText("Number of attempts");
        InputFormatters.decimal(weight);
        InputFormatters.decimal(hours);
        InputFormatters.integer(duration);
        InputFormatters.integer(attempts);

        GridPane grid = formGrid();
        addRow(grid, 0, "Title", title);
        addRow(grid, 1, "Type", type);
        addRow(grid, 2, "Due date", due);
        addRow(grid, 3, "Grade weight (0–100)", weight);
        addRow(grid, 4, "Estimated hours", hours);
        addRow(grid, 5, "Status", status);
        addRow(grid, 6, "Submission type", submitType);
        addRow(grid, 7, "Assignment option", allowLate);
        addRow(grid, 8, "Exam location", location);
        addRow(grid, 9, "Exam duration", duration);
        addRow(grid, 10, "Quiz option", online);
        addRow(grid, 11, "Quiz attempts", attempts);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(560);

        Runnable updateOptionalFields = () -> {
            TaskType selected = type.getValue();
            boolean assignment = selected == TaskType.ASSIGNMENT;
            boolean exam = selected == TaskType.EXAM;
            boolean quiz = selected == TaskType.QUIZ;
            setEnabled(submitType, assignment);
            setEnabled(allowLate, assignment);
            setEnabled(location, exam);
            setEnabled(duration, exam);
            setEnabled(online, quiz);
            setEnabled(attempts, quiz);
        };
        type.valueProperty().addListener((observable, previous, selected) -> updateOptionalFields.run());
        updateOptionalFields.run();

        AtomicReference<AcademicTask> validated = new AtomicReference<>();
        Node saveButton = dialog.getDialogPane().lookupButton(save);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                AcademicTask task = buildTask(
                        original, course, title, type, due, weight, hours, status,
                        submitType, allowLate, location, duration, online, attempts
                );
                validated.set(task);
            } catch (Exception e) {
                event.consume();
                Alerts.error("Check task information", e.getMessage());
            }
        });
        dialog.setResultConverter(button -> button == save ? validated.get() : null);

        dialog.showAndWait().ifPresent(task -> {
            try {
                AcademicTask saved = service.saveTask(task);
                loadTasks(course, saved.taskId());
                statusLabel.setText("Task saved and the weekly plan was regenerated.");
            } catch (Exception e) {
                Alerts.error("Unable to save task", e.getMessage());
            }
        });
    }

    private AcademicTask buildTask(AcademicTask original,
                                   Course course,
                                   TextField title,
                                   ComboBox<TaskType> type,
                                   DatePicker due,
                                   TextField weight,
                                   TextField hours,
                                   ComboBox<TaskStatus> status,
                                   TextField submitType,
                                   CheckBox allowLate,
                                   TextField location,
                                   TextField duration,
                                   CheckBox online,
                                   TextField attempts) {
        Validation.requireText(title.getText(), "Task title");
        if (due.getValue() == null) {
            throw new IllegalArgumentException("Due date is required.");
        }
        double parsedWeight = InputFormatters.parseRequiredDouble(weight, "Grade weight");
        double parsedHours = InputFormatters.parseRequiredDouble(hours, "Estimated hours");
        Validation.requirePercentage(parsedWeight, "Grade weight");
        Validation.requirePositive(parsedHours, "Estimated hours");

        TaskType selectedType = type.getValue();
        if (selectedType == null || status.getValue() == null) {
            throw new IllegalArgumentException("Task type and status are required.");
        }

        String selectedSubmitType = selectedType == TaskType.ASSIGNMENT
                ? emptyToNull(submitType.getText()) : null;
        Boolean selectedAllowLate = selectedType == TaskType.ASSIGNMENT
                ? allowLate.isSelected() : null;
        String selectedLocation = selectedType == TaskType.EXAM
                ? emptyToNull(location.getText()) : null;
        Integer selectedDuration = selectedType == TaskType.EXAM
                ? InputFormatters.parseOptionalInteger(duration, "Exam duration") : null;
        Boolean selectedOnline = selectedType == TaskType.QUIZ
                ? online.isSelected() : null;
        Integer selectedAttempts = selectedType == TaskType.QUIZ
                ? InputFormatters.parseOptionalInteger(attempts, "Quiz attempts") : null;

        if (selectedDuration != null && selectedDuration <= 0) {
            throw new IllegalArgumentException("Exam duration must be greater than zero.");
        }
        if (selectedAttempts != null && selectedAttempts <= 0) {
            throw new IllegalArgumentException("Quiz attempts must be greater than zero.");
        }

        return new AcademicTask(
                original == null ? 0 : original.taskId(),
                title.getText(),
                due.getValue().atTime(23, 59),
                parsedWeight,
                parsedHours,
                status.getValue(),
                selectedType,
                selectedSubmitType,
                selectedAllowLate,
                selectedLocation,
                selectedDuration,
                selectedOnline,
                selectedAttempts,
                course.courseId()
        );
    }

    private GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        return grid;
    }

    private void addRow(GridPane grid, int row, String label, Node field) {
        grid.add(new Label(label), 0, row);
        grid.add(field, 1, row);
    }

    private void setEnabled(Node node, boolean enabled) {
        node.setDisable(!enabled);
        node.setOpacity(enabled ? 1.0 : 0.45);
    }

    private String readable(String value) {
        String lower = value.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private String blankAsDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String compact(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
