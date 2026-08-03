package com.smartstudy.dao;

import com.smartstudy.config.Database;
import com.smartstudy.model.Schedule;
import com.smartstudy.model.StudySession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ScheduleDao {
    public Schedule replace(int studentId, LocalDate weekStart, List<StudySession> sessions) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int scheduleId;
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM schedules WHERE student_id=? AND week_start=?")) {
                    delete.setInt(1, studentId);
                    delete.setDate(2, java.sql.Date.valueOf(weekStart));
                    delete.executeUpdate();
                }
                try (PreparedStatement insertSchedule = connection.prepareStatement(
                        "INSERT INTO schedules(generated_on,week_start,student_id) VALUES(NOW(),?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    insertSchedule.setDate(1, java.sql.Date.valueOf(weekStart));
                    insertSchedule.setInt(2, studentId);
                    insertSchedule.executeUpdate();
                    try (ResultSet keys = insertSchedule.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Unable to create the study schedule.");
                        }
                        scheduleId = keys.getInt(1);
                    }
                }
                try (PreparedStatement insertSession = connection.prepareStatement(
                        "INSERT INTO study_sessions(start_time,end_time,duration_hours,schedule_id,task_id) VALUES(?,?,?,?,?)")) {
                    for (StudySession session : sessions) {
                        insertSession.setTimestamp(1, Timestamp.valueOf(session.startTime()));
                        insertSession.setTimestamp(2, Timestamp.valueOf(session.endTime()));
                        insertSession.setDouble(3, session.durationHours());
                        insertSession.setInt(4, scheduleId);
                        if (session.taskId() == null) {
                            insertSession.setNull(5, Types.INTEGER);
                        } else {
                            insertSession.setInt(5, session.taskId());
                        }
                        insertSession.addBatch();
                    }
                    insertSession.executeBatch();
                }
                connection.commit();

                List<StudySession> saved = sessions.stream()
                        .map(session -> new StudySession(
                                session.sessionId(),
                                session.startTime(),
                                session.endTime(),
                                session.durationHours(),
                                scheduleId,
                                session.taskId(),
                                session.taskTitle()
                        ))
                        .toList();
                return new Schedule(scheduleId, LocalDateTime.now(), weekStart, studentId, saved);
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<StudySession> findCurrent(int studentId) throws SQLException {
        String query = "SELECT ss.*,t.title task_title FROM study_sessions ss " +
                "JOIN schedules s ON s.schedule_id=ss.schedule_id " +
                "LEFT JOIN tasks t ON t.task_id=ss.task_id " +
                "WHERE s.schedule_id=(" +
                "SELECT schedule_id FROM schedules WHERE student_id=? " +
                "ORDER BY generated_on DESC, schedule_id DESC LIMIT 1" +
                ") ORDER BY ss.start_time";
        List<StudySession> output = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, studentId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    LocalDateTime startTime = result.getTimestamp("start_time").toLocalDateTime();
                    LocalDateTime endTime = result.getTimestamp("end_time").toLocalDateTime();
                    // Filter with the application's local clock instead of MySQL NOW(),
                    // avoiding timezone differences between JDBC and the database server.
                    if (endTime.isBefore(now)) {
                        continue;
                    }
                    output.add(new StudySession(
                            result.getInt("session_id"),
                            startTime,
                            endTime,
                            result.getDouble("duration_hours"),
                            result.getInt("schedule_id"),
                            (Integer) result.getObject("task_id"),
                            result.getString("task_title")
                    ));
                }
            }
        }
        return output;
    }

}
