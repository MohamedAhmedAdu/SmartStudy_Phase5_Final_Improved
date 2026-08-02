# Final Improvement Summary

This revision addresses the issues found during live testing:

- Prevents study sessions from being generated in the past.
- Hides old sessions from the current schedule screen.
- Adds dates and times to every study-session reminder.
- Shows reminders in chronological order and reduces ambiguity between repeated sessions.
- Filters completed tasks from the dashboard’s upcoming-task list.
- Adds a Completed Tasks dashboard metric.
- Automatically selects the first course and refreshes its task table.
- Adds direct Mark In Progress support.
- Adds numeric input filters and clearer labels for grade weight and estimated hours.
- Adds assignment, exam and quiz-specific optional fields.
- Parses DOCX tables as well as paragraphs.
- Rejects common attendance, absence, dismissal and policy sentences.
- Limits extracted titles safely and deduplicates extracted items.
- Adds Discard Incorrect Item and Confirm All Valid actions.
- Prevents duplicate confirmation of the same task/title/deadline.
- Shows a visible top-right logout button.
- Uses cyan for the logged-in student name and gold for the administrator name.
- Improves database error messages.
- Adds a launcher that always starts Maven from the correct project directory.
- Adds a local `smartstudy.properties` override so database credentials can remain outside source resources.
