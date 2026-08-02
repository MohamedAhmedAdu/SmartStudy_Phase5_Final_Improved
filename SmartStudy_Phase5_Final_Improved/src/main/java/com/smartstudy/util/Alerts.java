package com.smartstudy.util;
import javafx.scene.control.Alert;
public final class Alerts {
    private Alerts() {}
    public static void info(String title, String message) { show(Alert.AlertType.INFORMATION, title, message); }
    public static void error(String title, String message) { show(Alert.AlertType.ERROR, title, message); }
    public static boolean confirm(String title, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message); a.setTitle(title); a.setHeaderText(null);
        return a.showAndWait().filter(b -> b.getButtonData().isDefaultButton()).isPresent();
    }
    private static void show(Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type, message); a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}
