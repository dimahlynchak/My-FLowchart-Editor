package com.example.myflowcharteditor.model;

import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.control.TextField;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ElementFactory {
    public static DiagramElement createElement(String type, double x, double y) {
        switch (type) {
            case "Rectangle":
                Rectangle rectangle = new Rectangle(x, y, 100, 50);
                rectangle.setFill(Color.WHITE);
                rectangle.setStroke(Color.BLACK);
                return new DiagramShape(rectangle);
            case "Circle":
                Circle circle = new Circle(x, y, 50);
                circle.setFill(Color.WHITE);
                circle.setStroke(Color.BLACK);
                return new DiagramShape(circle);
            case "Ellipse":
                Ellipse ellipse = new Ellipse(x, y, 50, 30);
                ellipse.setFill(Color.WHITE);
                ellipse.setStroke(Color.BLACK);
                return new DiagramShape(ellipse);
            case "Line":
                Polyline polyline = new Polyline();
                polyline.getPoints().addAll(x, y, x + 50, y, x + 100, y, x + 150, y);
                polyline.setStroke(Color.BLACK);
                polyline.setStrokeWidth(1);
                return new DiagramShape(polyline);
            case "Arrow":
                Polyline arrowLine = new Polyline(x, y, x + 50, y, x + 100, y, x + 150, y);
                arrowLine.setStroke(Color.BLACK);
                arrowLine.setStrokeWidth(1);
                Polygon arrowHead = new Polygon();
                arrowHead.getPoints().addAll(x + 150.0, y - 5.0, x + 160.0, y, x + 150.0, y + 5.0);
                arrowHead.setFill(Color.BLACK);
                return new DiagramArrow(new Group(arrowLine, arrowHead), arrowLine, arrowHead);
            case "Triangle":
                Polygon triangle = new Polygon();
                triangle.getPoints().addAll(
                        50.0, 0.0,
                        0.0, 100.0,
                        100.0, 100.0
                );
                triangle.setFill(Color.WHITE);
                triangle.setStroke(Color.BLACK);
                triangle.setLayoutX(x);
                triangle.setLayoutY(y);
                return new DiagramShape(triangle);
            case "Process":
                return createImageViewElement("process.png", x, y);
            case "Decision":
                Polygon decision = new Polygon();
                decision.getPoints().addAll(
                        50.0, 0.0,
                        100.0, 50.0,
                        50.0, 100.0,
                        0.0, 50.0
                );
                decision.setFill(Color.WHITE);
                decision.setStroke(Color.BLACK);
                decision.setLayoutX(x);
                decision.setLayoutY(y);
                return new DiagramShape(decision);
            case "Step":
                return createImageViewElement("step.png", x, y);
            case "Document":
                return createImageViewElement("document.png", x, y);
            case "Input/Output":
                Polygon io = new Polygon();
                io.getPoints().addAll(
                        0.0, 0.0,
                        80.0, 0.0,
                        100.0, 50.0,
                        20.0, 50.0
                );
                io.setFill(Color.WHITE);
                io.setStroke(Color.BLACK);
                io.setLayoutX(x);
                io.setLayoutY(y);
                return new DiagramShape(io);
            case "Storage":
                return createImageViewElement("storage.png", x, y);
            case "Actor":
                return createImageViewElement("actor.png", x, y);
            case "Text":
                TextField textField = new TextField("text");
                textField.setStyle("-fx-background-color: transparent; -fx-text-fill: black;");
                textField.setPrefWidth(100);  // Adjust as needed
                Pane textPane = new Pane(textField);
                textPane.setLayoutX(x);
                textPane.setLayoutY(y);
                return new DiagramShape(textPane);
            case "Image":
                throw new IllegalArgumentException("Image type requires a file path");
            default:
                throw new IllegalArgumentException("Unsupported element type: " + type);
        }
    }

    public static DiagramElement createImageElement(String imagePath, double x, double y) {
        try {
            FileInputStream input = new FileInputStream(imagePath);
            Image image = new Image(input);
            ImageView imageView = new ImageView(image);
            imageView.setX(x);
            imageView.setY(y);
            imageView.setFitWidth(100);
            imageView.setFitHeight(60);
            return new DiagramShape(imageView);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static DiagramElement createImageViewElement(String imagePath, double x, double y) {
        ImageView imageView = createImageView(imagePath);
        imageView.setX(x);
        imageView.setY(y);
        return new DiagramShape(imageView);
    }

    private static ImageView createImageView(String imagePath) {
        String fullPath = "/com/example/flowchartEditor/" + imagePath;
        Image image = new Image(ElementFactory.class.getResourceAsStream(fullPath));
        if (image.isError()) {
            throw new IllegalArgumentException("Error loading image: " + fullPath);
        }
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(100);
        imageView.setFitHeight(60);
        return imageView;
    }
}
