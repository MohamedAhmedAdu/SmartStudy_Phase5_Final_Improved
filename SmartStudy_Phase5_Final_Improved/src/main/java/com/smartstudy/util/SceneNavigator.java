package com.smartstudy.util;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
public final class SceneNavigator {
    private static Stage stage;
    private SceneNavigator() {}
    public static void initialize(Stage primaryStage) { stage = primaryStage; stage.setTitle("SmartStudy"); stage.setMinWidth(1000); stage.setMinHeight(650); }
    public static void showLogin() { show("/fxml/login.fxml", 1050, 700); }
    public static void showRegister() { show("/fxml/register.fxml", 1050, 700); }
    public static void showMain() { show("/fxml/main.fxml", 1250, 780); }
    private static void show(String resource, double w, double h) {
        try {
            Parent root = FXMLLoader.load(SceneNavigator.class.getResource(resource));
            Scene scene = new Scene(root, w, h); scene.getStylesheets().add(SceneNavigator.class.getResource("/css/app.css").toExternalForm());
            stage.setScene(scene); stage.centerOnScreen(); stage.show();
        } catch (IOException e) { throw new IllegalStateException("Unable to load " + resource, e); }
    }
}
