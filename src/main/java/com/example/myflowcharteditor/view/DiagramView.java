package com.example.myflowcharteditor.view;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.util.Arrays;
import java.util.List;

public class DiagramView {
    private final BorderPane root;
    private final VBox leftPanel;
    private final Pane workArea;
    private final VBox topPanelContainer;
    private final HBox topPanel;
    private final HBox fileNameContainer;
    private final TextField fileNameField;
    private final Rectangle selectionRectangle;

    private Button zoomInButton;
    private Button zoomOutButton;
    private Label zoomLabel;
    private Button openButton;
    private Button saveAsPngButton;

    // Конструктор, що ініціалізує елементи інтерфейсу
    public DiagramView() {
        root = new BorderPane();

        topPanelContainer = new VBox(5);
        topPanelContainer.setStyle("-fx-padding: 5;");

        topPanel = new HBox(10);
        topPanel.setStyle("-fx-padding: 5;");

        openButton = new Button("Відкрити файл");
        saveAsPngButton = new Button("Зберегти");
        zoomOutButton = new Button("-");
        zoomLabel = new Label("100%");
        zoomInButton = new Button("+");

        topPanel.getChildren().addAll(openButton, saveAsPngButton, zoomOutButton, zoomLabel, zoomInButton);

        fileNameField = new TextField();
        fileNameField.setPromptText("Назва файлу");
        fileNameField.setMaxWidth(200);

        fileNameContainer = new HBox();
        fileNameContainer.setStyle("-fx-alignment: center;");
        fileNameContainer.getChildren().add(fileNameField);

        topPanelContainer.getChildren().addAll(fileNameContainer, topPanel);
        root.setTop(topPanelContainer);

        leftPanel = new VBox(10);
        leftPanel.setStyle("-fx-padding: 10; -fx-border-width: 1; -fx-border-color: black;");
        leftPanel.setMinHeight(600);
        leftPanel.setPrefWidth(200);  // Adjust the width as needed

        List<String> elementTypes = Arrays.asList("Text", "Rectangle", "Circle",
                "Ellipse", "Triangle", "Decision", "Input/Output", "Line", "Arrow",
                "Process", "Storage", "Step", "Document", "Actor", "Image");

        for (String type : elementTypes) {
            Button button = new Button(type);
            leftPanel.getChildren().add(button);
        }

        leftPanel.setStyle("-fx-alignment: top-left; -fx-padding: 10; -fx-spacing: 10;");

        workArea = new Pane();
        workArea.setStyle("-fx-background-color: white; -fx-border-color: black;");
        addGrid(workArea, 20, 100);
        workArea.setMinSize(2000, 2000);

        ScrollPane leftScrollPane = new ScrollPane(leftPanel);
        leftScrollPane.setFitToWidth(true);
        leftScrollPane.setPrefWidth(140);

        ScrollPane workAreaScrollPane = new ScrollPane(workArea);
        workAreaScrollPane.setFitToWidth(true);
        workAreaScrollPane.setFitToHeight(true);
        workAreaScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        workAreaScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        root.setLeft(leftScrollPane);
        root.setCenter(workAreaScrollPane);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(workArea.widthProperty());
        clip.heightProperty().bind(workArea.heightProperty());
        workArea.setClip(clip);

        workArea.widthProperty().addListener((obs, oldVal, newVal) -> addGrid(workArea, 20, 100));
        workArea.heightProperty().addListener((obs, oldVal, newVal) -> addGrid(workArea, 20, 100));

        leftScrollPane.setStyle("-fx-background: #f0f0f0; -fx-border-color: #d0d0d0; -fx-border-width: 1px;");
        workAreaScrollPane.setStyle("-fx-background: #f0f0f0; -fx-border-color: #d0d0d0; -fx-border-width: 1px;");

        workAreaScrollPane.setHvalue(0.5);
        workAreaScrollPane.setVvalue(0.5);

        selectionRectangle = new Rectangle();
        initializeSelectionRectangle();
    }

    public BorderPane getRoot() {
        return root;
    }

    public VBox getLeftPanel() {
        return leftPanel;
    }

    public Pane getWorkArea() {
        return workArea;
    }

    public Rectangle getSelectionRectangle() {
        return selectionRectangle;
    }

    public Button getZoomInButton() {
        return zoomInButton;
    }

    public Button getZoomOutButton() {
        return zoomOutButton;
    }

    public Label getZoomLabel() {
        return zoomLabel;
    }

    public HBox getFileNameContainer() {
        return fileNameContainer;
    }

    public VBox getTopPanelContainer() {
        return topPanelContainer;
    }

    public HBox getTopPanel() {
        return topPanel;
    }

    public TextField getFileNameField() {
        return fileNameField;
    }

    public Button getSaveAsPngButton() {
        return saveAsPngButton;
    }

    public Button getOpenButton() {
        return openButton;
    }

    private void addGrid(Pane pane, int minorGap, int majorGap) {
        pane.getChildren().removeIf(node -> node instanceof Line);
        double width = pane.getWidth();
        double height = pane.getHeight();

        for (int x = 0; x < width; x += minorGap) {
            Line line = new Line(x, 0, x, height);
            line.setStroke(Color.LIGHTGRAY);
            line.getStrokeDashArray().addAll(2.0, 2.0);
            pane.getChildren().add(line);
        }

        for (int y = 0; y < height; y += minorGap) {
            Line line = new Line(0, y, width, y);
            line.setStroke(Color.LIGHTGRAY);
            line.getStrokeDashArray().addAll(2.0, 2.0);
            pane.getChildren().add(line);
        }

        for (int x = 0; x < width; x += majorGap) {
            Line line = new Line(x, 0, x, height);
            line.setStroke(Color.DARKGRAY);
            line.setStrokeWidth(1);
            pane.getChildren().add(line);
        }

        for (int y = 0; y < height; y += majorGap) {
            Line line = new Line(0, y, width, y);
            line.setStroke(Color.DARKGRAY);
            line.setStrokeWidth(1);
            pane.getChildren().add(line);
        }
    }

    private void initializeSelectionRectangle() {
        selectionRectangle.setFill(Color.TRANSPARENT);
        selectionRectangle.setStroke(Color.BLACK);
        selectionRectangle.getStrokeDashArray().addAll(5.0, 5.0);
        workArea.getChildren().add(selectionRectangle);
        selectionRectangle.setVisible(false);
    }

    public void setGridVisible(boolean visible) {
        for (Node node : workArea.getChildren()) {
            if (node instanceof Line) {
                node.setVisible(visible);
            }
        }
    }
}
