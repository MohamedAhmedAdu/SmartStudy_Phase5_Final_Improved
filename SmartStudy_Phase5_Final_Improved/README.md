# SmartStudy — Final Improved Phase 5 Prototype

SmartStudy is a JavaFX desktop application that stores academic information in MySQL, extracts likely assessments from PDF/DOCX syllabi, and generates a prioritized weekly study schedule.

## Technology

- Java 21 and JavaFX 21
- Maven
- MySQL 8 and JDBC
- PDFBox for PDF text extraction
- Apache POI for DOCX paragraphs and tables
- BCrypt password hashing
- JUnit 5 tests

## Main functions

- Student registration, login, session timeout and visible logout
- Separate administrator login and account-management console
- Course and task create/edit/delete functions
- Assignment-, exam- and quiz-specific optional fields
- Task status: Pending, In Progress and Completed
- PDF/DOCX syllabus upload
- Stricter rule-based extraction that rejects common policy sentences
- Review, edit, discard, confirm selected and confirm-all-valid actions
- Schedule generation based on deadline, grade weight, effort and weekly hours
- No study sessions in the past
- Automatic regeneration after task/profile changes
- Dashboard progress, completed-task count and clearer dated reminders
- Distinct user colors: student names are cyan; administrator names are gold

## Fastest way to upgrade an existing working copy

Use `SmartStudy_Final_Upgrade_Patch.zip` rather than the full package:

1. Close SmartStudy.
2. Extract the patch into the existing `SmartStudy_Phase5_Submission` folder.
3. Choose **Replace the files in the destination**.
4. The patch deliberately does not contain `application.properties`, so your working MySQL password remains unchanged.
5. Double-click `run-windows.bat` or run `mvn javafx:run` from the project folder.

Your current MySQL database and student records remain available. Delete any old incorrectly confirmed tasks manually from **Courses & Tasks**.

## Fresh installation

### Requirements

- JDK 21 or later
- Apache Maven 3.9+
- MySQL Server 8+
- MySQL Workbench (recommended)

### Database

1. Start the `MySQL80` Windows service.
2. Open MySQL Workbench.
3. Run `database/setup.sql`.
4. Confirm the `smartstudy` schema appears.

### Database password

Choose either method:

**Method A — edit the bundled configuration**

Open:

`src/main/resources/application.properties`

Replace:

`db.password=CHANGE_ME`

with your MySQL root password.

**Method B — local override (recommended)**

1. Copy `smartstudy.properties.example`.
2. Rename the copy to `smartstudy.properties`.
3. Enter your MySQL password there.

The local override is ignored by Git and keeps the password outside the source resources.

### Run on Windows

Double-click:

`run-windows.bat`

The launcher automatically changes to the correct project folder, so it avoids the “there is no POM in this directory” error.

Or run:

```bat
mvn javafx:run
```

Run tests separately with:

```bat
run-tests-windows.bat
```

or:

```bat
mvn clean test
```

## Default administrator

- Email: `admin@smartstudy.local`
- Password: `Admin123!`

The student name appears in cyan. The administrator name appears in gold.

## Recommended demonstration flow

1. Register and log in as a student.
2. Add a course.
3. Add an assignment manually and demonstrate numeric-only weight/hour fields.
4. Upload `sample-syllabi/sample_syllabus.docx`.
5. Review the extracted rows; edit or discard anything incorrect.
6. Confirm selected items or use **Confirm All Valid**.
7. Regenerate the schedule and show that all sessions are in the future.
8. Mark a task In Progress, then Completed.
9. Refresh the dashboard and show updated progress and reminders.
10. Log out from the top-right button.
11. Log in as administrator and activate/deactivate a student account.

## Optional clean demo database

`database/reset_demo_data.sql` removes all student/demo data but preserves the administrator table. Use it only when you intentionally want a fresh demonstration.

## Prototype boundaries

- Institutional Blackboard/Canvas/Moodle authentication is not implemented.
- Email delivery is not configured; the email checkbox is a stored prototype preference.
- Syllabus extraction is rule-based because university syllabi are not standardized. Review and confirmation remain mandatory.
