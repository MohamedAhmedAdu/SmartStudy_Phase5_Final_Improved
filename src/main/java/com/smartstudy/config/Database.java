package com.smartstudy.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private Database() {}

    public static Connection getConnection() throws SQLException {
        String password = AppConfig.get("db.password");
        if (password.equals("CHANGE_ME")) {
            throw new SQLException("Database password is not configured. Open application.properties or smartstudy.properties and replace CHANGE_ME with your MySQL root password.");
        }
        return DriverManager.getConnection(
                AppConfig.get("db.url"),
                AppConfig.get("db.user"),
                password
        );
    }

    public static void assertAvailable() throws SQLException {
        try (Connection connection = getConnection()) {
            if (!connection.isValid(3)) {
                throw new SQLException("The database connection is not valid.");
            }
        } catch (SQLException e) {
            throw new SQLException(friendlyMessage(e), e);
        }
    }

    public static String friendlyMessage(SQLException e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        String lower = message.toLowerCase();
        if (lower.contains("communications link failure") || lower.contains("connection refused")) {
            return "SmartStudy cannot reach MySQL. Start the MySQL80 service and confirm that MySQL is using port 3306.";
        }
        if (lower.contains("access denied")) {
            return "MySQL rejected the username or password. Check db.user and db.password in application.properties or smartstudy.properties.";
        }
        if (lower.contains("unknown database")) {
            return "The smartstudy database does not exist. Run database/setup.sql in MySQL Workbench.";
        }
        if (lower.contains("change_me") || lower.contains("not configured")) {
            return message;
        }
        return message.isBlank() ? "Unable to connect to MySQL." : message;
    }
}
