package com.example.myflowcharteditor.utillity;

import com.example.myflowcharteditor.controller.DiagramController;
import com.example.myflowcharteditor.model.DiagramArrow;
import com.example.myflowcharteditor.model.DiagramElement;
import com.example.myflowcharteditor.model.DiagramShape;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

public class LineHandles extends Pane {
    private final Polyline polyline;
    private final Polygon arrowHead;
    private final Circle startHandle, endHandle, midHandle1, midHandle2;
    private double startX, startY;
    private double accumulatedOffsetX, accumulatedOffsetY;
    private final int gridGap = 10;
    private final DiagramController controller;
    private final Group arrowGroup;
    private boolean locked = false;  // Замкнення елемента
    private final Color lockedColor = Color.RED;
    private final Color unlockedColor = Color.BLUE;

    public LineHandles(DiagramElement element, DiagramController controller) {
        this.controller = controller;

        if (element instanceof DiagramArrow diagramArrow) {
            this.polyline = diagramArrow.getLine();
            this.arrowHead = diagramArrow.getArrowHead();
            this.arrowGroup = (Group) diagramArrow.getNode();
        } else if (element instanceof DiagramShape diagramShape && diagramShape.getNode() instanceof Polyline) {
            this.polyline = (Polyline) diagramShape.getNode();
            this.arrowHead = null;
            this.arrowGroup = null;
        } else {
            throw new IllegalArgumentException("Unsupported element type");
        }

        startHandle = new Circle(5, unlockedColor);
        endHandle = new Circle(5, unlockedColor);
        midHandle1 = new Circle(5, unlockedColor);
        midHandle2 = new Circle(5, unlockedColor);

        updateHandles();

        this.getChildren().addAll(startHandle, endHandle, midHandle1, midHandle2);

        setListeners();
        updateHandleColors();
    }

    public Polyline getPolyline() {
        return polyline;
    }

    public Polygon getArrowHead() {
        return arrowHead;
    }

    public Group getArrowGroup() {
        return arrowGroup;
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
        startHandle.setFill(color);
        endHandle.setFill(color);
        midHandle1.setFill(color);
        midHandle2.setFill(color);
    }

    private void setListeners() {
        addMouseListener(startHandle, 0, true);
        addMouseListener(midHandle1, 1, false);
        addMouseListener(midHandle2, 2, false);
        addMouseListener(endHandle, 3, true);

        this.setOnMousePressed(event -> {
            if (!locked) {
                startX = event.getSceneX();
                startY = event.getSceneY();
                accumulatedOffsetX = 0;
                accumulatedOffsetY = 0;
            }
        });

        this.setOnMouseDragged(event -> {
            if (!locked && controller.getSelectedElements().contains(this)) {
                double offsetX = event.getSceneX() - startX;
                double offsetY = event.getSceneY() - startY;
                controller.moveSelectedElements(offsetX, offsetY);
                startX = event.getSceneX();
                startY = event.getSceneY();
            }
        });
    }

    private void addMouseListener(Circle handle, int handleIndex, boolean isEndHandle) {
        handle.setOnMousePressed(event -> {
            if (!locked) {
                startX = event.getSceneX();
                startY = event.getSceneY();
                accumulatedOffsetX = 0;
                accumulatedOffsetY = 0;
            }
        });

        handle.setOnMouseDragged(event -> {
            if (!locked && controller.getSelectedElements().contains(this)) {
                double offsetX = event.getSceneX() - startX;
                double offsetY = event.getSceneY() - startY;
                moveHandle(handleIndex, isEndHandle, offsetX, offsetY);
                startX = event.getSceneX();
                startY = event.getSceneY();
            }
        });
    }

    public void movePolyline(double offsetX, double offsetY) {
        accumulatedOffsetX += offsetX;
        accumulatedOffsetY += offsetY;
        double snapToGridOffsetX = snapToGrid(accumulatedOffsetX);
        double snapToGridOffsetY = snapToGrid(accumulatedOffsetY);
        if (Math.abs(snapToGridOffsetX) >= gridGap || Math.abs(snapToGridOffsetY) >= gridGap) {
            for (int i = 0; i < polyline.getPoints().size(); i += 2) {
                polyline.getPoints().set(i, polyline.getPoints().get(i) + snapToGridOffsetX);
                polyline.getPoints().set(i + 1, polyline.getPoints().get(i + 1) + snapToGridOffsetY);
            }
            accumulatedOffsetX -= snapToGridOffsetX;
            accumulatedOffsetY -= snapToGridOffsetY;
            updateHandles();
            if (arrowHead != null) {
                updateArrowHead();
            }
        }
    }

    private void moveHandle(int handleIndex, boolean isEndHandle, double offsetX, double offsetY) {
        accumulatedOffsetX += offsetX;
        accumulatedOffsetY += offsetY;
        double snapToGridOffsetX = snapToGrid(accumulatedOffsetX);
        double snapToGridOffsetY = snapToGrid(accumulatedOffsetY);

        if (Math.abs(snapToGridOffsetX) >= gridGap || Math.abs(snapToGridOffsetY) >= gridGap) {
            if (isStraightLine()) {
                if (isEndHandle) {
                    polyline.getPoints().set(handleIndex * 2, polyline.getPoints().get(handleIndex * 2) + snapToGridOffsetX);
                    polyline.getPoints().set(handleIndex * 2 + 1, polyline.getPoints().get(handleIndex * 2 + 1) + snapToGridOffsetY);

                    double startX = polyline.getPoints().get(0);
                    double startY = polyline.getPoints().get(1);
                    double endX = polyline.getPoints().get(6);
                    double endY = polyline.getPoints().get(7);

                    polyline.getPoints().set(2, startX + (endX - startX) / 3);
                    polyline.getPoints().set(3, startY + (endY - startY) / 3);
                    polyline.getPoints().set(4, startX + 2 * (endX - startX) / 3);
                    polyline.getPoints().set(5, startY + 2 * (endY - startY) / 3);
                } else {
                    polyline.getPoints().set(handleIndex * 2, polyline.getPoints().get(handleIndex * 2) + snapToGridOffsetX);
                    polyline.getPoints().set(handleIndex * 2 + 1, polyline.getPoints().get(handleIndex * 2 + 1) + snapToGridOffsetY);
                }
            } else {
                polyline.getPoints().set(handleIndex * 2, polyline.getPoints().get(handleIndex * 2) + snapToGridOffsetX);
                polyline.getPoints().set(handleIndex * 2 + 1, polyline.getPoints().get(handleIndex * 2 + 1) + snapToGridOffsetY);
            }

            accumulatedOffsetX -= snapToGridOffsetX;
            accumulatedOffsetY -= snapToGridOffsetY;
            updateHandles();
            if (arrowHead != null) {
                updateArrowHead();
            }
        }
    }

    private double snapToGrid(double value) {
        return Math.round(value / gridGap) * gridGap;
    }

    public void updateHandles() {
        startHandle.setCenterX(polyline.getPoints().get(0));
        startHandle.setCenterY(polyline.getPoints().get(1));

        endHandle.setCenterX(polyline.getPoints().get(6));
        endHandle.setCenterY(polyline.getPoints().get(7));

        midHandle1.setCenterX(polyline.getPoints().get(2));
        midHandle1.setCenterY(polyline.getPoints().get(3));

        midHandle2.setCenterX(polyline.getPoints().get(4));
        midHandle2.setCenterY(polyline.getPoints().get(5));
    }

    private void updateArrowHead() {
        double endX = polyline.getPoints().get(6);
        double endY = polyline.getPoints().get(7);
        double prevX = polyline.getPoints().get(4);
        double prevY = polyline.getPoints().get(5);
        double angle = Math.atan2(endY - prevY, endX - prevX);

        double arrowLength = 10;
        double arrowWidth = 5;

        double sin = Math.sin(angle);
        double cos = Math.cos(angle);

        double x1 = endX - arrowLength * cos + arrowWidth * sin;
        double y1 = endY - arrowLength * sin - arrowWidth * cos;

        double x2 = endX;
        double y2 = endY;

        double x3 = endX - arrowLength * cos - arrowWidth * sin;
        double y3 = endY - arrowLength * sin + arrowWidth * cos;

        arrowHead.getPoints().setAll(
                x1, y1,
                x2, y2,
                x3, y3
        );
    }

    private boolean isStraightLine() {
        double startX = polyline.getPoints().get(0);
        double startY = polyline.getPoints().get(1);
        double endX = polyline.getPoints().get(6);
        double endY = polyline.getPoints().get(7);

        for (int i = 2; i < polyline.getPoints().size() - 2; i += 2) {
            double midX = polyline.getPoints().get(i);
            double midY = polyline.getPoints().get(i + 1);

            if (!isPointOnLine(startX, startY, endX, endY, midX, midY)) {
                return false;
            }
        }
        return true;
    }

    private boolean isPointOnLine(double startX, double startY, double endX, double endY, double midX, double midY) {
        double crossProduct = (midY - startY) * (endX - startX) - (midX - startX) * (endY - startY);
        return Math.abs(crossProduct) < 1e-7;
    }
}
