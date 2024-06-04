package com.example.myflowcharteditor;

import com.example.myflowcharteditor.controller.DiagramController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Редактор блок-схем");

        DiagramController controller = new DiagramController();
        Scene scene = new Scene(controller.getView().getRoot(), 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
