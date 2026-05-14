package com.meetpulse;

import com.meetpulse.service.PreferencesManager;
import com.meetpulse.ui.MeetPulseUI;
import com.meetpulse.ui.ThemeManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        PreferencesManager prefs = new PreferencesManager();
        ThemeManager.setTheme(prefs.getTheme());

        MeetPulseUI ui = new MeetPulseUI();
        VBox root = ui.buildRoot();

        ThemeManager.ThemeColors colors = ThemeManager.getColors();
        Scene scene = new Scene(root, 900, 780);

        scene.getRoot().setStyle("-fx-background-color: " + colors.bg + ";");

        scene.getStylesheets().add(
                "data:text/css," +
                        ".scroll-bar>.thumb{-fx-background-color:" + colors.scrollbarThumb + ";-fx-background-radius:4;}" +
                        ".scroll-bar>.track{-fx-background-color:" + colors.scrollbarTrack + ";}" +
                        ".scroll-bar>.increment-button,.scroll-bar>.decrement-button" +
                        "{-fx-background-color:transparent;-fx-padding:0;}" +
                        ".text-area>.scroll-pane{-fx-background-color:" + colors.surface2 + ";}" +
                        ".text-area>.scroll-pane>.viewport{-fx-background-color:" + colors.surface2 + ";}" +
                        ".slider>.thumb{-fx-background-color:" + colors.accent + ";}" +
                        ".slider>.track{-fx-background-color:" + colors.surface2 + ";}"
        );

        stage.setTitle("MeetPulse — Audio Intelligence");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(800);
        stage.setMinHeight(700);

        stage.setOnCloseRequest(e -> {
            prefs.savePreferences();
            System.exit(0);
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}