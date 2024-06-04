package com.example.myflowcharteditor.controller;

import com.example.myflowcharteditor.model.*;
import com.example.myflowcharteditor.utillity.LineHandles;
import com.example.myflowcharteditor.utillity.ResizeBox;
import com.example.myflowcharteditor.view.DiagramView;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiagramController {
    private final DiagramView view;
    private final DiagramModel model;
    private double initialMouseX, initialMouseY;

    private final List<ResizeBox> selectedResizeBoxes = new ArrayList<>();
    private final List<LineHandles> selectedLineHandlesList = new ArrayList<>();
    private final List<Object> selectedElements = new ArrayList<>();

    private final Map<Node, String> elementTypesMap = new HashMap<>();

    private ContextMenu contextMenu;

    private double zoomScale = 1.0;
    private final double maxZoomScale = 2.0;
    private final double minZoomScale = 0.5;

    private ComboBox<String> fontStyleComboBox;
    private ComboBox<Integer> fontSizeComboBox;
    private TextField lastSelectedTextField;

    public DiagramController() {
        this.view = new DiagramView();
        this.model = new DiagramModel(view.getWorkArea());
        initializeContextMenu();
        setupEventHandlers();
        initializeFontControls();
        setupFileHandlers();

        Button saveAsPngButton = view.getSaveAsPngButton();
        saveAsPngButton.setOnAction(event -> saveAsPng());
    }

    private void initializeContextMenu() {
        contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Видалити");
        deleteItem.setOnAction(event -> deleteSelectedElements());

        MenuItem copyItem = new MenuItem("Копіювати");
        copyItem.setOnAction(event -> copySelectedElements());

        MenuItem pasteItem = new MenuItem("Вставити");
        pasteItem.setOnAction(event -> pasteElements());

        MenuItem cutItem = new MenuItem("Вирізати");
        cutItem.setOnAction(event -> cutSelectedElements());

        MenuItem lockItem = new MenuItem("Замкнути");
        lockItem.setOnAction(event -> lockSelectedElements());

        MenuItem unlockItem = new MenuItem("Відімкнути");
        unlockItem.setOnAction(event -> unlockSelectedElements());

        contextMenu.getItems().addAll(deleteItem, copyItem, pasteItem, cutItem, lockItem, unlockItem);
    }

    private void setupEventHandlers() {
        view.getWorkArea().setOnContextMenuRequested(event -> {
            contextMenu.show(view.getWorkArea(), event.getScreenX(), event.getScreenY());
            event.consume();
        });

        view.getWorkArea().setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                contextMenu.hide();
                if (!event.getTarget().equals(fontStyleComboBox) && !event.getTarget().equals(fontSizeComboBox)) {
                    switchToDefaultControls();
                    lastSelectedTextField = null;
                }
            }
        });

        view.getWorkArea().setOnMouseEntered(event -> view.getWorkArea().requestFocus());

        view.getWorkArea().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.BACK_SPACE) {
                deleteSelectedElements();
            } else if (event.getCode() == KeyCode.C && event.isShortcutDown()) {
                copySelectedElements();
            } else if (event.getCode() == KeyCode.V && event.isShortcutDown()) {
                pasteElements();
            } else if (event.getCode() == KeyCode.X && event.isShortcutDown()) {
                cutSelectedElements();
            }
        });

        view.getWorkArea().setOnDragOver(event -> {
            if (event.getGestureSource() != view.getWorkArea() && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        view.getWorkArea().setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                String shapeType = db.getString();
                addShapeToWorkArea(shapeType, event.getX(), event.getY());
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        view.getWorkArea().addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getTarget() == view.getWorkArea()) {
                clearSelection();

                initialMouseX = event.getX();
                initialMouseY = event.getY();
                view.getSelectionRectangle().setX(initialMouseX);
                view.getSelectionRectangle().setY(initialMouseY);
                view.getSelectionRectangle().setWidth(0);
                view.getSelectionRectangle().setHeight(0);
                view.getSelectionRectangle().setVisible(true);
            }
        });

        view.getWorkArea().addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (view.getSelectionRectangle().isVisible()) {
                double endX = event.getX();
                double endY = event.getY();
                updateSelectionRectangle(initialMouseX, initialMouseY, endX, endY);
            }
        });

        view.getWorkArea().addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (view.getSelectionRectangle().isVisible()) {
                selectElementsWithinRectangle();
                view.getSelectionRectangle().setVisible(false);
            }
        });

        for (Node node : view.getLeftPanel().getChildren()) {
            if (node instanceof Button button) {
                button.setOnDragDetected(event -> {
                    Dragboard db = button.startDragAndDrop(TransferMode.ANY);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(button.getText());
                    db.setContent(content);
                    event.consume();
                });
                button.setOnAction(event -> {
                    if (button.getText().equals("Image")) {
                        addImageToWorkArea();
                    } else {
                        addElementToCenter(button.getText());
                    }
                });
            }
        }

        view.getZoomInButton().setOnAction(event -> zoomIn());
        view.getZoomOutButton().setOnAction(event -> zoomOut());

        view.getWorkArea().setOnZoom(event -> {
            double zoomFactor = event.getZoomFactor();
            if (zoomFactor > 1.05) {
                zoomIn();
            } else if (zoomFactor < 0.95) {
                zoomOut();
            }
            event.consume();
        });

        view.getWorkArea().setOnScroll(event -> {
            if (event.isControlDown()) {
                double deltaY = event.getDeltaY();
                if (deltaY > 10) {
                    zoomIn();
                } else if (deltaY < -10) {
                    zoomOut();
                }
                event.consume();
            }
        });

        view.getFileNameField().setOnMouseClicked(event -> {
            if (lastSelectedTextField != null) {
                lastSelectedTextField.requestFocus();
                lastSelectedTextField.selectAll();
            }
        });

        view.getFileNameField().setOnKeyPressed(event -> {
            if (lastSelectedTextField != null && !view.getFileNameField().isFocused()) {
                lastSelectedTextField.requestFocus();
                lastSelectedTextField.selectAll();
            }
        });
    }

    private void initializeFontControls() {
        fontStyleComboBox = new ComboBox<>();
        fontStyleComboBox.getItems().addAll("Arial", "Courier New", "Georgia", "Helvetica", "Times New Roman", "Verdana");
        fontStyleComboBox.setValue("Helvetica");

        fontSizeComboBox = new ComboBox<>();
        for (int i = 8; i <= 72; i += 2) {
            fontSizeComboBox.getItems().add(i);
        }
        fontSizeComboBox.setValue(12);

        fontStyleComboBox.setOnAction(event -> updateTextFont());
        fontSizeComboBox.setOnAction(event -> updateTextFont());

        fontStyleComboBox.setOnShowing(event -> keepTextSelection());
        fontSizeComboBox.setOnShowing(event -> keepTextSelection());
    }

    private void switchToFontControls() {
        HBox fontControls = new HBox(10);
        fontControls.getChildren().addAll(fontStyleComboBox, fontSizeComboBox);
        view.getTopPanelContainer().getChildren().setAll(view.getFileNameContainer(), fontControls);
    }

    private void switchToDefaultControls() {
        view.getTopPanelContainer().getChildren().setAll(view.getFileNameContainer(), view.getTopPanel());
    }

    private void setupTextSelectionHandler(TextField textField) {
        textField.setOnMouseClicked(event -> {
            if (!textField.getSelectedText().isEmpty()) {
                lastSelectedTextField = textField;
                switchToFontControls();
            } else {
                switchToDefaultControls();
                lastSelectedTextField = null;
            }
        });

        textField.setOnKeyReleased(event -> {
            if (!textField.getSelectedText().isEmpty()) {
                lastSelectedTextField = textField;
                switchToFontControls();
            } else {
                switchToDefaultControls();
                lastSelectedTextField = null;
            }
        });

        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && lastSelectedTextField != null) {
                lastSelectedTextField.requestFocus();
                lastSelectedTextField.selectAll();
            }
        });
    }

    private void updateTextFont() {
        if (lastSelectedTextField != null) {
            lastSelectedTextField.setFont(Font.font(fontStyleComboBox.getValue(), fontSizeComboBox.getValue()));
        }
    }

    private void keepTextSelection() {
        if (lastSelectedTextField != null) {
            lastSelectedTextField.requestFocus();
            lastSelectedTextField.selectAll();
        }
    }

    public DiagramView getView() {
        return view;
    }

    public List<Object> getSelectedElements() {
        return selectedElements;
    }

    public void moveSelectedElements(double offsetX, double offsetY) {
        for (Object element : selectedElements) {
            if (element instanceof ResizeBox) {
                ResizeBox resizeBox = (ResizeBox) element;
                if (!resizeBox.isLocked()) {
                    resizeBox.move(offsetX, offsetY);
                }
            } else if (element instanceof LineHandles) {
                LineHandles lineHandles = (LineHandles) element;
                if (!lineHandles.isLocked()) {
                    lineHandles.movePolyline(offsetX, offsetY);
                }
            }
        }
    }

    private void addShapeToWorkArea(String shapeType, double x, double y) {
        DiagramElement element = shapeType.equals("Image") ? null : ElementFactory.createElement(shapeType, x, y);
        if (element == null) return;

        view.getWorkArea().getChildren().add(element.getNode());
        elementTypesMap.put(element.getNode(), shapeType);

        if (element instanceof DiagramArrow) {
            LineHandles lineHandles = new LineHandles(element, this);
            view.getWorkArea().getChildren().add(lineHandles);
            selectedLineHandlesList.add(lineHandles);
            selectedElements.add(lineHandles);
        } else if (element instanceof DiagramShape && ((DiagramShape) element).getNode() instanceof Polyline) {
            LineHandles lineHandles = new LineHandles(element, this);
            view.getWorkArea().getChildren().add(lineHandles);
            selectedLineHandlesList.add(lineHandles);
            selectedElements.add(lineHandles);
        } else if (element instanceof DiagramShape) {
            ResizeBox resizeBox = new ResizeBox(((DiagramShape) element).getNode(), this);
            view.getWorkArea().getChildren().add(resizeBox);
            selectedResizeBoxes.add(resizeBox);
            selectedElements.add(resizeBox);
        }

        element.getNode().setOnMouseClicked(event -> {
            clearSelection();

            if (element instanceof DiagramArrow || (element instanceof DiagramShape && ((DiagramShape) element).getNode() instanceof Polyline)) {
                LineHandles lineHandles = new LineHandles(element, this);
                view.getWorkArea().getChildren().add(lineHandles);
                selectedLineHandlesList.add(lineHandles);
                selectedElements.add(lineHandles);
            } else {
                ResizeBox resizeBox = new ResizeBox(((DiagramShape) element).getNode(), this);
                view.getWorkArea().getChildren().add(resizeBox);
                selectedResizeBoxes.add(resizeBox);
                selectedElements.add(resizeBox);
            }
            event.consume();
        });

        element.getNode().setOnContextMenuRequested(event -> {
            contextMenu.show(element.getNode(), event.getScreenX(), event.getScreenY());
            event.consume();
        });

        if (element.getNode() instanceof Pane pane && pane.getChildren().get(0) instanceof TextField textField) {
            setupTextSelectionHandler(textField);
        }
    }

    private void addElementToCenter(String type) {
        double centerX = view.getWorkArea().getWidth() / 2;
        double centerY = view.getWorkArea().getHeight() / 2;
        if (type.equals("Image")) {
            addImageToWorkArea();
        } else {
            addShapeToWorkArea(type, centerX, centerY);
        }
    }

    private void clearSelection() {
        List<ResizeBox> toRemoveResizeBoxes = new ArrayList<>();
        for (ResizeBox resizeBox : selectedResizeBoxes) {
            if (!resizeBox.isLocked()) {
                view.getWorkArea().getChildren().remove(resizeBox);
                toRemoveResizeBoxes.add(resizeBox);
            }
        }
        selectedResizeBoxes.removeAll(toRemoveResizeBoxes);

        List<LineHandles> toRemoveLineHandles = new ArrayList<>();
        for (LineHandles lineHandles : selectedLineHandlesList) {
            if (!lineHandles.isLocked()) {
                view.getWorkArea().getChildren().remove(lineHandles);
                toRemoveLineHandles.add(lineHandles);
            }
        }
        selectedLineHandlesList.removeAll(toRemoveLineHandles);

        selectedElements.removeAll(toRemoveResizeBoxes);
        selectedElements.removeAll(toRemoveLineHandles);
    }

    private void updateSelectionRectangle(double startX, double startY, double endX, double endY) {
        view.getSelectionRectangle().setX(Math.min(startX, endX));
        view.getSelectionRectangle().setY(Math.min(startY, endY));
        view.getSelectionRectangle().setWidth(Math.abs(startX - endX));
        view.getSelectionRectangle().setHeight(Math.abs(startY - endY));
    }

    private void selectElementsWithinRectangle() {
        List<ResizeBox> newSelectedResize = new ArrayList<>();
        List<LineHandles> newSelectedLineHandles = new ArrayList<>();

        for (Node node : view.getWorkArea().getChildren()) {
            if (node instanceof Shape shape && !(node instanceof Rectangle && shape == view.getSelectionRectangle())) {
                if (shape.getStroke() == Color.LIGHTGRAY || shape.getStroke() == Color.DARKGRAY || isNodeAlreadyHandled(shape)) {
                    continue;
                }

                if (isShapeWithinSelectionRectangle(shape)) {
                    if (shape instanceof Polyline) {
                        LineHandles lineHandles = new LineHandles(new DiagramShape(shape), this);
                        if (!lineHandles.isLocked() && !isNodeAlreadyHandled(lineHandles)) {
                            newSelectedLineHandles.add(lineHandles);
                            selectedElements.add(lineHandles);
                        }
                    } else {
                        ResizeBox resizeBox = new ResizeBox(shape, this);
                        if (!resizeBox.isLocked() && !isNodeAlreadyHandled(resizeBox)) {
                            newSelectedResize.add(resizeBox);
                            selectedElements.add(resizeBox);
                        }
                    }
                }
            } else if (node instanceof ImageView imageView) {
                if (isShapeWithinSelectionRectangle(imageView) && !isNodeAlreadyHandled(imageView)) {
                    ResizeBox resizeBox = new ResizeBox(imageView, this);
                    if (!resizeBox.isLocked()) {
                        newSelectedResize.add(resizeBox);
                        selectedElements.add(resizeBox);
                    }
                }
            } else if (node instanceof Group group) {
                if (!isGroupLocked(group) && isShapeWithinSelectionRectangle(group)) {
                    for (Node child : group.getChildren()) {
                        if (child instanceof Polyline polyline) {
                            LineHandles lineHandles = new LineHandles(new DiagramArrow(group, polyline, (Polygon) group.getChildren().get(1)), this);
                            if (!lineHandles.isLocked()) {
                                newSelectedLineHandles.add(lineHandles);
                                selectedElements.add(lineHandles);
                            }
                            break;
                        }
                    }
                }
            } else if (node instanceof Pane pane && pane.getChildren().get(0) instanceof TextField) {
                if (isShapeWithinSelectionRectangle(pane) && !isNodeAlreadyHandled(pane)) {
                    ResizeBox resizeBox = new ResizeBox(pane, this);
                    if (!resizeBox.isLocked()) {
                        newSelectedResize.add(resizeBox);
                        selectedElements.add(resizeBox);
                    }
                }
            }
        }

        view.getWorkArea().getChildren().addAll(newSelectedResize);
        view.getWorkArea().getChildren().addAll(newSelectedLineHandles);

        selectedResizeBoxes.addAll(newSelectedResize);
        selectedLineHandlesList.addAll(newSelectedLineHandles);
    }

    private boolean isNodeAlreadyHandled(Node node) {
        boolean isHandledByResizeBox = view.getWorkArea().getChildren().stream()
                .filter(e -> e instanceof ResizeBox && ((ResizeBox) e).getNode() == node)
                .anyMatch(e -> ((ResizeBox) e).isLocked());

        boolean isHandledByLineHandles = view.getWorkArea().getChildren().stream()
                .filter(e -> e instanceof LineHandles && ((LineHandles) e).getPolyline() == node)
                .anyMatch(e -> ((LineHandles) e).isLocked());

        return isHandledByResizeBox || isHandledByLineHandles;
    }

    private boolean isGroupLocked(Group group) {
        return view.getWorkArea().getChildren().stream()
                .filter(e -> e instanceof LineHandles && group.getChildren().contains(((LineHandles) e).getPolyline()))
                .anyMatch(e -> ((LineHandles) e).isLocked());
    }

    private boolean isShapeWithinSelectionRectangle(Node node) {
        return view.getSelectionRectangle().getBoundsInParent().intersects(node.getBoundsInParent());
    }

    private boolean isShapeWithinSelectionRectangle(Shape shape) {
        return view.getSelectionRectangle().getBoundsInParent().intersects(shape.getBoundsInParent());
    }

    public void deleteSelectedElements() {
        model.deleteSelectedElements(selectedElements);
    }

    public void copySelectedElements() {
        model.copySelectedElements(selectedElements, elementTypesMap);
        clearSelection(); // Clear the previous selection
    }

    public void pasteElements() {
        List<DiagramElement> newElements = model.pasteElements();
        for (DiagramElement newElement : newElements) {
            Node node = newElement.getNode();
            view.getWorkArea().getChildren().add(node);

            if (newElement instanceof DiagramArrow) {
                LineHandles lineHandles = new LineHandles(newElement, this);
                view.getWorkArea().getChildren().add(lineHandles);
                selectedLineHandlesList.add(lineHandles);
                selectedElements.add(lineHandles);
            } else if (newElement instanceof DiagramShape && ((DiagramShape) newElement).getNode() instanceof Polyline) {
                LineHandles lineHandles = new LineHandles(newElement, this);
                view.getWorkArea().getChildren().add(lineHandles);
                selectedLineHandlesList.add(lineHandles);
                selectedElements.add(lineHandles);
            } else if (newElement instanceof DiagramShape) {
                ResizeBox resizeBox = new ResizeBox(((DiagramShape) newElement).getNode(), this);
                view.getWorkArea().getChildren().add(resizeBox);
                selectedResizeBoxes.add(resizeBox);
                selectedElements.add(resizeBox);
            }

            elementTypesMap.put(node, "PastedElement");
        }
    }

    public void cutSelectedElements() {
        model.cutSelectedElements(selectedElements, elementTypesMap);
    }

    public void lockSelectedElements() {
        model.lockSelectedElements(selectedElements);
    }

    public void unlockSelectedElements() {
        model.unlockSelectedElements(selectedElements);
        refocusWorkArea();
    }

    private void refocusWorkArea() {
        view.getWorkArea().requestFocus();
    }

    private void addImageToWorkArea() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File selectedFile = fileChooser.showOpenDialog(view.getWorkArea().getScene().getWindow());

        if (selectedFile != null) {
            String imagePath = selectedFile.getAbsolutePath();
            DiagramElement element = ElementFactory.createImageElement(imagePath, view.getWorkArea().getWidth() / 2, view.getWorkArea().getHeight() / 2);
            if (element != null) {
                view.getWorkArea().getChildren().add(element.getNode());
                ResizeBox resizeBox = new ResizeBox(element.getNode(), this);
                view.getWorkArea().getChildren().add(resizeBox);
                selectedResizeBoxes.add(resizeBox);
                selectedElements.add(resizeBox);
                elementTypesMap.put(element.getNode(), "Image");

                element.getNode().setOnMouseClicked(event -> {
                    clearSelection();
                    ResizeBox newResizeBox = new ResizeBox(element.getNode(), this);
                    view.getWorkArea().getChildren().add(newResizeBox);
                    selectedResizeBoxes.add(newResizeBox);
                    selectedElements.add(newResizeBox);
                    event.consume();
                });
            }
        }
    }

    private void zoomIn() {
        if (zoomScale < maxZoomScale) {
            zoomScale += 0.1;
            applyZoom();
        }
    }

    private void zoomOut() {
        if (zoomScale > minZoomScale) {
            zoomScale -= 0.1;
            applyZoom();
        }
    }

    private void applyZoom() {
        view.getWorkArea().setScaleX(zoomScale);
        view.getWorkArea().setScaleY(zoomScale);
        view.getZoomLabel().setText((int) (zoomScale * 100) + "%");
    }

    private void setupFileHandlers() {
        view.getOpenButton().setOnAction(event -> addImageToWorkArea());
    }

    private void saveAsPng() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Diagram as PNG");

        String fileName = view.getFileNameField().getText();
        if (fileName == null || fileName.isEmpty()) {
            fileName = "Назва файлу";
        }
        fileChooser.setInitialFileName(fileName + ".png");

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Files", "*.png"));
        File file = fileChooser.showSaveDialog(view.getWorkArea().getScene().getWindow());
        if (file != null) {
            try {
                view.setGridVisible(false);

                SnapshotParameters params = new SnapshotParameters();
                params.setFill(Color.TRANSPARENT); // Прозорий фон
                WritableImage image = view.getWorkArea().snapshot(params, null);

                saveImageToFile(image, file);

                view.setGridVisible(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveImageToFile(WritableImage image, File file) throws IOException {
        BufferedImage bufferedImage = new BufferedImage(
                (int) image.getWidth(),
                (int) image.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int argb = image.getPixelReader().getArgb(x, y);
                bufferedImage.setRGB(x, y, argb);
            }
        }

        ImageIO.write(bufferedImage, "png", file);
    }

    public Map<Node, String> getElementTypesMap() {
        return elementTypesMap;
    }
}
