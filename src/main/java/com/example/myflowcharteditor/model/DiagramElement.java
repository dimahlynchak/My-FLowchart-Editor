package com.example.myflowcharteditor.model;

import javafx.scene.Node;

import java.io.Serializable;

public interface DiagramElement extends Serializable {
    Node getNode();
}
