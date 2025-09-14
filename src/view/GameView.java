package view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.control.Slider;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import controller.GameController;
import model.*;

import java.util.List;
import java.util.ArrayList;

/**
 * Main game view that renders all systems, wires, and packets.
 * Enhanced with dynamic viewport scaling to ensure all systems remain visible.
 */
public class GameView {
    private GameController gameController;
    private StackPane root;
    private Canvas canvas;
    private GraphicsContext gc;
    private GameLevel currentLevel;

    // Viewport management for dynamic scaling
    private double viewportScale = 1.0;
    private Point2D viewportOffset = new Point2D(0, 0);
    private Point2D viewportCenter = new Point2D(0, 0);
    private boolean autoScaleEnabled = true;
    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 3.0;
    private static final double SCALE_MARGIN = 50.0; // Pixels of margin around systems

    // Wire preview data for rendering during main loop
    private Point2D wirePreviewStart = null;
    private Point2D wirePreviewEnd = null;
    private boolean wirePreviewValid = false;
    private boolean showWirePreview = false;

    // Run button simulation (drawn on canvas instead of UI component)
    private boolean isRunButtonVisible = true;
    private double runButtonX = 0;
    private double runButtonY = 10;
    private double runButtonWidth = 120;
    private double runButtonHeight = 40;

    // Simulate button for temporal navigation
    private boolean isSimulateButtonVisible = true;
    private double simulateButtonX = 0;
    private double simulateButtonY = 60;
    private double simulateButtonWidth = 120;
    private double simulateButtonHeight = 40;

    // Exit simulate button
    private boolean isExitSimulateButtonVisible = false;
    private double exitSimulateButtonX = 0;
    private double exitSimulateButtonY = 60;
    private double exitSimulateButtonWidth = 120;
    private double exitSimulateButtonHeight = 40;

    // Time slider for temporal navigation
    private Slider timeSlider;
    private Label timeSliderLabel;

    public GameView(GameController gameController) {
        this.gameController = gameController;
        initializeUI();
        setupInputHandlers();
    }

    /**
     * Initializes the user interface.
     */
    private void initializeUI() {
        root = new StackPane();

        // Make canvas size dynamic based on available space
        canvas = new Canvas();
        canvas.setWidth(800);
        canvas.setHeight(600);

        // Bind canvas size to root pane size
        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty());

        gc = canvas.getGraphicsContext2D();

        root.getChildren().add(canvas);

        // Create time slider
        createTimeSlider();

        // Set background
        root.setStyle("-fx-background-color: #0a0a0a;");
    }

    /**
     * Creates the time slider for temporal navigation.
     */
    private void createTimeSlider() {
        // Create slider label
        timeSliderLabel = new Label("Time: 0.0s / 60.0s");
        timeSliderLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        timeSliderLabel.setTextFill(Color.WHITE);
        timeSliderLabel.setLayoutX(20);
        timeSliderLabel.setLayoutY(20);

        // Create slider
        timeSlider = new Slider();
        timeSlider.setMin(0.0);
        timeSlider.setMax(60.0);
        timeSlider.setValue(0.0);
        timeSlider.setPrefWidth(300);
        timeSlider.setLayoutX(20);
        timeSlider.setLayoutY(50);
        timeSlider.setDisable(true);
        timeSlider.setVisible(false);

        // Add listener for slider value changes
        timeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (gameController.canStartSimulation()) {
                gameController.updatePacketPositionsForTime(newVal.doubleValue());
            }
        });

        // Add to root pane
        root.getChildren().addAll(timeSliderLabel, timeSlider);
    }

    /**
     * Sets up input event handlers to connect with the InputHandler.
     */
    private void setupInputHandlers() {
        // Set up keyboard event handlers on the root pane
        root.setOnKeyPressed(event -> {
            gameController.getInputHandler().handleKeyPress(event);

            // Handle viewport-specific keys
            handleViewportKeyPress(event);
        });

        root.setOnKeyReleased(event -> {
            gameController.getInputHandler().handleKeyRelease(event);
        });

        // Set up mouse event handlers on the canvas where game elements are drawn
        canvas.setOnMousePressed(event -> {
            // Initialize mouse position for panning
            lastMouseX = event.getX();
            lastMouseY = event.getY();

            // Check if Run button was clicked
            if (isRunButtonClicked(event.getX(), event.getY())) {
                handleRunButtonClick();
                return;
            }

            // Check if Simulate button was clicked
            if (isSimulateButtonClicked(event.getX(), event.getY())) {
                handleSimulateButtonClick();
                return;
            }

            // Check if Exit Simulate button was clicked
            if (isExitSimulateButtonClicked(event.getX(), event.getY())) {
                handleExitSimulateButtonClick();
                return;
            }

            gameController.getInputHandler().handleMousePress(event);
        });

        canvas.setOnMouseDragged(event -> {
            gameController.getInputHandler().handleMouseDrag(event);

            // Handle viewport panning when space is held
            if (gameController.getInputHandler().isSpacePressed()) {
                handleViewportPan(event);
            }
        });

        canvas.setOnMouseReleased(event -> {
            gameController.getInputHandler().handleMouseRelease(event);
        });

        // Add mouse move handling for hover effects on wires and bends
        canvas.setOnMouseMoved(event -> {
            handleMouseMove(event);
        });

        // Clear hover state when mouse leaves canvas
        canvas.setOnMouseExited(event -> {
            hoveredWire = null;
            canvas.setCursor(javafx.scene.Cursor.DEFAULT);
        });

        // Add mouse wheel support for zooming
        canvas.setOnScroll(event -> {
            handleViewportZoom(event);
        });

        // Make the root focusable so it can receive keyboard events
        root.setFocusTraversable(true);
        root.requestFocus(); // Request focus to ensure key events are received
        java.lang.System.out.println("DEBUG: GameView - Root pane focusable: " + root.isFocusTraversable() + ", focused: " + root.isFocused());

        // Add focus change listener to debug focus issues
        root.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            java.lang.System.out.println("DEBUG: GameView - Root pane focus changed from " + wasFocused + " to " + isNowFocused);
        });

        // Make canvas focusable for mouse events but don't duplicate key handlers
        canvas.setFocusTraversable(true);
    }

    // Mouse position tracking for panning
    private double lastMouseX = 0;
    private double lastMouseY = 0;

    // Wire hover tracking for visual feedback
    private WireConnection hoveredWire = null;

    /**
     * Handles viewport panning via mouse drag.
     */
    private void handleViewportPan(javafx.scene.input.MouseEvent event) {
        // Get current mouse position
        double currentX = event.getX();
        double currentY = event.getY();

        // Calculate delta from last position
        double deltaX = currentX - lastMouseX;
        double deltaY = currentY - lastMouseY;

        // Update viewport offset
        viewportOffset.setX(viewportOffset.getX() + deltaX);
        viewportOffset.setY(viewportOffset.getY() + deltaY);

        // Update last mouse position
        lastMouseX = currentX;
        lastMouseY = currentY;

        // Disable auto-scale when manually panning
        autoScaleEnabled = false;
    }

    /**
     * Handles mouse move events for hover effects on wires and bends.
     */
    private void handleMouseMove(javafx.scene.input.MouseEvent event) {
        // Convert screen coordinates to world coordinates
        Point2D worldPos = screenToWorld(event.getX(), event.getY());

        // Check if hovering over a wire or bend for visual feedback
        if (gameController != null && gameController.getGameState() != null) {
            // Find wire at position for hover effect
            WireConnection newHoveredWire = findWireAtPosition(worldPos);
            if (newHoveredWire != null) {
                // Update hovered wire for visual highlighting
                this.hoveredWire = newHoveredWire;
                canvas.setCursor(javafx.scene.Cursor.HAND);
                return;
            }

            // Check if hovering over a bend
            WireConnection wireWithBend = findWireWithBendAtPosition(worldPos);
            if (wireWithBend != null) {
                canvas.setCursor(javafx.scene.Cursor.MOVE);
                return;
            }
        }

        // No wire or bend hovered - clear hover state
        this.hoveredWire = null;

        // Default cursor
        canvas.setCursor(javafx.scene.Cursor.DEFAULT);
    }

    /**
     * Finds a wire at the given world position for hover detection.
     */
    private WireConnection findWireAtPosition(Point2D position) {
        if (gameController == null || gameController.getGameState() == null) {
            return null;
        }

        for (WireConnection connection : gameController.getGameState().getWireConnections()) {
            if (!connection.isActive()) continue;

            if (isPositionNearWire(position, connection)) {
                return connection;
            }
        }

        return null;
    }

    /**
     * Finds a wire with a bend at the given position for hover detection.
     */
    private WireConnection findWireWithBendAtPosition(Point2D position) {
        if (gameController == null || gameController.getGameState() == null) {
            return null;
        }

        for (WireConnection connection : gameController.getGameState().getWireConnections()) {
            if (!connection.isActive()) continue;

            for (WireBend bend : connection.getBends()) {
                if (position.distanceTo(bend.getPosition()) <= 15.0) { // Same radius as InputHandler for consistency
                    return connection;
                }
            }
        }

        return null;
    }

    /**
     * Checks if a position is near a wire path for hover detection.
     */
    private boolean isPositionNearWire(Point2D position, WireConnection connection) {
        // Use the same smooth curve path that's used for rendering
        boolean useSmoothCurves = true;
        if (gameController != null && gameController.getGameState() != null) {
            Object setting = gameController.getGameState().getGameSettings().get("smoothWireCurves");
            if (setting != null) {
                useSmoothCurves = (Boolean) setting;
            }
        }

        List<Point2D> pathPoints = connection.getPathPoints(useSmoothCurves);

        // Check each line segment of the wire
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point2D start = pathPoints.get(i);
            Point2D end = pathPoints.get(i + 1);

            if (isPositionNearLineSegment(position, start, end)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a position is near a line segment for hover detection.
     */
    private boolean isPositionNearLineSegment(Point2D position, Point2D lineStart, Point2D lineEnd) {
        double distance = distanceToLineSegment(position, lineStart, lineEnd);
        return distance <= 25.0; // Same detection radius as InputHandler
    }

    /**
     * Calculates the distance from a point to a line segment for hover detection.
     */
    private double distanceToLineSegment(Point2D point, Point2D lineStart, Point2D lineEnd) {
        double A = point.getX() - lineStart.getX();
        double B = point.getY() - lineStart.getY();
        double C = lineEnd.getX() - lineStart.getX();
        double D = lineEnd.getY() - lineStart.getY();

        double dot = A * C + B * D;
        double lenSq = C * C + D * D;

        if (lenSq == 0) {
            return point.distanceTo(lineStart);
        }

        double param = dot / lenSq;

        Point2D closest;
        if (param < 0) {
            closest = lineStart;
        } else if (param > 1) {
            closest = lineEnd;
        } else {
            closest = new Point2D(
                    lineStart.getX() + param * C,
                    lineStart.getY() + param * D
            );
        }

        return point.distanceTo(closest);
    }

    /**
     * Handles viewport-specific keyboard input.
     */
    private void handleViewportKeyPress(javafx.scene.input.KeyEvent event) {
        switch (event.getCode()) {
            case R:
                // Reset viewport to show all systems
                resetViewport();
                break;
            case SPACE:
                // Space is handled in mouse drag for panning
                break;
            case EQUALS:
            case ADD:
                // Zoom in
                setViewportScale(viewportScale * 1.2);
                break;
            case MINUS:
            case SUBTRACT:
                // Zoom out
                setViewportScale(viewportScale / 1.2);
                break;
            case DIGIT0:
                // Reset zoom to 1.0
                setViewportScale(1.0);
                break;
            case A:
                // Toggle auto-scale
                if (event.isControlDown()) {
                    setAutoScaleEnabled(!autoScaleEnabled);
                }
                break;
        }
    }

    /**
     * Handles viewport zooming via mouse wheel.
     */
    private void handleViewportZoom(javafx.scene.input.ScrollEvent event) {
        double zoomFactor = 1.1;
        if (event.getDeltaY() < 0) {
            zoomFactor = 1.0 / zoomFactor;
        }

        // Get mouse position for zoom center
        double mouseX = event.getX();
        double mouseY = event.getY();

        // Convert screen coordinates to world coordinates
        Point2D worldPos = screenToWorld(mouseX, mouseY);

        // Calculate new scale
        double newScale = viewportScale * zoomFactor;
        newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale));

        if (newScale != viewportScale) {
            // Calculate new offset to zoom towards mouse position
            double scaleRatio = newScale / viewportScale;
            viewportOffset.setX(mouseX - worldPos.getX() * newScale);
            viewportOffset.setY(mouseY - worldPos.getY() * newScale);

            viewportScale = newScale;

            // Disable auto-scale when manually zooming
            autoScaleEnabled = false;
        }
    }

    /**
     * Updates the view with current game state.
     */
    public void update() {
        if (currentLevel == null) return;

        // Don't update canvas if shop is visible
        if (gameController.getShopView() != null && gameController.getShopView().isVisible()) {
            return;
        }

        // Update viewport if auto-scaling is enabled
        if (autoScaleEnabled) {
            updateViewport();
        }

        // Clear canvas
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Apply viewport transformation
        gc.save();
        applyViewportTransform();

        // Draw systems
        drawSystems();

        // Draw wire connections
        drawWireConnections();

        // Draw wire preview if enabled (inside viewport transformation)
        if (showWirePreview && wirePreviewStart != null && wirePreviewEnd != null) {
            renderWirePreview();
        }

        // Draw packets
        drawPackets();

        // Restore graphics context
        gc.restore();

        // Draw control instructions (always in screen space)
        drawControlInstructions();

        // Draw Run button (always in screen space)
        drawRunButton();
        
        // Draw Simulate and Exit buttons
        drawSimulateButtons();

        // Draw error message if any
        drawErrorMessage();

        // Update time slider
        updateTimeSlider();
    }

    /**
     * Updates the time slider based on current game state.
     */
    private void updateTimeSlider() {
        if (timeSlider == null || timeSliderLabel == null) return;

        // Only show slider when in simulating mode
        boolean isSimulating = gameController.isSimulatingMode();
        
        // Update slider visibility and state
        timeSlider.setVisible(isSimulating);
        timeSlider.setDisable(!isSimulating);
        
        if (isSimulating) {
            // Update slider values
            double currentTime = gameController.getGameState().getTemporalProgress();
            double maxTime = gameController.getGameState().getCurrentLevel() != null ?
                    gameController.getGameState().getCurrentLevel().getLevelDuration() : 60.0;
            
            // Update slider max value if needed
            if (timeSlider.getMax() != maxTime) {
                timeSlider.setMax(maxTime);
            }
            
            // Update slider value (prevent infinite loop)
            if (Math.abs(timeSlider.getValue() - currentTime) > 0.01) {
                timeSlider.setValue(currentTime);
            }
            
            // Update label
            timeSliderLabel.setText(String.format("Time: %.1fs / %.1fs", currentTime, maxTime));
        }
    }

    /**
     * Updates the viewport to ensure all systems are visible.
     */
    private void updateViewport() {
        if (currentLevel == null || currentLevel.getSystems().isEmpty()) {
            return;
        }

        // Calculate bounds of all systems
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for (model.System system : currentLevel.getSystems()) {
            Point2D pos = system.getPosition();
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
        }

        // Add margin around systems
        minX -= SCALE_MARGIN;
        minY -= SCALE_MARGIN;
        maxX += SCALE_MARGIN;
        maxY += SCALE_MARGIN;

        // Calculate required dimensions
        double levelWidth = maxX - minX;
        double levelHeight = maxY - minY;

        // Calculate scale to fit in canvas
        double scaleX = (canvas.getWidth() - 100) / levelWidth; // Leave 100px margin
        double scaleY = (canvas.getHeight() - 100) / levelHeight; // Leave 100px margin

        // Use the smaller scale to ensure everything fits
        viewportScale = Math.min(scaleX, scaleY);

        // Clamp scale to reasonable bounds
        viewportScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, viewportScale));

        // Calculate center of level
        double levelCenterX = (minX + maxX) / 2.0;
        double levelCenterY = (minY + maxY) / 2.0;

        // Calculate viewport center (center of canvas)
        double canvasCenterX = canvas.getWidth() / 2.0;
        double canvasCenterY = canvas.getHeight() / 2.0;

        // Calculate offset to center the level
        viewportOffset.setX(canvasCenterX - (levelCenterX * viewportScale));
        viewportOffset.setY(canvasCenterY - (levelCenterY * viewportScale));

        viewportCenter.setX(levelCenterX);
        viewportCenter.setY(levelCenterY);
    }

    /**
     * Applies the viewport transformation to the graphics context.
     */
    private void applyViewportTransform() {
        gc.translate(viewportOffset.getX(), viewportOffset.getY());
        gc.scale(viewportScale, viewportScale);
    }

    /**
     * Converts screen coordinates to world coordinates.
     */
    public Point2D screenToWorld(double screenX, double screenY) {
        double worldX = (screenX - viewportOffset.getX()) / viewportScale;
        double worldY = (screenY - viewportOffset.getY()) / viewportScale;
        return new Point2D(worldX, worldY);
    }

    /**
     * Converts world coordinates to screen coordinates.
     */
    public Point2D worldToScreen(double worldX, double worldY) {
        double screenX = worldX * viewportScale + viewportOffset.getX();
        double screenY = worldY * viewportScale + viewportOffset.getY();
        return new Point2D(screenX, screenY);
    }

    /**
     * Gets the current viewport scale.
     */
    public double getViewportScale() {
        return viewportScale;
    }

    /**
     * Gets the current viewport offset.
     */
    public Point2D getViewportOffset() {
        return viewportOffset;
    }

    /**
     * Sets whether auto-scaling is enabled.
     */
    public void setAutoScaleEnabled(boolean enabled) {
        this.autoScaleEnabled = enabled;
    }

    /**
     * Manually sets the viewport scale.
     */
    public void setViewportScale(double scale) {
        this.viewportScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }

    /**
     * Manually sets the viewport offset.
     */
    public void setViewportOffset(Point2D offset) {
        this.viewportOffset = offset;
    }

    /**
     * Resets the viewport to show all systems.
     */
    public void resetViewport() {
        updateViewport();
    }

    /**
     * Draws all systems in the current level.
     */
    private void drawSystems() {
        if (currentLevel == null) return;

        for (model.System system : currentLevel.getSystems()) {
            drawSystem(system);
            drawSystemPacketIndicators(system);
        }
    }

    /**
     * Draws a single system.
     */
    private void drawSystem(model.System system) {
        Point2D pos = system.getPosition();

        // Choose color and shape by system type for distinct appearance
        Color bodyColor = Color.BLUE;
        String label = "";

        if (system instanceof ReferenceSystem) {
            ReferenceSystem refSystem = (ReferenceSystem) system;
            if (refSystem.isSource()) {
                bodyColor = Color.LIMEGREEN; // Source reference systems
                label = "REF";
            } else {
                bodyColor = Color.CRIMSON; // Destination reference systems  
                label = "REF";
            }
        } else if (system.getSystemType() != null) {
            switch (system.getSystemType()) {
                case NORMAL:
                    bodyColor = Color.DODGERBLUE;
                    label = "N";
                    break;
                case SPY:
                    bodyColor = Color.MEDIUMPURPLE;
                    label = "SPY";
                    break;
                case SABOTEUR:
                    bodyColor = Color.DARKORANGE;
                    label = "SAB";
                    break;
                case VPN:
                    bodyColor = Color.TEAL;
                    label = "VPN";
                    break;
                case ANTI_TROJAN:
                    bodyColor = Color.DARKSEAGREEN;
                    label = "AT";
                    break;
                case DISTRIBUTOR:
                    bodyColor = Color.SLATEBLUE;
                    label = "DSTB";
                    break;
                case MERGER:
                    bodyColor = Color.STEELBLUE;
                    label = "MRG";
                    break;
                case REFERENCE:
                default:
                    bodyColor = Color.GRAY;
                    break;
            }
        }

        gc.setFill(bodyColor);

        // Compute rectangle size based on port counts, not their absolute positions
        double[] halfSizes = computeHalfSizes(system);
        double halfWidth = halfSizes[0];
        double halfHeight = halfSizes[1];

        // Layout ports around the rectangle edges using even spacing
        layoutPorts(system, halfWidth, halfHeight);

        // Draw system body (rectangle)
        gc.fillRect(pos.getX() - halfWidth, pos.getY() - halfHeight, halfWidth * 2, halfHeight * 2);

        // Draw system border
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeRect(pos.getX() - halfWidth, pos.getY() - halfHeight, halfWidth * 2, halfHeight * 2);

        // Draw a compact label to indicate type
        if (!label.isEmpty()) {
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", 9));
            gc.fillText(label, pos.getX() - 12, pos.getY() + 3);
        }

        // Draw system status indicator if globally enabled
        if (gameController != null && gameController.getGameState() != null
                && gameController.getGameState().isShowSystemIndicators()) {
            // Show indicators for all systems when enabled, regardless of network connectivity
            // This allows users to see which systems are part of a connected network
            drawSystemIndicator(system);
        }

        // Draw ports
        drawPorts(system);
    }

    /**
     * Checks if all ports of a specific system are connected.
     * This determines if the system indicator should be green.
     */
    private boolean isSystemFullyConnected(model.System system) {
        if (gameController == null || gameController.getWiringController() == null) {
            return false;
        }

        // Check if all ports on this specific system are connected
        for (Port port : system.getAllPorts()) {
            if (!port.isConnected()) {
                return false; // Found an unconnected port
            }
        }

        return true; // All ports are connected
    }

    /**
     * Draws a status indicator for the system.
     */
    private void drawSystemIndicator(model.System system) {
        Point2D pos = system.getPosition();

        // Determine indicator color based on system status
        Color indicatorColor;
        String indicatorSymbol;

        if (system.hasFailed()) {
            indicatorColor = Color.RED;
            indicatorSymbol = "✗";
        } else if (system.isDeactivated()) {
            indicatorColor = Color.ORANGE;
            indicatorSymbol = "⏸";
        } else if (!system.isActive()) {
            indicatorColor = Color.YELLOW;
            indicatorSymbol = "⚠";
        } else if (isSystemFullyConnected(system)) {
            indicatorColor = Color.LIME;
            indicatorSymbol = "✓";
        } else {
            // System is active but not fully connected
            indicatorColor = Color.GRAY;
            indicatorSymbol = "○";
        }

        // Use the same computed rectangle size used in drawSystem
        double[] halfSizes = computeHalfSizes(system);
        double halfWidth = halfSizes[0];
        double halfHeight = halfSizes[1];

        // Draw indicator circle in the top-right corner of the system
        double indicatorX = pos.getX() + halfWidth - 5;
        double indicatorY = pos.getY() - halfHeight + 5;
        double indicatorSize = 8;

        // Draw indicator background
        gc.setFill(indicatorColor);
        gc.fillOval(indicatorX - indicatorSize/2, indicatorY - indicatorSize/2, indicatorSize, indicatorSize);

        // Draw indicator border
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeOval(indicatorX - indicatorSize/2, indicatorY - indicatorSize/2, indicatorSize, indicatorSize);

        // Draw indicator symbol
        gc.setFill(Color.BLACK);
        gc.setFont(javafx.scene.text.Font.font("Arial", 6));
        gc.fillText(indicatorSymbol, indicatorX - 2, indicatorY + 2);
    }

    /**
     * Draws ports for a system.
     */
    private void drawPorts(model.System system) {
        // Ports are positioned during layoutPorts; just draw them here
        for (Port port : system.getInputPorts()) {
            Color portColor = Color.YELLOW;
            if (gameController.getInputHandler().getSelectedPort() == port) {
                portColor = Color.LIME;
            }
            drawPort(port, portColor);
        }

        for (Port port : system.getOutputPorts()) {
            Color portColor = Color.ORANGE;
            if (gameController.getInputHandler().getSelectedPort() == port) {
                portColor = Color.LIME;
            }
            drawPort(port, portColor);
        }
    }

    /**
     * Computes rectangle half sizes based on port counts to avoid overlaps.
     */
    private double[] computeHalfSizes(model.System system) {
        int inputCount = system.getInputPorts() != null ? system.getInputPorts().size() : 0;
        int outputCount = system.getOutputPorts() != null ? system.getOutputPorts().size() : 0;
        int maxPerSide = Math.max(inputCount, outputCount);
        double baseHalfWidth = 24.0; // leave room for label
        double baseHalfHeight = 20.0;
        double slotSpacing = 16.0; // vertical spacing per port
        double edgePadding = 12.0;
        double neededHalfHeight = Math.max(baseHalfHeight, ((Math.max(1, maxPerSide) - 1) * slotSpacing) / 2.0 + edgePadding);
        double neededHalfWidth = Math.max(baseHalfWidth, 28.0);
        return new double[]{neededHalfWidth, neededHalfHeight};
    }

    /**
     * Lays out ports evenly along left (inputs) and right (outputs) edges of the system rectangle.
     */
    private void layoutPorts(model.System system, double halfWidth, double halfHeight) {
        Point2D center = system.getPosition();
        List<Port> inputs = system.getInputPorts();
        List<Port> outputs = system.getOutputPorts();
        double slotSpacing = 16.0;
        double portOffset = 12.0; // distance from rectangle edge

        // Inputs on the left edge
        if (inputs != null && !inputs.isEmpty()) {
            int count = inputs.size();
            for (int i = 0; i < count; i++) {
                Port p = inputs.get(i);
                double y = (count == 1)
                        ? center.getY()
                        : center.getY() - ((count - 1) * slotSpacing) / 2.0 + i * slotSpacing;
                double x = center.getX() - halfWidth - portOffset;
                p.setPosition(new Point2D(x, y));
            }
        }

        // Outputs on the right edge
        if (outputs != null && !outputs.isEmpty()) {
            int count = outputs.size();
            for (int i = 0; i < count; i++) {
                Port p = outputs.get(i);
                double y = (count == 1)
                        ? center.getY()
                        : center.getY() - ((count - 1) * slotSpacing) / 2.0 + i * slotSpacing;
                double x = center.getX() + halfWidth + portOffset;
                p.setPosition(new Point2D(x, y));
            }
        }
    }

    /**
     * Draws a single port.
     */
    private void drawPort(Port port, Color color) {
        Point2D pos = port.getPosition();

        gc.setFill(color);

        if (port.getShape() == PortShape.SQUARE) {
            // Draw square port
            gc.fillRect(pos.getX() - 5, pos.getY() - 5, 10, 10);
            gc.setStroke(Color.WHITE);
            gc.strokeRect(pos.getX() - 5, pos.getY() - 5, 10, 10);
        } else if (port.getShape() == PortShape.TRIANGLE) {
            // Draw triangle port
            double[] xPoints = {pos.getX(), pos.getX() - 5, pos.getX() + 5};
            double[] yPoints = {pos.getY() - 5, pos.getY() + 5, pos.getY() + 5};
            gc.fillPolygon(xPoints, yPoints, 3);
            gc.setStroke(Color.WHITE);
            gc.strokePolygon(xPoints, yPoints, 3);
        } else if (port.getShape() == PortShape.HEXAGON) {
            // Draw hexagon port
            double[] xPoints = new double[6];
            double[] yPoints = new double[6];
            double radius = 5;
            for (int i = 0; i < 6; i++) {
                double angle = i * Math.PI / 3;
                xPoints[i] = pos.getX() + radius * Math.cos(angle);
                yPoints[i] = pos.getY() + radius * Math.sin(angle);
            }
            gc.fillPolygon(xPoints, yPoints, 6);
            gc.setStroke(Color.WHITE);
            gc.strokePolygon(xPoints, yPoints, 6);
        }
    }

    /**
     * Draws wire connections with activity indicators.
     */
    private void drawWireConnections() {
        if (currentLevel == null) return;

        

        for (WireConnection connection : currentLevel.getWireConnections()) {
            if (connection.isActive()) {
                drawWireConnection(connection);
            } else {
                
            }
        }
    }

    /**
     * Draws a single wire connection with enhanced visuals.
     */
    private void drawWireConnection(WireConnection connection) {
        // Check if smooth curves are enabled in game settings
        boolean useSmoothCurves = true; // Default to smooth curves
        if (gameController != null && gameController.getGameState() != null) {
            Object setting = gameController.getGameState().getGameSettings().get("smoothWireCurves");
            if (setting instanceof Boolean) {
                useSmoothCurves = (Boolean) setting;
            }
        }

        List<Point2D> pathPoints = connection.getPathPoints(useSmoothCurves);
        boolean hasPacket = connection.isOccupied();

        // Determine wire color and effects based on activity and hover state
        Color wireColor = Color.CYAN;
        Color glowColor = Color.LIGHTCYAN;
        double wireWidth = 2.0;

        // Check if wire passes over systems - if so, make it red
        boolean passesOverSystems = false;
        if (gameController != null && gameController.getGameState() != null && currentLevel != null) {
            passesOverSystems = connection.passesOverSystems(currentLevel.getSystems());
            
        }
        
        if (passesOverSystems) {
            wireColor = Color.RED;
            glowColor = Color.LIGHTCORAL;
            // Keep normal thickness
        } else if (connection == hoveredWire) {
            // Add hover highlighting only if not problematic
            wireColor = Color.YELLOW;
            glowColor = Color.ORANGE;
            wireWidth = 3.0; // Make hovered wire thicker
        }

        if (hasPacket && !passesOverSystems) {
            // Active wire with packets - make it glow and pulse (only if not problematic)
            List<Packet> packets = connection.getPacketsOnWire();
            if (!packets.isEmpty()) {
                // Color based on dominant packet type (first active packet)
                Packet dominantPacket = packets.stream()
                        .filter(Packet::isActive)
                        .findFirst()
                        .orElse(packets.get(0));

                if (dominantPacket instanceof BulkPacket) {
                    wireColor = Color.RED;
                    glowColor = Color.LIGHTCORAL;
                } else if (dominantPacket instanceof ConfidentialPacket) {
                    wireColor = Color.ORANGE;
                    glowColor = Color.YELLOW;
                } else {
                    wireColor = Color.LIME;
                    glowColor = Color.LIGHTGREEN;
                }

                // Pulsing effect based on packet count and speed
                double avgSpeed = packets.stream()
                        .filter(Packet::isActive)
                        .mapToDouble(p -> p.getMovementVector().magnitude())
                        .average()
                        .orElse(30.0);
                double packetCountMultiplier = Math.min(packets.size() / 2.0, 1.5);
                double pulseIntensity = 1.0 + 0.5 * Math.sin(java.lang.System.currentTimeMillis() * 0.01 * (avgSpeed / 50.0)) * packetCountMultiplier;
                wireWidth = 2.0 * pulseIntensity;
            }
        }

        // Enable anti-aliasing for smoother wire rendering
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

        // Draw glow effect for active wires or problematic wires
        if (hasPacket || passesOverSystems) {
            gc.setStroke(glowColor);
            gc.setLineWidth(wireWidth + 2);
            for (int i = 0; i < pathPoints.size() - 1; i++) {
                Point2D start = pathPoints.get(i);
                Point2D end = pathPoints.get(i + 1);
                gc.strokeLine(start.getX(), start.getY(), end.getX(), end.getY());
            }
        }

        // Draw main wire with enhanced quality
        gc.setStroke(wireColor);
        gc.setLineWidth(wireWidth);

        // For smooth curves, draw as a continuous path for better quality
        if (useSmoothCurves && pathPoints.size() > 2) {
            // Draw smooth curve as a polyline for better visual quality
            for (int i = 0; i < pathPoints.size() - 1; i++) {
                Point2D start = pathPoints.get(i);
                Point2D end = pathPoints.get(i + 1);
                gc.strokeLine(start.getX(), start.getY(), end.getX(), end.getY());
            }
        } else {
            // Draw as individual line segments for rigid wires
            for (int i = 0; i < pathPoints.size() - 1; i++) {
                Point2D start = pathPoints.get(i);
                Point2D end = pathPoints.get(i + 1);
                gc.strokeLine(start.getX(), start.getY(), end.getX(), end.getY());
            }
        }

        // Draw activity indicators along the wire for occupied wires
        if (hasPacket) {
            drawWireActivityIndicators(connection, pathPoints);
        }

        // Draw packet progress indicators for all packets - DISABLED
        // Progress bars above wires are no longer shown
        /*
        if (hasPacket) {
            List<Packet> packets = connection.getPacketsOnWire();
            for (Packet packet : packets) {
                if (packet.isActive()) {
                    drawPacketProgressOnWire(connection, pathPoints, packet);
                }
            }
        }
        */

        // Draw bends as colored points with enhanced visibility
        for (int i = 0; i < connection.getBends().size(); i++) {
            WireBend bend = connection.getBends().get(i);
            Point2D pos = bend.getPosition();

            // Check if this bend is selected
            boolean isSelected = gameController.getInputHandler().getSelectedWire() == connection &&
                    gameController.getInputHandler().getSelectedBendIndex() == i;

            // Check if this bend is being hovered over
            boolean isHovered = hoveredWire == connection &&
                    gameController.getInputHandler().getSelectedBendIndex() == i;

            if (isSelected) {
                // Selected bend - draw with highlight and larger size
                gc.setFill(Color.YELLOW);
                gc.fillOval(pos.getX() - 8, pos.getY() - 8, 16, 16);
                gc.setStroke(Color.ORANGE);
                gc.setLineWidth(4);
                gc.strokeOval(pos.getX() - 8, pos.getY() - 8, 16, 16);

                // Add pulsing effect for selected bend
                double pulse = 1.0 + 0.3 * Math.sin(java.lang.System.currentTimeMillis() * 0.01);
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(2);
                gc.strokeOval(pos.getX() - 10 * pulse, pos.getY() - 10 * pulse, 20 * pulse, 20 * pulse);
            } else if (isHovered) {
                // Hovered bend - draw with hover effect
                gc.setFill(Color.LIME);
                gc.fillOval(pos.getX() - 7, pos.getY() - 7, 14, 14);
                gc.setStroke(Color.GREEN);
                gc.setLineWidth(3);
                gc.strokeOval(pos.getX() - 7, pos.getY() - 7, 14, 14);
            } else {
                // Normal bend - larger size and better colors for easier interaction
                Color bendColor = hasPacket ? wireColor : Color.RED;
                gc.setFill(bendColor);
                gc.fillOval(pos.getX() - 6, pos.getY() - 6, 12, 12); // Increased from 8x8 to 12x12
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(2); // Increased from 1 to 2
                gc.strokeOval(pos.getX() - 6, pos.getY() - 6, 12, 12);

                // Add subtle glow effect for all bends
                gc.setStroke(Color.color(1.0, 1.0, 1.0, 0.5));
                gc.setLineWidth(1);
                gc.strokeOval(pos.getX() - 8, pos.getY() - 8, 16, 16);
            }
        }
    }

    /**
     * Draws activity indicators along an active wire.
     */
    private void drawWireActivityIndicators(WireConnection connection, List<Point2D> pathPoints) {
        if (pathPoints.size() < 2) return;

        double time = java.lang.System.currentTimeMillis() * 0.005;

        // Draw flowing particles along the wire
        for (int particle = 0; particle < 3; particle++) {
            double progress = ((time + particle * 0.3) % 1.0);
            Point2D particlePos = getPositionAlongWire(pathPoints, progress);

            if (particlePos != null) {
                gc.setFill(Color.color(1.0, 1.0, 1.0, 0.8));
                gc.fillOval(particlePos.getX() - 2, particlePos.getY() - 2, 4, 4);
            }
        }
    }

    /**
     * Draws a progress indicator showing packet position on wire.
     */
    private void drawPacketProgressOnWire(WireConnection connection, List<Point2D> pathPoints, Packet packet) {
        if (pathPoints.size() < 2 || packet == null) return;

        // Calculate packet progress along wire
        Point2D packetPos = packet.getCurrentPosition();
        Point2D startPos = pathPoints.get(0);
        Point2D endPos = pathPoints.get(pathPoints.size() - 1);

        double totalDistance = startPos.distanceTo(endPos);
        double packetDistance = startPos.distanceTo(packetPos);
        double progress = Math.max(0, Math.min(1, packetDistance / totalDistance));

        // Draw progress bar above the wire with offset based on packet index
        List<Packet> packets = connection.getPacketsOnWire();
        int packetIndex = packets.indexOf(packet);
        double yOffset = -15 - (packetIndex * 8); // Stack progress bars for multiple packets

        Point2D midPoint = new Point2D(
                (startPos.getX() + endPos.getX()) / 2,
                (startPos.getY() + endPos.getY()) / 2 + yOffset
        );

        // Progress bar background
        gc.setFill(Color.color(0.3, 0.3, 0.3, 0.7));
        gc.fillRect(midPoint.getX() - 25, midPoint.getY() - 3, 50, 6);

        // Progress bar fill
        Color progressColor = Color.LIGHTGREEN;
        if (packet instanceof BulkPacket) {
            progressColor = Color.LIGHTCORAL;
        } else if (packet instanceof ConfidentialPacket) {
            progressColor = Color.YELLOW;
        }

        gc.setFill(progressColor);
        gc.fillRect(midPoint.getX() - 25, midPoint.getY() - 3, 50 * progress, 6);

        // Progress bar border
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokeRect(midPoint.getX() - 25, midPoint.getY() - 3, 50, 6);
    }

    /**
     * Gets a position along the wire path based on progress (0.0 to 1.0).
     */
    private Point2D getPositionAlongWire(List<Point2D> pathPoints, double progress) {
        if (pathPoints.size() < 2) return null;

        // Calculate total path length
        double totalLength = 0;
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            totalLength += pathPoints.get(i).distanceTo(pathPoints.get(i + 1));
        }

        double targetDistance = totalLength * progress;
        double currentDistance = 0;

        // Find the segment that contains the target distance
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point2D start = pathPoints.get(i);
            Point2D end = pathPoints.get(i + 1);
            double segmentLength = start.distanceTo(end);

            if (currentDistance + segmentLength >= targetDistance) {
                // Found the segment - interpolate position
                double segmentProgress = (targetDistance - currentDistance) / segmentLength;
                return new Point2D(
                        start.getX() + (end.getX() - start.getX()) * segmentProgress,
                        start.getY() + (end.getY() - start.getY()) * segmentProgress
                );
            }

            currentDistance += segmentLength;
        }

        // Return last point if we've gone past the end
        return pathPoints.get(pathPoints.size() - 1);
    }

    /**
     * Draws packet flow indicators for systems.
     */
    private void drawSystemPacketIndicators(model.System system) {

        // Draw port activity indicators for both input and output ports
        List<Port> allPorts = new ArrayList<>();
        allPorts.addAll(system.getInputPorts());
        allPorts.addAll(system.getOutputPorts());

        for (Port port : allPorts) {
            if (port.getCurrentPacket() != null) {
                Point2D portPos = port.getPosition();

                // Pulsing indicator for occupied ports
                double pulseSize = 3 + 2 * Math.sin(java.lang.System.currentTimeMillis() * 0.01);
                gc.setFill(Color.color(1.0, 1.0, 0.0, 0.8));
                gc.fillOval(portPos.getX() - pulseSize, portPos.getY() - pulseSize,
                        pulseSize * 2, pulseSize * 2);

                // Packet type indicator
                Packet packet = port.getCurrentPacket();
                String indicator = "●";
                if (packet instanceof BulkPacket) {
                    indicator = "■";
                } else if (packet instanceof ConfidentialPacket) {
                    indicator = "♦";
                }

                gc.setFill(Color.WHITE);
                gc.setFont(javafx.scene.text.Font.font("Arial", 8));
                gc.fillText(indicator, portPos.getX() - 3, portPos.getY() + 3);
            }
        }
    }

    /**
     * Draws all active packets.
     */
    private void drawPackets() {
        for (Packet packet : gameController.getGameState().getActivePackets()) {
            if (packet.isActive()) {
                drawPacket(packet);
            }
        }
    }

    /**
     * Draws a single packet with enhanced visualization.
     */
    private void drawPacket(Packet packet) {
        Point2D pos = packet.getCurrentPosition();
        Vec2D velocity = packet.getMovementVector();

        // Determine packet color based on type
        Color packetColor = Color.WHITE; // Default color
        Color borderColor = Color.BLACK;
        Color trailColor = Color.WHITE;

        if (packet instanceof SquarePacket) {
            packetColor = Color.LIME;
            trailColor = Color.LIGHTGREEN;
        } else if (packet instanceof TrianglePacket) {
            packetColor = Color.MAGENTA;
            trailColor = Color.PINK;
        } else if (packet instanceof MessengerPacket) {
            // Messenger packets based on size
            if (packet.getSize() == 1) {
                packetColor = Color.CYAN; // Small messenger
                trailColor = Color.LIGHTCYAN;
            } else if (packet.getSize() == 2) {
                packetColor = Color.LIME; // Medium messenger
                trailColor = Color.LIGHTGREEN;
            } else if (packet.getSize() == 3) {
                packetColor = Color.MAGENTA; // Large messenger
                trailColor = Color.PINK;
            }
        } else if (packet instanceof ConfidentialPacket) {
            packetColor = Color.ORANGE; // Confidential packets
            trailColor = Color.YELLOW;
        } else if (packet instanceof BulkPacket) {
            packetColor = Color.RED; // Bulk packets
            trailColor = Color.LIGHTCORAL;
        }

        // Check if packet is protected (VPN effect)
        if (packet.getPacketType() != null && packet.getPacketType().isProtected()) {
            packetColor = Color.YELLOW; // Protected packets are yellow
            trailColor = Color.LIGHTYELLOW;
        }

        // Check if packet is trojan
        if (packet.getPacketType() != null && packet.getPacketType().isTrojan()) {
            packetColor = Color.DARKRED; // Trojan packets are dark red
            trailColor = Color.RED;
        }

        // Check if packet is bit packet (from bulk packet splitting)
        if (packet.getPacketType() != null && packet.getPacketType().isBitPacket()) {
            packetColor = Color.PURPLE; // Bit packets are purple
            trailColor = Color.PLUM;
        }

        // Calculate speed for visual effects
        double speed = velocity.magnitude();
        double minSize = Math.max(2, packet.getSize());

        // Size varies based on speed (faster = slightly larger for visibility)
        double sizeVariation = Math.min(2.0, speed / 50.0);
        double displaySize = minSize + sizeVariation;

        // Draw motion trail behind packet
        drawPacketTrail(pos, velocity, trailColor, displaySize);

        // Draw packet based on specific packet type shape
        drawPacketShape(packet, pos, displaySize, packetColor, borderColor, trailColor, speed);

        // Add noise level indicator above each packet
        double noiseLevel = packet.getNoiseLevel();
        int maxHealth = packet.getSize();
        double remainingHealth = maxHealth - noiseLevel;
        
        // Choose color based on health status
        Color healthColor;
        if (remainingHealth <= 0) {
            healthColor = Color.RED; // Critical - about to be lost
        } else if (remainingHealth <= 1) {
            healthColor = Color.ORANGE; // Warning - low health
        } else {
            healthColor = Color.WHITE; // Healthy
        }
        
        gc.setFill(healthColor);
        gc.setFont(javafx.scene.text.Font.font("Arial", 8));
        String healthText = String.format("%.1f/%d", remainingHealth, maxHealth);
        gc.fillText(healthText, pos.getX() - 8, pos.getY() - displaySize - 5);

        // Add packet size indicator for larger packets (keep existing functionality)
        if (packet.getSize() > 3) {
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", 8));
            gc.fillText(String.valueOf(packet.getSize()), pos.getX() - 3, pos.getY() + 3);
        }

        // Add speed indicator for fast-moving packets
        if (speed > 100) {
            gc.setFill(Color.YELLOW);
            gc.setFont(javafx.scene.text.Font.font("Arial", 6));
            gc.fillText("⚡", pos.getX() + displaySize - 2, pos.getY() - displaySize + 8);
        }
    }

    /**
     * Draws the specific shape for different packet types.
     */
    private void drawPacketShape(Packet packet, Point2D pos, double displaySize, Color packetColor, Color borderColor, Color trailColor, double speed) {
        // Determine packet type and draw appropriate shape
        PacketType type = packet.getPacketType();
        
        if (type == PacketType.SQUARE_MESSENGER) {
            // 🟩 مربعی - Green square
            gc.setFill(Color.LIME);
            gc.fillRect(pos.getX() - displaySize, pos.getY() - displaySize, displaySize * 2, displaySize * 2);
            
            // Add glow effect for moving packets
            if (speed > 10) {
                gc.setStroke(Color.LIGHTGREEN);
                gc.setLineWidth(2);
                gc.strokeRect(pos.getX() - displaySize - 1, pos.getY() - displaySize - 1,
                        displaySize * 2 + 2, displaySize * 2 + 2);
            }
            
            gc.setStroke(Color.DARKGREEN);
            gc.setLineWidth(1);
            gc.strokeRect(pos.getX() - displaySize, pos.getY() - displaySize, displaySize * 2, displaySize * 2);
            
        } else if (type == PacketType.TRIANGLE_MESSENGER) {
            // 🔺 مثلثی - Yellow triangle
            gc.setFill(Color.YELLOW);
            
            // Draw triangle
            double[] xPoints = {
                pos.getX(), 
                pos.getX() - displaySize, 
                pos.getX() + displaySize
            };
            double[] yPoints = {
                pos.getY() - displaySize, 
                pos.getY() + displaySize, 
                pos.getY() + displaySize
            };
            gc.fillPolygon(xPoints, yPoints, 3);
            
            // Add glow effect for moving packets
            if (speed > 10) {
                gc.setStroke(Color.LIGHTYELLOW);
                gc.setLineWidth(2);
                gc.strokePolygon(xPoints, yPoints, 3);
            }
            
            gc.setStroke(Color.ORANGE);
            gc.setLineWidth(1);
            gc.strokePolygon(xPoints, yPoints, 3);
            
        } else if (type == PacketType.SMALL_MESSENGER) {
            // ⬡ شش‌ضلعی کوچک - Black hexagon
            gc.setFill(Color.BLACK);
            
            // Draw hexagon
            double[] xPoints = new double[6];
            double[] yPoints = new double[6];
            for (int i = 0; i < 6; i++) {
                double angle = i * Math.PI / 3;
                xPoints[i] = pos.getX() + displaySize * Math.cos(angle);
                yPoints[i] = pos.getY() + displaySize * Math.sin(angle);
            }
            gc.fillPolygon(xPoints, yPoints, 6);
            
            // Add glow effect for moving packets
            if (speed > 10) {
                gc.setStroke(Color.GRAY);
                gc.setLineWidth(2);
                gc.strokePolygon(xPoints, yPoints, 6);
            }
            
            gc.setStroke(Color.DARKGRAY);
            gc.setLineWidth(1);
            gc.strokePolygon(xPoints, yPoints, 6);
            
        } else if (type == PacketType.CONFIDENTIAL) {
            // ⚫ دایره سیاه - Black circle
            gc.setFill(Color.BLACK);
            gc.fillOval(pos.getX() - displaySize, pos.getY() - displaySize, displaySize * 2, displaySize * 2);
            
            // Add glow effect for moving packets
            if (speed > 10) {
                gc.setStroke(Color.GRAY);
                gc.setLineWidth(2);
                gc.strokeOval(pos.getX() - displaySize - 1, pos.getY() - displaySize - 1,
                        displaySize * 2 + 2, displaySize * 2 + 2);
            }
            
            gc.setStroke(Color.DARKGRAY);
            gc.setLineWidth(1);
            gc.strokeOval(pos.getX() - displaySize, pos.getY() - displaySize, displaySize * 2, displaySize * 2);
            
        } else if (type == PacketType.CONFIDENTIAL_PROTECTED) {
            // 🟡 دایره زرد - Yellow circle
            gc.setFill(Color.GOLD);
            gc.fillOval(pos.getX() - displaySize, pos.getY() - displaySize, displaySize * 2, displaySize * 2);
            
            // Add glow effect for moving packets
            if (speed > 10) {
                gc.setStroke(Color.LIGHTYELLOW);
                gc.setLineWidth(2);
                gc.strokeOval(pos.getX() - displaySize - 1, pos.getY() - displaySize - 1,
                        displaySize * 2 + 2, displaySize * 2 + 2);
            }
            
            gc.setStroke(Color.ORANGE);
            gc.setLineWidth(1);
            gc.strokeOval(pos.getX() - displaySize, pos.getY() - displaySize, displaySize * 2, displaySize * 2);
            
        } else if (type == PacketType.BULK_SMALL) {
            // 🔷 شش‌ضلعی آبی - Blue hexagon
            gc.setFill(Color.BLUE);
            drawHexagon(gc, pos.getX(), pos.getY(), displaySize);
            
            // Add glow effect for moving packets
            if (speed > 10) {
                gc.setStroke(Color.LIGHTBLUE);
                gc.setLineWidth(2);
                drawHexagon(gc, pos.getX(), pos.getY(), displaySize + 1);
            }
            
            gc.setStroke(Color.DARKBLUE);
            gc.setLineWidth(1);
            drawHexagon(gc, pos.getX(), pos.getY(), displaySize);
            
        } else if (type == PacketType.BULK_LARGE) {
            // 🔶 هشت‌ضلعی - Octagon
            gc.setFill(Color.PURPLE);
            drawOctagon(gc, pos.getX(), pos.getY(), displaySize);
            
            // Add glow effect for moving packets
            if (speed > 10) {
                gc.setStroke(Color.PLUM);
                gc.setLineWidth(2);
                drawOctagon(gc, pos.getX(), pos.getY(), displaySize + 1);
            }
            
            gc.setStroke(Color.DARKVIOLET);
            gc.setLineWidth(1);
            drawOctagon(gc, pos.getX(), pos.getY(), displaySize);
            
        } else {
            // Default shape for other packet types
        if (packet.getSize() <= 3) {
                // Draw small packets as squares
            gc.setFill(packetColor);
            gc.fillRect(pos.getX() - displaySize, pos.getY() - displaySize, displaySize * 2, displaySize * 2);

            if (speed > 10) {
                gc.setStroke(trailColor);
                gc.setLineWidth(2);
                gc.strokeRect(pos.getX() - displaySize - 1, pos.getY() - displaySize - 1,
                        displaySize * 2 + 2, displaySize * 2 + 2);
            }

            gc.setStroke(borderColor);
            gc.setLineWidth(1);
            gc.strokeRect(pos.getX() - displaySize, pos.getY() - displaySize, displaySize * 2, displaySize * 2);
        } else {
                // Draw larger packets as circles
            gc.setFill(packetColor);
            gc.fillOval(pos.getX() - displaySize, pos.getY() - displaySize, displaySize * 2, displaySize * 2);

            if (speed > 10) {
                gc.setStroke(trailColor);
                gc.setLineWidth(2);
                gc.strokeOval(pos.getX() - displaySize - 1, pos.getY() - displaySize - 1,
                        displaySize * 2 + 2, displaySize * 2 + 2);
            }

            gc.setStroke(borderColor);
            gc.setLineWidth(1);
            gc.strokeOval(pos.getX() - displaySize, pos.getY() - displaySize, displaySize * 2, displaySize * 2);
        }
        }
    }

    /**
     * Draws a hexagon shape for bulk packets.
     */
    private void drawHexagon(GraphicsContext gc, double centerX, double centerY, double radius) {
        double[] xPoints = new double[6];
        double[] yPoints = new double[6];
        
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3.0; // 60 degrees apart
            xPoints[i] = centerX + radius * Math.cos(angle);
            yPoints[i] = centerY + radius * Math.sin(angle);
        }
        
        gc.fillPolygon(xPoints, yPoints, 6);
    }

    /**
     * Draws an octagon shape for large bulk packets.
     */
    private void drawOctagon(GraphicsContext gc, double centerX, double centerY, double radius) {
        double[] xPoints = new double[8];
        double[] yPoints = new double[8];
        
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0; // 45 degrees apart
            xPoints[i] = centerX + radius * Math.cos(angle);
            yPoints[i] = centerY + radius * Math.sin(angle);
        }
        
        gc.fillPolygon(xPoints, yPoints, 8);
    }

    /**
     * Draws a motion trail behind the packet to show movement direction and speed.
     */
    private void drawPacketTrail(Point2D pos, Vec2D velocity, Color trailColor, double packetSize) {
        double speed = velocity.magnitude();
        if (speed < 5) return; // Don't draw trail for very slow packets

        // Calculate trail length based on speed
        double trailLength = Math.min(30, speed / 3);
        Vec2D normalizedVel = velocity.normalize();

        // Draw multiple trail segments with decreasing opacity
        int segments = 5;
        for (int i = 0; i < segments; i++) {
            double segmentDistance = (trailLength / segments) * (i + 1);
            double opacity = 1.0 - (double) i / segments;

            Point2D trailPos = new Point2D(
                    pos.getX() - normalizedVel.getX() * segmentDistance,
                    pos.getY() - normalizedVel.getY() * segmentDistance
            );

            // Create color with opacity
            Color segmentColor = Color.color(
                    trailColor.getRed(),
                    trailColor.getGreen(),
                    trailColor.getBlue(),
                    opacity * 0.6
            );

            gc.setFill(segmentColor);
            double segmentSize = packetSize * (0.3 + 0.7 * opacity);
            gc.fillOval(trailPos.getX() - segmentSize/2, trailPos.getY() - segmentSize/2,
                    segmentSize, segmentSize);
        }
    }


    /**
     * Draws control instructions on the screen.
     */
    private void drawControlInstructions() {
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", 12));

        String[] instructions;

        if (gameController.isEditingMode()) {
            instructions = new String[]{
                    "EDITING MODE:",
                    "Click & Drag Ports - Connect Wires",
                    "Right-Click Wire - Remove Wire",
                    "B - Bend Creation Mode",
                    "M - Wire Merge Mode",
                    "C - Toggle Smooth Wire Curves",
                    "S - Shop",
                    "P - Menu (Pause/Exit)",
                    "ESC - Exit Current Mode",
                    "",
                    "VIEWPORT CONTROLS:",
                    "Mouse Wheel - Zoom In/Out",
                    "Space + Drag - Pan View",
                    "R - Reset View (Show All Systems)",
                    "Auto-scale: " + (autoScaleEnabled ? "ON" : "OFF")
            };
        } else {
            instructions = new String[]{
                    "SIMULATION MODE:",
                    "Arrow Keys - Temporal Navigation",
                    "P - Pause/Resume",
                    "C - Wire Curves (LOCKED during simulation)",
                    "ESC - Return to Editing",
                    "S - Shop",
                    "",
                    "VIEWPORT CONTROLS:",
                    "Mouse Wheel - Zoom In/Out",
                    "Space + Drag - Pan View",
                    "R - Reset View (Show All Systems)"
            };
        }

        int y = 20;
        for (String instruction : instructions) {
            if (instruction.isEmpty()) {
                y += 5; // Smaller gap for empty lines
                continue;
            }
            gc.fillText(instruction, 10, y);
            y += 15;
        }

        // Show current sub-mode and connection status
        if (gameController.isEditingMode()) {
            if (gameController.getInputHandler().isWiringMode()) {
                gc.setFill(Color.YELLOW);
                gc.fillText("WIRING MODE - Click ports to connect", 10, y + 10);
            } else if (gameController.getInputHandler().isBendCreationMode()) {
                gc.setFill(Color.ORANGE);
                gc.fillText("BEND CREATION MODE - Click on wires to add bends (1 coin each)", 10, y + 10);
            } else if (gameController.getInputHandler().isWireMergeMode()) {
                gc.setFill(Color.CYAN);
                gc.fillText("WIRE MERGE MODE - Click two wires to merge them", 10, y + 10);
            } else {
                // Show connection status when not in special modes
                showConnectionStatus(y + 10);
            }
        } else if (gameController.getGameState().isPaused()) {
            gc.setFill(Color.RED);
            gc.fillText("PAUSED - Press P to resume", 10, y + 10);
        } else {
            // Show connection status in simulation mode too
            showConnectionStatus(y + 10);
        }

        // Show wire curve mode status
        boolean smoothWires = gameController.isSmoothWires();
        gc.setFill(smoothWires ? Color.LIME : Color.ORANGE);
        gc.fillText("Wire Curves: " + (smoothWires ? "SMOOTH" : "RIGID"), 10, y + 25);

        // Show wire curve mode indicator in top-right corner
        showWireCurveModeIndicator(smoothWires);

        // Show viewport information
        showViewportInfo(y + 80);
    }

    /**
     * Shows the network connection status.
     */
    private void showConnectionStatus(int y) {
        if (currentLevel == null) return;

        int totalSystems = currentLevel.getSystems().size();

        // Graph connectivity via wiring controller
        boolean graphConnected = false;
        int reachable = 0;
        boolean allPortsConnected = false;
        int[] portCounts = {0, 0};

        if (gameController != null && gameController.getWiringController() != null) {
            graphConnected = gameController.getWiringController().isNetworkConnected(gameController.getGameState());
            reachable = gameController.getWiringController().getReachableSystemCount(gameController.getGameState());
            allPortsConnected = gameController.getWiringController().areAllPortsConnected(gameController.getGameState());
            portCounts = gameController.getWiringController().getPortConnectivityCounts(gameController.getGameState());
        }

        // Status line 1: Connected: Yes/No
        gc.setFill(graphConnected ? Color.LIME : Color.YELLOW);
        gc.fillText("Connected: " + (graphConnected ? "Yes" : "No"), 10, y);

        // Status line 2: Reachable X/Y
        gc.setFill(Color.CYAN);
        gc.fillText("Reachable: " + reachable + "/" + totalSystems + " systems", 10, y + 15);

        // Status line 3: Port Connectivity X/Y
        gc.setFill(allPortsConnected ? Color.LIME : Color.ORANGE);
        gc.fillText("Ports: " + portCounts[0] + "/" + portCounts[1] + " connected", 10, y + 30);

        // Show active packet count during simulation
        if (gameController.isSimulationMode()) {
            int activePackets = (int) gameController.getGameState().getActivePackets().stream()
                    .mapToInt(p -> p.isActive() ? 1 : 0).sum();
            gc.setFill(Color.CYAN);
            gc.fillText("Active Packets: " + activePackets, 10, y + 45);

            // Show wire occupancy status
            int occupiedWires = getOccupiedWireCount();
            int totalWires = getTotalWireCount();
            gc.setFill(Color.ORANGE);
            gc.fillText("Wire Occupancy: " + occupiedWires + "/" + totalWires + " wires occupied", 10, y + 60);
        }
    }

    /**
     * Shows viewport information including scale and position.
     */
    private void showViewportInfo(int y) {
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(javafx.scene.text.Font.font("Arial", 10));

        // Show current scale
        gc.fillText(String.format("Scale: %.2fx", viewportScale), 10, y);

        // Show viewport center
        gc.fillText(String.format("Center: (%.0f, %.0f)", viewportCenter.getX(), viewportCenter.getY()), 10, y + 15);

        // Show canvas size
        gc.fillText(String.format("Canvas: %.0fx%.0f", canvas.getWidth(), canvas.getHeight()), 10, y + 30);

        // Show auto-scale status
        gc.setFill(autoScaleEnabled ? Color.LIME : Color.ORANGE);
        gc.fillText("Auto-scale: " + (autoScaleEnabled ? "ON" : "OFF"), 10, y + 45);
    }

    /**
     * Displays a transient on-screen message near the top-left.
     */
    public void showToast(String message, Color color) {
        if (message == null) return;
        gc.setFill(color != null ? color : Color.ORANGE);
        gc.fillText(message, 10, 40);
    }

    /**
     * Sets the current level and updates the view.
     */
    public void setLevel(GameLevel level) {
        this.currentLevel = level;

        // Reset viewport for new level
        if (autoScaleEnabled) {
            updateViewport();
        }

        // Update the view immediately
        update();
    }

    /**
     * Requests focus for the game view to enable keyboard input.
     */
    public void requestFocus() {
        root.requestFocus();
        canvas.requestFocus();
        
    }

    /**
     * Gets the root node of this view.
     */
    public StackPane getRoot() {
        return root;
    }

    /**
     * Gets the canvas.
     */
    public Canvas getCanvas() {
        return canvas;
    }

    /**
     * Gets the current canvas size.
     */
    public javafx.geometry.Dimension2D getCanvasSize() {
        return new javafx.geometry.Dimension2D(canvas.getWidth(), canvas.getHeight());
    }

    /**
     * Gets the root pane for adding overlays.
     */
    public StackPane getRootPane() {
        return root;
    }

    /**
     * Adds the pause menu overlay to the scene graph.
     */
    public void addPauseOverlay(javafx.scene.layout.Pane pauseOverlay) {
        if (!root.getChildren().contains(pauseOverlay)) {
            root.getChildren().add(pauseOverlay);
            
        }
    }

    /**
     * Adds the shop overlay to the scene graph.
     */
    public void addShopOverlay(javafx.scene.layout.Pane shopOverlay) {
        if (!root.getChildren().contains(shopOverlay)) {
            root.getChildren().add(shopOverlay);
            
        }
    }

    /**
     * Adds the HUD overlay to the scene graph.
     */
    public void addHUDOverlay(javafx.scene.layout.Pane hudOverlay) {
        if (!root.getChildren().contains(hudOverlay)) {
            // Ensure HUD is positioned correctly
            hudOverlay.setLayoutX(20);
            // Bottom position is handled by HUD's own layout listener

            // Add HUD to the scene
            root.getChildren().add(hudOverlay);

            // Force the HUD to be visible
            hudOverlay.setVisible(true);

            // Debug: Print HUD addition status
            
        }
    }

    /**
     * Adds the HUD indicator button to the scene graph.
     * This button is always visible and allows users to show the HUD.
     */
    public void addHUDIndicator(javafx.scene.control.Button hudIndicator) {
        // Method kept for compatibility but does nothing - HUD is always visible
        
    }

    /**
     * Shows wire preview during wiring mode with enhanced visual feedback.
     */
    public void showWirePreview(Point2D start, Point2D end, boolean isValid) {
        wirePreviewStart = start;
        wirePreviewEnd = end;
        wirePreviewValid = isValid;
        showWirePreview = true; // Set flag to true to indicate preview is ready
    }

    /**
     * Clears wire preview.
     */
    public void clearWirePreview() {
        wirePreviewStart = null;
        wirePreviewEnd = null;
        wirePreviewValid = false;
        showWirePreview = false; // Clear flag
    }

    /**
     * Renders the wire preview with enhanced visual feedback.
     * This method is called during the main update loop when viewport transformation is active.
     */
    private void renderWirePreview() {
        if (wirePreviewStart == null || wirePreviewEnd == null) {
            return;
        }

        // Draw preview line with enhanced visibility
        Color previewColor = wirePreviewValid ? Color.LIME : Color.RED;
        gc.setStroke(previewColor);
        gc.setLineWidth(4);
        gc.setLineDashes(8, 4); // Dashed line for preview
        gc.strokeLine(wirePreviewStart.getX(), wirePreviewStart.getY(), wirePreviewEnd.getX(), wirePreviewEnd.getY());

        // Draw start point with pulsing effect
        gc.setFill(Color.LIME);
        gc.fillOval(wirePreviewStart.getX() - 6, wirePreviewStart.getY() - 6, 12, 12);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeOval(wirePreviewStart.getX() - 6, wirePreviewStart.getY() - 6, 12, 12);

        // Draw end point with validation feedback
        if (wirePreviewValid) {
            gc.setFill(Color.LIME);
            gc.fillOval(wirePreviewEnd.getX() - 6, wirePreviewEnd.getY() - 6, 12, 12);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            gc.strokeOval(wirePreviewEnd.getX() - 6, wirePreviewEnd.getY() - 6, 12, 12);
        } else {
            // Red X for invalid connection
            gc.setStroke(Color.RED);
            gc.setLineWidth(3);
            gc.strokeLine(wirePreviewEnd.getX() - 8, wirePreviewEnd.getY() - 8, wirePreviewEnd.getX() + 8, wirePreviewEnd.getY() + 8);
            gc.strokeLine(wirePreviewEnd.getX() - 8, wirePreviewEnd.getY() + 8, wirePreviewEnd.getX() + 8, wirePreviewEnd.getY() - 8);
        }

        // Calculate and display wire length
        double distance = wirePreviewStart.distanceTo(wirePreviewEnd);
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", 12));
        String lengthText = String.format("Length: %.1f", distance);
        gc.fillText(lengthText, (wirePreviewStart.getX() + wirePreviewEnd.getX()) / 2, (wirePreviewStart.getY() + wirePreviewEnd.getY()) / 2 - 10);

        gc.setLineDashes(null); // Reset to solid line
    }

    /**
     * Gets the number of wires currently occupied by packets.
     */
    private int getOccupiedWireCount() {
        if (currentLevel == null) return 0;

        int count = 0;
        for (WireConnection connection : currentLevel.getWireConnections()) {
            if (connection.isActive() && connection.isOccupied()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets the total number of active wires.
     */
    private int getTotalWireCount() {
        if (currentLevel == null) return 0;

        int count = 0;
        for (WireConnection connection : currentLevel.getWireConnections()) {
            if (connection.isActive()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Draws a visual indicator in the top-right corner showing the current wire rendering mode.
     */
    private void showWireCurveModeIndicator(boolean smoothWires) {
        double indicatorWidth = 120;
        double indicatorHeight = 30;
        double indicatorX = canvas.getWidth() - indicatorWidth - 20;
        double indicatorY = 20;

        // Different background color based on mode
        if (gameController.isSimulationMode()) {
            gc.setFill(Color.DARKGRAY); // Darker background for locked state
        } else {
            gc.setFill(Color.BLACK);
        }
        gc.fillRect(indicatorX, indicatorY, indicatorWidth, indicatorHeight);

        // Different text color and content based on mode
        if (gameController.isSimulationMode()) {
            gc.setFill(Color.GRAY);
            gc.setFont(javafx.scene.text.Font.font("Arial", 10));
            gc.fillText(smoothWires ? "SMOOTH (LOCKED)" : "RIGID (LOCKED)", indicatorX + 5, indicatorY + 20);
        } else {
            gc.setFill(smoothWires ? Color.LIME : Color.ORANGE);
            gc.setFont(javafx.scene.text.Font.font("Arial", 12));
            gc.fillText(smoothWires ? "SMOOTH WIRES" : "RIGID WIRES", indicatorX + 10, indicatorY + 20);
        }
    }

    /**
     * Draws the Run button on canvas.
     */
    private void drawRunButton() {
        // Show Run button only in editing mode, not in simulating mode
        if (!gameController.isEditingMode()) {
            isRunButtonVisible = false;
            return;
        }

        isRunButtonVisible = true;

        // Position button in top-right corner
        if (canvas != null) {
            double canvasWidth = canvas.getWidth();
            runButtonX = canvasWidth - runButtonWidth - 20; // 20px margin from right edge
        }

        // Check if simulation can start
        boolean canStart = canStartSimulation();
        
        // Draw button background
        if (canStart) {
            // Ready to start - green button
            gc.setFill(Color.rgb(76, 175, 80)); // Green
        } else {
            // Not ready - red button
            gc.setFill(Color.rgb(244, 67, 54)); // Red
        }
        
        // Draw button rectangle with rounded corners effect
        gc.fillRoundRect(runButtonX, runButtonY, runButtonWidth, runButtonHeight, 8, 8);
        
        // Draw button border
        gc.setStroke(Color.rgb(255, 255, 255, 0.3));
        gc.setLineWidth(1);
        gc.strokeRoundRect(runButtonX, runButtonY, runButtonWidth, runButtonHeight, 8, 8);
        
        // Draw button text
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", 14));
        String buttonText = "START";
        
        // Center text in button
        double textX = runButtonX + (runButtonWidth / 2) - 20; // Approximate centering for "START"
        double textY = runButtonY + (runButtonHeight / 2) + 5; // Vertical centering
        gc.fillText(buttonText, textX, textY);
    }

    /**
     * Draws error message on screen if there is one.
     */
    private void drawErrorMessage() {
        if (currentErrorMessage == null) return;

        // Check if error message should still be displayed
        errorMessageFrameCount++;
        if (errorMessageFrameCount > ERROR_MESSAGE_DURATION_FRAMES) {
            currentErrorMessage = null;
            return;
        }

        // Position error message below the START button
        double messageX = runButtonX;
        double messageY = runButtonY + runButtonHeight + 20;

        // Draw error message background
        gc.setFill(Color.rgb(244, 67, 54, 0.9)); // Red background with transparency
        double messageWidth = 300;
        double messageHeight = 60;
        gc.fillRoundRect(messageX - 50, messageY - 10, messageWidth, messageHeight, 8, 8);

        // Draw error message border
        gc.setStroke(Color.rgb(255, 255, 255, 0.5));
        gc.setLineWidth(2);
        gc.strokeRoundRect(messageX - 50, messageY - 10, messageWidth, messageHeight, 8, 8);

        // Draw error message text
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", 12));
        
        // Word wrap the message if it's too long
        String[] words = currentErrorMessage.split(" ");
        StringBuilder line1 = new StringBuilder();
        StringBuilder line2 = new StringBuilder();
        
        boolean useSecondLine = false;
        for (String word : words) {
            if (line1.length() + word.length() > 35 && !useSecondLine) {
                useSecondLine = true;
            }
            
            if (useSecondLine) {
                if (line2.length() > 0) line2.append(" ");
                line2.append(word);
            } else {
                if (line1.length() > 0) line1.append(" ");
                line1.append(word);
            }
        }
        
        gc.fillText(line1.toString(), messageX - 40, messageY + 10);
        if (line2.length() > 0) {
            gc.fillText(line2.toString(), messageX - 40, messageY + 30);
        }
    }

    /**
     * Handles Run button click with validation and error messages.
     */
    private void handleRunButtonClick() {
        // Only allow Run button in editing mode
        if (!gameController.isEditingMode()) {
            return;
        }

        // Check all conditions and show appropriate error message
        boolean allPortsConnected = gameController.getWiringController().areAllPortsConnected(gameController.getGameState());
        boolean referenceSystemsReady = gameController.areReferenceSystemsReady();
        boolean noWireCollisions = !gameController.doAnyWiresPassOverSystems();

        if (!allPortsConnected) {
            showSimulationError("All ports must be connected!");
            return;
        }

        if (!referenceSystemsReady) {
            showSimulationError("Reference systems not ready. Connect source to destination.");
            return;
        }

        if (!noWireCollisions) {
            showSimulationError("Some wires pass over systems. Move wires away from systems.");
            return;
        }

        // All conditions met - start simulation
        gameController.enterSimulationMode();
    }

    /**
     * Handles Simulate button click to enter temporal navigation mode.
     */
    private void handleSimulateButtonClick() {
        // Only allow Simulate button in editing mode
        if (!gameController.isEditingMode()) {
            return;
        }

        // Check all conditions and show appropriate error message
        boolean allPortsConnected = gameController.getWiringController().areAllPortsConnected(gameController.getGameState());
        boolean referenceSystemsReady = gameController.areReferenceSystemsReady();
        boolean noWireCollisions = !gameController.doAnyWiresPassOverSystems();

        if (!allPortsConnected) {
            showSimulationError("All ports must be connected!");
            return;
        }

        if (!referenceSystemsReady) {
            showSimulationError("Reference systems not ready. Connect source to destination.");
            return;
        }

        if (!noWireCollisions) {
            showSimulationError("Some wires pass over systems. Move wires away from systems.");
            return;
        }

        // All conditions met - enter simulation mode
        gameController.enterSimulatingMode();
    }

    /**
     * Handles Exit Simulate button click to exit temporal navigation mode.
     */
    private void handleExitSimulateButtonClick() {
        if (!gameController.isSimulatingMode()) {
            return;
        }

        // Exit simulation mode and reset to initial state
        gameController.exitSimulatingMode();
    }

    /**
     * Checks if simulation can start based on all conditions.
     */
    private boolean canStartSimulation() {
        if (!gameController.isEditingMode()) {
            return false;
        }

        boolean allPortsConnected = gameController.getWiringController().areAllPortsConnected(gameController.getGameState());
        boolean referenceSystemsReady = gameController.areReferenceSystemsReady();
        boolean noWireCollisions = !gameController.doAnyWiresPassOverSystems();

        return allPortsConnected && referenceSystemsReady && noWireCollisions;
    }

    // Error message display
    private String currentErrorMessage = null;
    private int errorMessageFrameCount = 0;
    private static final int ERROR_MESSAGE_DURATION_FRAMES = 180; // 3 seconds at 60fps

    /**
     * Shows simulation error message to user on screen.
     */
    private void showSimulationError(String message) {
        java.lang.System.out.println("SIMULATION ERROR: " + message);
        currentErrorMessage = message;
        errorMessageFrameCount = 0;
    }

    /**
     * Draws the Simulate and Exit buttons on canvas.
     */
    private void drawSimulateButtons() {
        // Show buttons in both editing and simulating modes
        if (!gameController.isEditingMode() && !gameController.isSimulatingMode()) {
            isSimulateButtonVisible = false;
            isExitSimulateButtonVisible = false;
            return;
        }

        // Check if simulation can start and current mode
        boolean canStart = canStartSimulation();
        boolean isSimulating = gameController.isSimulatingMode();

        // Position buttons in top-right corner
        if (canvas != null) {
            double canvasWidth = canvas.getWidth();
            if (isSimulating) {
                // In simulating mode, position Exit button where Run button was
                exitSimulateButtonX = canvasWidth - exitSimulateButtonWidth - 20;
            } else {
                // In editing mode, position Simulate button below Run button
                simulateButtonX = canvasWidth - simulateButtonWidth - 20;
            }
        }

        // Draw Simulate button
        if (!isSimulating) {
            isSimulateButtonVisible = true;
            isExitSimulateButtonVisible = false;
            
            // Draw button background
            if (canStart) {
                // Ready to start - green button
                gc.setFill(Color.rgb(76, 175, 80)); // Green
            } else {
                // Not ready - red button
                gc.setFill(Color.rgb(244, 67, 54)); // Red
            }
            
            // Draw button rectangle with rounded corners effect
            gc.fillRoundRect(simulateButtonX, simulateButtonY, simulateButtonWidth, simulateButtonHeight, 8, 8);
            
            // Draw button border
            gc.setStroke(Color.rgb(255, 255, 255, 0.3));
            gc.setLineWidth(1);
            gc.strokeRoundRect(simulateButtonX, simulateButtonY, simulateButtonWidth, simulateButtonHeight, 8, 8);
            
            // Draw button text
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 14));
            String buttonText = "SIMULATE";
            double textWidth = gc.getFont().getSize() * buttonText.length() * 0.6;
            double textX = simulateButtonX + (simulateButtonWidth - textWidth) / 2;
            double textY = simulateButtonY + simulateButtonHeight / 2 + 5;
            gc.fillText(buttonText, textX, textY);
        }
        // Draw Exit Simulate button
        else {
            isSimulateButtonVisible = false;
            isExitSimulateButtonVisible = true;
            
            // Draw button background - orange for exit
            gc.setFill(Color.rgb(255, 152, 0)); // Orange
            
            // Draw button rectangle with rounded corners effect
            gc.fillRoundRect(exitSimulateButtonX, exitSimulateButtonY, exitSimulateButtonWidth, exitSimulateButtonHeight, 8, 8);
            
            // Draw button border
            gc.setStroke(Color.rgb(255, 255, 255, 0.3));
            gc.setLineWidth(1);
            gc.strokeRoundRect(exitSimulateButtonX, exitSimulateButtonY, exitSimulateButtonWidth, exitSimulateButtonHeight, 8, 8);
            
            // Draw button text
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 14));
            String buttonText = "EXIT SIM";
            double textWidth = gc.getFont().getSize() * buttonText.length() * 0.6;
            double textX = exitSimulateButtonX + (exitSimulateButtonWidth - textWidth) / 2;
            double textY = exitSimulateButtonY + exitSimulateButtonHeight / 2 + 5;
            gc.fillText(buttonText, textX, textY);
        }
    }

    /**
     * Checks if the given coordinates are within the Run button area.
     */
    private boolean isRunButtonClicked(double x, double y) {
        if (!isRunButtonVisible) return false;
        
        return x >= runButtonX && x <= runButtonX + runButtonWidth &&
               y >= runButtonY && y <= runButtonY + runButtonHeight;
    }

    /**
     * Checks if the given coordinates are within the Simulate button area.
     */
    private boolean isSimulateButtonClicked(double x, double y) {
        if (!isSimulateButtonVisible) return false;
        
        return x >= simulateButtonX && x <= simulateButtonX + simulateButtonWidth &&
               y >= simulateButtonY && y <= simulateButtonY + simulateButtonHeight;
    }

    /**
     * Checks if the given coordinates are within the Exit Simulate button area.
     */
    private boolean isExitSimulateButtonClicked(double x, double y) {
        if (!isExitSimulateButtonVisible) return false;
        
        return x >= exitSimulateButtonX && x <= exitSimulateButtonX + exitSimulateButtonWidth &&
               y >= exitSimulateButtonY && y <= exitSimulateButtonY + exitSimulateButtonHeight;
    }
}
