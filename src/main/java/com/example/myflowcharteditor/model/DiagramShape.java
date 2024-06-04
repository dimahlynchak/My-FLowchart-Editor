package com.example.myflowcharteditor.model;

import javafx.scene.Node;

public class DiagramShape implements DiagramElement {
    private final transient Node node;  // Node не є Serializable

    public DiagramShape(Node node) {
        this.node = node;
    }

    @Override
    public Node getNode() {
        return node;
    }
}
