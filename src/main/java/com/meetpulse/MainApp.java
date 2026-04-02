package com.meetpulse;

import com.meetpulse.ui.MeetPulseUI;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        MeetPulseUI ui    = new MeetPulseUI();
        VBox        root  = ui.buildRoot();
        Scene       scene = new Scene(root, 820, 660);

        // inline CSS — no external file dependency
        scene.getRoot().setStyle("-fx-background-color: #0b0e17;");

        // global scrollbar style
        scene.getStylesheets().add(
                "data:text/css," +
                        ".scroll-bar>.thumb{-fx-background-color:#1f2d47;-fx-background-radius:4;}" +
                        ".scroll-bar>.track{-fx-background-color:transparent;}" +
                        ".scroll-bar>.increment-button,.scroll-bar>.decrement-button" +
                        "{-fx-background-color:transparent;-fx-padding:0;}" +
                        ".text-area>.scroll-pane{-fx-background-color:#080c14;}" +
                        ".text-area>.scroll-pane>.viewport{-fx-background-color:#080c14;}"
        );

        stage.setTitle("MeetPulse — Audio Intelligence");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(740);
        stage.setMinHeight(600);

        // clean shutdown
        stage.setOnCloseRequest(e -> System.exit(0));

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}