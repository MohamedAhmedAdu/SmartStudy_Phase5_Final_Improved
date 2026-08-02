package com.smartstudy.dao;

import com.smartstudy.config.Database;
import com.smartstudy.model.ExtractedItem;
import com.smartstudy.model.Syllabus;
import com.smartstudy.model.TaskType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class SyllabusDao {
    public int upsert(Syllabus syllabus) throws SQLException {
        String query = "INSERT INTO syllabi(file_name,file_format,upload_date,stored_path,course_id) " +
                "VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE " +
                "file_name=?,file_format=?,upload_date=?,stored_path=?,syllabus_id=LAST_INSERT_ID(syllabus_id)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, syllabus.fileName());
            statement.setString(2, syllabus.fileFormat());
            statement.setTimestamp(3, Timestamp.valueOf(syllabus.uploadDate()));
            statement.setString(4, syllabus.storedPath());
            statement.setInt(5, syllabus.courseId());
            statement.setString(6, syllabus.fileName());
            statement.setString(7, syllabus.fileFormat());
            statement.setTimestamp(8, Timestamp.valueOf(syllabus.uploadDate()));
            statement.setString(9, syllabus.storedPath());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            try (PreparedStatement lookup = connection.prepareStatement(
                    "SELECT syllabus_id FROM syllabi WHERE course_id=?")) {
                lookup.setInt(1, syllabus.courseId());
                try (ResultSet result = lookup.executeQuery()) {
                    if (result.next()) {
                        return result.getInt(1);
                    }
                }
            }
            throw new SQLException("Unable to resolve the saved syllabus ID.");
        }
    }

    public void replaceExtractedItems(int syllabusId, List<ExtractedItem> items) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM extracted_items WHERE syllabus_id=? AND confirmed=FALSE")) {
                    delete.setInt(1, syllabusId);
                    delete.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO extracted_items(title,item_type,due_date,extracted_date,weight,confirmed,syllabus_id) " +
                                "VALUES(?,?,?,?,?,FALSE,?)")) {
                    for (ExtractedItem item : items) {
                        insert.setString(1, item.title());
                        insert.setString(2, item.itemType().name());
                        if (item.dueDate() == null) {
                            insert.setNull(3, Types.TIMESTAMP);
                        } else {
                            insert.setTimestamp(3, Timestamp.valueOf(item.dueDate()));
                        }
                        insert.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                        insert.setDouble(5, item.weight());
                        insert.setInt(6, syllabusId);
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

    public List<ExtractedItem> findPendingItems(int syllabusId) throws SQLException {
        List<ExtractedItem> output = new ArrayList<>();
        String query = "SELECT * FROM extracted_items WHERE syllabus_id=? AND confirmed=FALSE ORDER BY due_date IS NULL,due_date,title";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, syllabusId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    output.add(map(result));
                }
            }
        }
        return output;
    }

    public List<ExtractedItem> findPendingItemsByCourse(int courseId) throws SQLException {
        List<ExtractedItem> output = new ArrayList<>();
        String query = "SELECT ei.* FROM extracted_items ei JOIN syllabi s ON s.syllabus_id=ei.syllabus_id " +
                "WHERE s.course_id=? AND ei.confirmed=FALSE ORDER BY ei.due_date IS NULL,ei.due_date,ei.title";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, courseId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    output.add(map(result));
                }
            }
        }
        return output;
    }

    public void deletePendingItem(int itemId) throws SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM extracted_items WHERE item_id=? AND confirmed=FALSE")) {
            statement.setInt(1, itemId);
            statement.executeUpdate();
        }
    }

    public void updateItem(ExtractedItem item) throws SQLException {
        String query = "UPDATE extracted_items SET title=?,item_type=?,due_date=?,weight=? " +
                "WHERE item_id=? AND confirmed=FALSE";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, item.title());
            statement.setString(2, item.itemType().name());
            if (item.dueDate() == null) {
                statement.setNull(3, Types.TIMESTAMP);
            } else {
                statement.setTimestamp(3, Timestamp.valueOf(item.dueDate()));
            }
            statement.setDouble(4, item.weight());
            statement.setInt(5, item.itemId());
            if (statement.executeUpdate() == 0) {
                throw new SQLException("The extracted item was not updated. It may already be confirmed.");
            }
        }
    }

    public void confirmAndLink(int itemId, int taskId) throws SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE extracted_items SET confirmed=TRUE,task_id=? WHERE item_id=? AND confirmed=FALSE")) {
            statement.setInt(1, taskId);
            statement.setInt(2, itemId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("The extracted item was already confirmed or no longer exists.");
            }
        }
    }

    private ExtractedItem map(ResultSet result) throws SQLException {
        Timestamp due = result.getTimestamp("due_date");
        return new ExtractedItem(
                result.getInt("item_id"),
                result.getString("title"),
                TaskType.valueOf(result.getString("item_type")),
                due == null ? null : due.toLocalDateTime(),
                result.getTimestamp("extracted_date").toLocalDateTime(),
                result.getDouble("weight"),
                result.getBoolean("confirmed"),
                result.getInt("syllabus_id"),
                (Integer) result.getObject("task_id")
        );
    }
}
