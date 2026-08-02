package com.smartstudy;

import com.smartstudy.util.SceneNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

public final class SmartStudyApp extends Application {
    @Override public void start(Stage stage) {
        SceneNavigator.initialize(stage);
        SceneNavigator.showLogin();
    }
    public static void main(String[] args) { launch(args); }
}
