package com.example.myflowcharteditor.model;

import javafx.scene.Group;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;

public class DiagramArrow implements DiagramElement {
    private final transient Group group; // Group не є Serializable
    private final Polyline line;
    private final Polygon arrowHead;

    public DiagramArrow(Group group, Polyline line, Polygon arrowHead) {
        this.group = group;
        this.line = line;
        this.arrowHead = arrowHead;
    }

    @Override
    public javafx.scene.Node getNode() {
        return group;
    }

    public Polyline getLine() {
        return line;
    }

    public Polygon getArrowHead() {
        return arrowHead;
    }
}
