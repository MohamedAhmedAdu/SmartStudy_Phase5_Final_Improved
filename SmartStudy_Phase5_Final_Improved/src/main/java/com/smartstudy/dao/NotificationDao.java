package com.smartstudy.dao;

import com.smartstudy.config.Database;
import com.smartstudy.model.Notification;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public final class NotificationDao {
    public void insert(Notification notification) throws SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO notifications(message,send_at,channel,sent,student_id) VALUES(?,?,?,?,?)")) {
            statement.setString(1, notification.message());
            statement.setTimestamp(2, Timestamp.valueOf(notification.sendAt()));
            statement.setString(3, notification.channel());
            statement.setBoolean(4, notification.sent());
            statement.setInt(5, notification.studentId());
            statement.executeUpdate();
        }
    }

    public void replaceAutomatic(int studentId, List<Notification> notifications) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM notifications WHERE student_id=? AND sent=FALSE AND message LIKE '[AUTO]%'")) {
                    delete.setInt(1, studentId);
                    delete.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO notifications(message,send_at,channel,sent,student_id) VALUES(?,?,?,FALSE,?)")) {
                    for (Notification notification : notifications) {
                        insert.setString(1, notification.message());
                        insert.setTimestamp(2, Timestamp.valueOf(notification.sendAt()));
                        insert.setString(3, notification.channel());
                        insert.setInt(4, studentId);
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<Notification> findUpcoming(int studentId, int limit) throws SQLException {
        List<Notification> output = new ArrayList<>();
        String query = "SELECT * FROM notifications " +
                "WHERE student_id=? AND sent=FALSE AND send_at>=DATE_SUB(NOW(),INTERVAL 5 MINUTE) " +
                "ORDER BY send_at ASC LIMIT ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, studentId);
            statement.setInt(2, limit);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    output.add(new Notification(
                            result.getInt("notification_id"),
                            result.getString("message"),
                            result.getTimestamp("send_at").toLocalDateTime(),
                            result.getString("channel"),
                            result.getBoolean("sent"),
                            result.getInt("student_id")
                    ));
                }
            }
        }
        return output;
    }
}
