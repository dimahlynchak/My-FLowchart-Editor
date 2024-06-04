package com.example.myflowcharteditor.utillity;

import com.example.myflowcharteditor.controller.DiagramController;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

public class ResizeBox extends Pane {
    private final Node node;
    private final Line topLine, rightLine, bottomLine, leftLine;
    private final Circle topLeft, topRight, bottomLeft, bottomRight;
    private final int gridGap = 10;
    private double startX, startY;
    private boolean resizing = false;
    private final DiagramController controller;
    private boolean locked = false;  // Замкнення елемента
    private final Color lockedColor = Color.RED;
    private final Color unlockedColor = Color.BLUE;

    public ResizeBox(Node node, DiagramController controller) {
        this.node = node;
        this.controller = controller;
        double nodeWidth = node.getBoundsInParent().getWidth();
        double nodeHeight = node.getBoundsInParent().getHeight();

        this.setLayoutX(node.getBoundsInParent().getMinX());
        this.setLayoutY(node.getBoundsInParent().getMinY());
        this.setPrefWidth(nodeWidth);
        this.setPrefHeight(nodeHeight);

        topLine = new Line(0, 0, nodeWidth, 0);
        rightLine = new Line(nodeWidth, 0, nodeWidth, nodeHeight);
        bottomLine = new Line(nodeWidth, nodeHeight, 0, nodeHeight);
        leftLine = new Line(0, nodeHeight, 0, 0);

        topLine.getStrokeDashArray().addAll(5.0, 5.0);
        rightLine.getStrokeDashArray().addAll(5.0, 5.0);
        bottomLine.getStrokeDashArray().addAll(5.0, 5.0);
        leftLine.getStrokeDashArray().addAll(5.0, 5.0);

        topLeft = new Circle(5, unlockedColor);
        topRight = new Circle(5, unlockedColor);
        bottomLeft = new Circle(5, unlockedColor);
        bottomRight = new Circle(5, unlockedColor);

        updateHandles(nodeWidth, nodeHeight);

        this.getChildren().addAll(topLine, rightLine, bottomLine, leftLine, topLeft, topRight, bottomLeft, bottomRight);

        setListeners();
        makeDraggable(this);
        updateHandleColors();
    }

    public Node getNode() {
        return node;
    }

    public void lock() {
        locked = true;
        updateHandleColors();
    }

    public void unlock() {
        locked = false;
        updateHandleColors();
    }

    public boolean isLocked() {
        return locked;
    }

    public void updateHandleColors() {
        Color color = locked ? lockedColor : unlockedColor;
        topLine.setStroke(color);
        rightLine.setStroke(color);
        bottomLine.setStroke(color);
        leftLine.setStroke(color);
        topLeft.setFill(color);
        topRight.setFill(color);
        bottomLeft.setFill(color);
        bottomRight.setFill(color);
    }

    private void setListeners() {
        topLeft.setOnMousePressed(event -> resizing = !locked);
        topLeft.setOnMouseReleased(event -> resizing = false);
        topLeft.setOnMouseDragged(event -> {
            if (!locked && controller.getSelectedElements().contains(this)) {
                double offsetX = snapToGrid(event.getX());
                double offsetY = snapToGrid(event.getY());
                if (getPrefWidth() - offsetX > 20 && getPrefHeight() - offsetY > 20) {
                    setLayoutX(getLayoutX() + offsetX);
                    setLayoutY(getLayoutY() + offsetY);
                    setPrefWidth(getPrefWidth() - offsetX);
                    setPrefHeight(getPrefHeight() - offsetY);
                    updateNode();
                    updateHandles(getPrefWidth(), getPrefHeight());
                }
            }
        });

        topRight.setOnMousePressed(event -> resizing = !locked);
        topRight.setOnMouseReleased(event -> resizing = false);
        topRight.setOnMouseDragged(event -> {
            if (!locked && controller.getSelectedElements().contains(this)) {
                double offsetX = snapToGrid(event.getX());
                double offsetY = snapToGrid(event.getY());
                if (offsetX > 20 && getPrefHeight() - offsetY > 20) {
                    setLayoutY(getLayoutY() + offsetY);
                    setPrefWidth(offsetX);
                    setPrefHeight(getPrefHeight() - offsetY);
                    updateNode();
                    updateHandles(offsetX, getPrefHeight());
                }
            }
        });

        bottomLeft.setOnMousePressed(event -> resizing = !locked);
        bottomLeft.setOnMouseReleased(event -> resizing = false);
        bottomLeft.setOnMouseDragged(event -> {
            if (!locked && controller.getSelectedElements().contains(this)) {
                double offsetX = snapToGrid(event.getX());
                double offsetY = snapToGrid(event.getY());
                if (getPrefWidth() - offsetX > 20 && offsetY > 20) {
                    setLayoutX(getLayoutX() + offsetX);
                    setPrefWidth(getPrefWidth() - offsetX);
                    setPrefHeight(offsetY);
                    updateNode();
                    updateHandles(getPrefWidth(), offsetY);
                }
            }
        });

        bottomRight.setOnMousePressed(event -> resizing = !locked);
        bottomRight.setOnMouseReleased(event -> resizing = false);
        bottomRight.setOnMouseDragged(event -> {
            if (!locked && controller.getSelectedElements().contains(this)) {
                double offsetX = snapToGrid(event.getX());
                double offsetY = snapToGrid(event.getY());
                if (offsetX > 20 && offsetY > 20) {
                    setPrefWidth(offsetX);
                    setPrefHeight(offsetY);
                    updateNode();
                    updateHandles(offsetX, offsetY);
                }
            }
        });
    }

    public void updateNode() {
        if (node instanceof Rectangle rectangle) {
            rectangle.setX(getLayoutX());
            rectangle.setY(getLayoutY());
            rectangle.setWidth(getPrefWidth());
            rectangle.setHeight(getPrefHeight());
        } else if (node instanceof Circle circle) {
            circle.setCenterX(getLayoutX() + getPrefWidth() / 2);
            circle.setCenterY(getLayoutY() + getPrefHeight() / 2);
            circle.setRadius(Math.min(getPrefWidth(), getPrefHeight()) / 2);
        } else if (node instanceof Ellipse ellipse) {
            ellipse.setCenterX(getLayoutX() + getPrefWidth() / 2);
            ellipse.setCenterY(getLayoutY() + getPrefHeight() / 2);
            ellipse.setRadiusX(getPrefWidth() / 2);
            ellipse.setRadiusY(getPrefHeight() / 2);
        } else if (node instanceof Polygon polygon) {
            double scaleX = getPrefWidth() / polygon.getLayoutBounds().getWidth();
            double scaleY = getPrefHeight() / polygon.getLayoutBounds().getHeight();
            for (int i = 0; i < polygon.getPoints().size(); i += 2) {
                polygon.getPoints().set(i, polygon.getPoints().get(i) * scaleX);
                polygon.getPoints().set(i + 1, polygon.getPoints().get(i + 1) * scaleY);
            }
            polygon.setLayoutX(getLayoutX());
            polygon.setLayoutY(getLayoutY());
        } else if (node instanceof ImageView imageView) {
            imageView.setX(getLayoutX());
            imageView.setY(getLayoutY());
            imageView.setFitWidth(getPrefWidth());
            imageView.setFitHeight(getPrefHeight());
        } else if (node instanceof Pane pane) {
            pane.setLayoutX(getLayoutX());
            pane.setLayoutY(getLayoutY());
            pane.setPrefWidth(getPrefWidth());
            pane.setPrefHeight(getPrefHeight());
            if (!pane.getChildren().isEmpty() && pane.getChildren().get(0) instanceof TextField textField) {
                textField.setPrefWidth(getPrefWidth());
                textField.setPrefHeight(getPrefHeight());
            }
        }
    }

    private double snapToGrid(double value) {
        return Math.round(value / gridGap) * gridGap;
    }

    private void updateHandles(double newWidth, double newHeight) {
        topLine.setEndX(newWidth);
        rightLine.setStartX(newWidth);
        rightLine.setEndX(newWidth);
        rightLine.setEndY(newHeight);
        bottomLine.setStartY(newHeight);
        bottomLine.setEndX(newWidth);
        bottomLine.setEndY(newHeight);
        leftLine.setEndY(newHeight);

        topLeft.setCenterX(0);
        topLeft.setCenterY(0);
        topRight.setCenterX(newWidth);
        topRight.setCenterY(0);
        bottomLeft.setCenterX(0);
        bottomLeft.setCenterY(newHeight);
        bottomRight.setCenterX(newWidth);
        bottomRight.setCenterY(newHeight);

        topLine.setStartX(0);
        topLine.setEndX(newWidth);
        rightLine.setStartX(newWidth);
        rightLine.setEndX(newWidth);
        rightLine.setStartY(0);
        rightLine.setEndY(newHeight);
        bottomLine.setStartY(newHeight);
        bottomLine.setEndX(newWidth);
        bottomLine.setStartX(0);
        bottomLine.setEndY(newHeight);
        leftLine.setStartX(0);
        leftLine.setEndY(newHeight);
        leftLine.setStartY(0);
        leftLine.setEndX(0);
    }

    private void makeDraggable(Pane pane) {
        pane.setOnMousePressed(event -> {
            if (!locked && !resizing && controller.getSelectedElements().contains(this)) {
                startX = event.getSceneX() - pane.getLayoutX();
                startY = event.getSceneY() - pane.getLayoutY();
            }
        });

        pane.setOnMouseDragged(event -> {
            if (!locked && !resizing && controller.getSelectedElements().contains(this)) {
                double newX = snapToGrid(event.getSceneX() - startX);
                double newY = snapToGrid(event.getSceneY() - startY);
                controller.moveSelectedElements(newX - getLayoutX(), newY - getLayoutY());
            }
        });
    }

    public void move(double offsetX, double offsetY) {
        setLayoutX(getLayoutX() + offsetX);
        setLayoutY(getLayoutY() + offsetY);
        updateNode();
    }
}
