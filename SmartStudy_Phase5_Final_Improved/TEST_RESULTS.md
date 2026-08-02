# SmartStudy Final Validation Status

## Completed in the preparation environment

| Check | Status |
|---|---|
| All 9 FXML files parse as valid XML | PASS |
| `pom.xml` parses as valid XML | PASS |
| All production Java source files compile against API-compatible dependency stubs using Java 21 | PASS |
| All JUnit source files compile | PASS |
| Core models and schedule generator compile with the real JDK 21 | PASS |
| Schedule smoke test creates no past session | PASS |
| Schedule smoke test respects the task deadline | PASS |
| Parser smoke test rejects absence/final-dismissal policy sentences | PASS |
| Parser smoke test extracts valid Final Exam and Assignment rows | PASS |
| Extracted titles stay inside the database-safe limit | PASS |

## Required on the team workstation

Run the real dependencies and MySQL integration:

```bat
mvn clean test
mvn javafx:run
```

Then verify registration, login, CRUD, syllabus upload, confirmation, scheduling, completion, logout and administrator actions against the local MySQL database.
