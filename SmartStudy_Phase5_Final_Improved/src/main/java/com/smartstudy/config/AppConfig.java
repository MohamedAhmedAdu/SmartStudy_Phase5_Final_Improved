package com.smartstudy.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class AppConfig {
    private static final Properties P = new Properties();

    static {
        loadClasspathDefaults();
        loadLocalOverrides();
        loadEnvironmentOverrides();
    }

    private AppConfig() {}

    private static void loadClasspathDefaults() {
        try (InputStream in = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (in == null) {
                throw new IllegalStateException("application.properties is missing");
            }
            P.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static void loadLocalOverrides() {
        Path local = Path.of("smartstudy.properties").toAbsolutePath();
        if (!Files.isRegularFile(local)) {
            return;
        }
        try (InputStream in = Files.newInputStream(local)) {
            P.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Unable to read " + local + ": " + e.getMessage());
        }
    }

    private static void loadEnvironmentOverrides() {
        override("db.url", "SMARTSTUDY_DB_URL");
        override("db.user", "SMARTSTUDY_DB_USER");
        override("db.password", "SMARTSTUDY_DB_PASSWORD");
    }

    private static void override(String property, String environmentVariable) {
        String value = System.getenv(environmentVariable);
        if (value != null && !value.isBlank()) {
            P.setProperty(property, value.trim());
        }
    }

    public static String get(String key) {
        return P.getProperty(key, "").trim();
    }

    public static int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(get(key));
        } catch (Exception e) {
            return fallback;
        }
    }

    public static Path syllabusStorage() {
        String configured = get("syllabus.storage");
        if (configured.isBlank()) {
            configured = "data/syllabi";
        }
        return Path.of(configured).toAbsolutePath().normalize();
    }
}
