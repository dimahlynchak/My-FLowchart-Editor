package com.example.myflowcharteditor.model;

import com.example.myflowcharteditor.controller.DiagramController;
import com.example.myflowcharteditor.utillity.LineHandles;
import com.example.myflowcharteditor.utillity.ResizeBox;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DiagramModel {
    private final Pane workArea;
    private List<DiagramElement> clipboard;
    private DiagramController controller;

    public DiagramModel(Pane workArea) {
        this.workArea = workArea;
        this.clipboard = new ArrayList<>();
    }

    public void deleteSelectedElements(List<Object> selectedElements) {
        List<Node> nodesToRemove = new ArrayList<>();
        for (Object element : selectedElements) {
            if (element instanceof ResizeBox resizeBox) {
                if (resizeBox.isLocked()) {
                    continue;
                }
                nodesToRemove.add(resizeBox.getNode());
                nodesToRemove.add(resizeBox);
            } else if (element instanceof LineHandles lineHandles) {
                if (lineHandles.isLocked()) {
                    continue;
                }
                if (lineHandles.getArrowGroup() != null) {
                    nodesToRemove.add(lineHandles.getArrowGroup());
                } else {
                    nodesToRemove.add(lineHandles.getPolyline());
                    if (lineHandles.getArrowHead() != null) {
                        nodesToRemove.add(lineHandles.getArrowHead());
                    }
                }
                nodesToRemove.add(lineHandles);
            }
        }
        workArea.getChildren().removeAll(nodesToRemove);
        selectedElements.clear();
    }

    public void copySelectedElements(List<Object> selectedElements, Map<Node, String> elementTypesMap) {
        clipboard.clear();
        for (Object element : selectedElements) {
            if (element instanceof ResizeBox resizeBox) {
                if (resizeBox.isLocked()) {
                    continue;
                }
                Node node = resizeBox.getNode();
                String elementType = elementTypesMap.get(node);
                DiagramElement newElement = null;
                if (node instanceof Rectangle rectangle) {
                    Rectangle newRectangle = new Rectangle(rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());
                    newRectangle.setFill(rectangle.getFill());
                    newRectangle.setStroke(rectangle.getStroke());
                    newElement = new DiagramShape(newRectangle);
                } else if (node instanceof Circle circle) {
                    Circle newCircle = new Circle(circle.getCenterX(), circle.getCenterY(), circle.getRadius());
                    newCircle.setFill(circle.getFill());
                    newCircle.setStroke(circle.getStroke());
                    newElement = new DiagramShape(newCircle);
                } else if (node instanceof Ellipse ellipse) {
                    Ellipse newEllipse = new Ellipse(ellipse.getCenterX(), ellipse.getCenterY(), ellipse.getRadiusX(), ellipse.getRadiusY());
                    newEllipse.setFill(ellipse.getFill());
                    newEllipse.setStroke(ellipse.getStroke());
                    newElement = new DiagramShape(newEllipse);
                } else if (node instanceof Polygon polygon) {
                    Polygon newPolygon = new Polygon();
                    newPolygon.getPoints().addAll(polygon.getPoints());
                    newPolygon.setFill(polygon.getFill());
                    newPolygon.setStroke(polygon.getStroke());
                    newPolygon.setLayoutX(polygon.getLayoutX());
                    newPolygon.setLayoutY(polygon.getLayoutY());
                    newElement = new DiagramShape(newPolygon);
                } else if (node instanceof ImageView imageView) {
                    ImageView newImageView = new ImageView(imageView.getImage());
                    newImageView.setX(imageView.getX());
                    newImageView.setY(imageView.getY());
                    newImageView.setFitWidth(imageView.getFitWidth());
                    newImageView.setFitHeight(imageView.getFitHeight());
                    newElement = new DiagramShape(newImageView);
                } else if (node instanceof Pane pane) {
                    TextField textField = (TextField) pane.getChildren().get(0);
                    TextField newTextField = new TextField(textField.getText());
                    newTextField.setStyle(textField.getStyle());
                    newTextField.setPrefWidth(textField.getPrefWidth());
                    Pane newTextPane = new Pane(newTextField);
                    newTextPane.setLayoutX(pane.getLayoutX());
                    newTextPane.setLayoutY(pane.getLayoutY());
                    newTextPane.setPrefWidth(pane.getPrefWidth());
                    newTextPane.setPrefHeight(pane.getPrefHeight());
                    newElement = new DiagramShape(newTextPane);
                }
                clipboard.add(newElement);
                elementTypesMap.put(newElement.getNode(), elementType);
            } else if (element instanceof LineHandles lineHandles) {
                if (lineHandles.isLocked()) {
                    continue;
                }
                Node node = lineHandles.getPolyline();
                String elementType = elementTypesMap.get(node);

                if (lineHandles.getArrowHead() != null) {
                    Polyline originalLine = lineHandles.getPolyline();
                    Polygon originalArrowHead = lineHandles.getArrowHead();

                    Polyline newLine = new Polyline();
                    newLine.getPoints().addAll(originalLine.getPoints());
                    newLine.setStroke(originalLine.getStroke());
                    newLine.setStrokeWidth(originalLine.getStrokeWidth());

                    Polygon newArrowHead = new Polygon();
                    newArrowHead.getPoints().addAll(originalArrowHead.getPoints());
                    newArrowHead.setFill(originalArrowHead.getFill());

                    Group newGroup = new Group(newLine, newArrowHead);
                    DiagramArrow newDiagramArrow = new DiagramArrow(newGroup, newLine, newArrowHead);
                    clipboard.add(newDiagramArrow);
                    elementTypesMap.put(newDiagramArrow.getNode(), "Arrow");
                } else {
                    Polyline newPolyline = new Polyline();
                    newPolyline.getPoints().addAll(((Polyline) node).getPoints());
                    newPolyline.setStroke(((Polyline) node).getStroke());
                    newPolyline.setStrokeWidth(((Polyline) node).getStrokeWidth());
                    clipboard.add(new DiagramShape(newPolyline));
                    elementTypesMap.put(newPolyline, "Line");
                }
            }
        }
    }

    public List<DiagramElement> pasteElements() {
        List<DiagramElement> newElements = new ArrayList<>();
        double offsetX = 20;
        double offsetY = 20;

        for (DiagramElement element : clipboard) {
            DiagramElement newElement = null;

            if (element instanceof DiagramArrow diagramArrow) {
                Polyline originalLine = diagramArrow.getLine();
                Polygon originalArrowHead = diagramArrow.getArrowHead();

                Polyline newLine = createNewPolyline(originalLine, offsetX, offsetY);

                Polygon newArrowHead = new Polygon();
                newArrowHead.getPoints().clear();
                List<Double> newArrowHeadPoints = new ArrayList<>();
                for (int i = 0; i < originalArrowHead.getPoints().size(); i += 2) {
                    newArrowHeadPoints.add(originalArrowHead.getPoints().get(i) + offsetX);
                    newArrowHeadPoints.add(originalArrowHead.getPoints().get(i + 1) + offsetY);
                }
                newArrowHead.getPoints().addAll(newArrowHeadPoints);

                Group newGroup = new Group(newLine, newArrowHead);
                newElement = new DiagramArrow(newGroup, newLine, newArrowHead);
            } else if (element instanceof DiagramShape diagramShape) {
                Node node = diagramShape.getNode();
                if (node instanceof Rectangle rectangle) {
                    Rectangle newRectangle = new Rectangle(rectangle.getX() + offsetX, rectangle.getY() + offsetY, rectangle.getWidth(), rectangle.getHeight());
                    newRectangle.setFill(rectangle.getFill());
                    newRectangle.setStroke(rectangle.getStroke());
                    newElement = new DiagramShape(newRectangle);
                } else if (node instanceof Circle circle) {
                    Circle newCircle = new Circle(circle.getCenterX() + offsetX, circle.getCenterY() + offsetY, circle.getRadius());
                    newCircle.setFill(circle.getFill());
                    newCircle.setStroke(circle.getStroke());
                    newElement = new DiagramShape(newCircle);
                } else if (node instanceof Ellipse ellipse) {
                    Ellipse newEllipse = new Ellipse(ellipse.getCenterX() + offsetX, ellipse.getCenterY() + offsetY, ellipse.getRadiusX(), ellipse.getRadiusY());
                    newEllipse.setFill(ellipse.getFill());
                    newEllipse.setStroke(ellipse.getStroke());
                    newElement = new DiagramShape(newEllipse);
                } else if (node instanceof Polygon polygon) {
                    Polygon newPolygon = new Polygon();
                    for (int i = 0; i < polygon.getPoints().size(); i += 2) {
                        newPolygon.getPoints().addAll(polygon.getPoints().get(i) + offsetX, polygon.getPoints().get(i + 1) + offsetY);
                    }
                    newPolygon.setFill(polygon.getFill());
                    newPolygon.setStroke(polygon.getStroke());
                    newPolygon.setLayoutX(polygon.getLayoutX() + offsetX);
                    newPolygon.setLayoutY(polygon.getLayoutY() + offsetY);
                    newElement = new DiagramShape(newPolygon);
                } else if (node instanceof ImageView imageView) {
                    ImageView newImageView = new ImageView(imageView.getImage());
                    newImageView.setX(imageView.getX() + offsetX);
                    newImageView.setY(imageView.getY() + offsetY);
                    newImageView.setFitWidth(imageView.getFitWidth());
                    newImageView.setFitHeight(imageView.getFitHeight());
                    newElement = new DiagramShape(newImageView);
                } else if (node instanceof Pane pane) {
                    TextField textField = (TextField) pane.getChildren().get(0);
                    TextField newTextField = new TextField(textField.getText());
                    newTextField.setStyle(textField.getStyle());
                    newTextField.setPrefWidth(textField.getPrefWidth());
                    Pane newTextPane = new Pane(newTextField);
                    newTextPane.setLayoutX(pane.getLayoutX() + offsetX);
                    newTextPane.setLayoutY(pane.getLayoutY() + offsetY);
                    newTextPane.setPrefWidth(pane.getPrefWidth());
                    newTextPane.setPrefHeight(pane.getPrefHeight());
                    newElement = new DiagramShape(newTextPane);
                } else if (node instanceof Polyline originalPolyline) {
                    Polyline newPolyline = createNewPolyline(originalPolyline, offsetX, offsetY);
                    newElement = new DiagramShape(newPolyline);
                }
            }

            if (newElement != null) {
                newElements.add(newElement);
            }

            offsetX += 50;
            offsetY += 50;
        }

        return newElements;
    }

    private Polyline createNewPolyline(Polyline originalLine, double offsetX, double offsetY) {
        Polyline newLine = new Polyline();
        newLine.getPoints().clear();
        List<Double> newLinePoints = new ArrayList<>();
        for (int i = 0; i < originalLine.getPoints().size(); i += 2) {
            newLinePoints.add(originalLine.getPoints().get(i) + offsetX);
            newLinePoints.add(originalLine.getPoints().get(i + 1) + offsetY);
        }
        newLine.getPoints().addAll(newLinePoints);
        newLine.setStroke(originalLine.getStroke());
        newLine.setStrokeWidth(originalLine.getStrokeWidth());
        return newLine;
    }

    public void cutSelectedElements(List<Object> selectedElements, Map<Node, String> elementTypesMap) {
        copySelectedElements(selectedElements, elementTypesMap);
        deleteSelectedElements(selectedElements);
    }

    public void lockSelectedElements(List<Object> selectedElements) {
        for (Object element : selectedElements) {
            if (element instanceof ResizeBox) {
                ((ResizeBox) element).lock();
            } else if (element instanceof LineHandles) {
                ((LineHandles) element).lock();
            }
        }
    }

    public void unlockSelectedElements(List<Object> selectedElements) {
        for (Object element : selectedElements) {
            if (element instanceof ResizeBox) {
                ((ResizeBox) element).unlock();
            } else if (element instanceof LineHandles) {
                ((LineHandles) element).unlock();
            }
        }
    }

    public void loadFromFile(File file) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            List<DiagramElement> elements = (List<DiagramElement>) in.readObject();
            addElementsToWorkArea(elements);
            System.out.println("Diagram loaded from " + file.getName());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void addElementsToWorkArea(List<DiagramElement> elements) {
        for (DiagramElement element : elements) {
            Node node = element.getNode();
            workArea.getChildren().add(node);
            if (element instanceof DiagramArrow) {
                LineHandles lineHandles = new LineHandles(element, controller);
                workArea.getChildren().add(lineHandles);
                controller.getSelectedElements().add(lineHandles);
            } else if (element instanceof DiagramShape) {
                ResizeBox resizeBox = new ResizeBox(node, controller);
                workArea.getChildren().add(resizeBox);
                controller.getSelectedElements().add(resizeBox);
            }
            controller.getElementTypesMap().put(node, element.getClass().getSimpleName());
        }
    }

}
