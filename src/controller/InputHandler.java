package controller;

import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyEvent;
import model.Port;
import model.Point2D;
import model.WireConnection;
import model.System;
import model.AbilityType;
import model.GameState;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import model.WireBend;

/**
 * Handles keyboard and mouse input for the game.
 * Supports custom key bindings with duplicate prevention.
 */
public class InputHandler {
    private GameController gameController;
    private boolean isWiringMode;
    private boolean isBendCreationMode;
    private boolean isWireMergeMode;
    private boolean isSystemMovementMode;
    private boolean isAbilityTargetingMode;
    private AbilityType pendingAbility;
    private Port selectedPort;
    private WireConnection selectedWire;
    private WireConnection firstSelectedWireForMerge;
    private System selectedSystem;
    private int selectedBendIndex;

    // Space key tracking for viewport panning
    private boolean isSpacePressed = false;

    // Custom key bindings
    private Map<String, KeyCode> keyBindings;
    private Map<KeyCode, String> reverseKeyBindings;

    // Default key bindings
    private static final Map<String, KeyCode> DEFAULT_BINDINGS = new HashMap<>();
    static {
        DEFAULT_BINDINGS.put("temporal_backward", KeyCode.LEFT);
        DEFAULT_BINDINGS.put("temporal_forward", KeyCode.RIGHT);
        DEFAULT_BINDINGS.put("wiring_mode", KeyCode.SHIFT);
        DEFAULT_BINDINGS.put("bend_creation_mode", KeyCode.B);
        DEFAULT_BINDINGS.put("wire_merge_mode", KeyCode.M);
        DEFAULT_BINDINGS.put("system_movement_mode", KeyCode.G);
        DEFAULT_BINDINGS.put("toggle_indicators", KeyCode.I);
        DEFAULT_BINDINGS.put("shop_toggle", KeyCode.S);
        DEFAULT_BINDINGS.put("pause_resume", KeyCode.P);
        DEFAULT_BINDINGS.put("start_simulation", KeyCode.R);
        DEFAULT_BINDINGS.put("escape", KeyCode.ESCAPE);
        DEFAULT_BINDINGS.put("toggle_smooth_wires", KeyCode.C); // C for Curves
    }

    public InputHandler(GameController gameController) {
        this.gameController = gameController;
        this.isWiringMode = false;
        this.isBendCreationMode = false;
        this.isWireMergeMode = false;
        this.isSystemMovementMode = false;
        this.isAbilityTargetingMode = false;
        this.pendingAbility = null;
        this.selectedPort = null;
        this.selectedWire = null;
        this.firstSelectedWireForMerge = null;
        this.selectedBendIndex = -1;

        // Initialize key bindings with defaults
        this.keyBindings = new HashMap<>(DEFAULT_BINDINGS);
        this.reverseKeyBindings = new HashMap<>();
        for (Map.Entry<String, KeyCode> entry : keyBindings.entrySet()) {
            reverseKeyBindings.put(entry.getValue(), entry.getKey());
        }
    }

    /**
     * Remaps a key binding.
     */
    public boolean remapKey(String action, KeyCode newKey) {
        // Check if the new key is already assigned to another action
        if (reverseKeyBindings.containsKey(newKey)) {
            String existingAction = reverseKeyBindings.get(newKey);
            if (!existingAction.equals(action)) {
                return false; // Duplicate assignment
            }
        }

        // Remove old binding
        KeyCode oldKey = keyBindings.get(action);
        if (oldKey != null) {
            reverseKeyBindings.remove(oldKey);
        }

        // Set new binding
        keyBindings.put(action, newKey);
        reverseKeyBindings.put(newKey, action);

        return true;
    }

    /**
     * Gets the current key binding for an action.
     */
    public KeyCode getKeyBinding(String action) {
        return keyBindings.get(action);
    }

    /**
     * Resets key bindings to defaults.
     */
    public void resetKeyBindings() {
        this.keyBindings = new HashMap<>(DEFAULT_BINDINGS);
        this.reverseKeyBindings.clear();
        for (Map.Entry<String, KeyCode> entry : keyBindings.entrySet()) {
            reverseKeyBindings.put(entry.getValue(), entry.getKey());
        }
    }

    /**
     * Gets all current key bindings.
     */
    public Map<String, KeyCode> getAllKeyBindings() {
        return new HashMap<>(keyBindings);
    }

    /**
     * Handles key press events.
     */
    public void handleKeyPress(KeyEvent event) {
        KeyCode code = event.getCode();
        java.lang.System.out.println("DEBUG: handleKeyPress called with key: " + code.getName());

        // Handle space key for viewport panning
        if (code == KeyCode.SPACE) {
            isSpacePressed = true;
            java.lang.System.out.println("DEBUG: Space key pressed, setting isSpacePressed = true");
            return; // Don't process as a regular key binding
        }

        String action = reverseKeyBindings.get(code);
        java.lang.System.out.println("DEBUG: Action for key " + code.getName() + ": " + action);

        if (action == null) {
            java.lang.System.out.println("DEBUG: No binding found for key " + code.getName());
            return; // No binding for this key
        }

        switch (action) {
            case "temporal_backward":
                // Temporal navigation - move backward in time
                handleTemporalNavigation(-1);
                break;
            case "temporal_forward":
                // Temporal navigation - move forward in time
                handleTemporalNavigation(1);
                break;
            case "wiring_mode":
                // Enter wiring mode
                isWiringMode = true;
                break;
            case "bend_creation_mode":
                // Toggle bend creation mode
                isBendCreationMode = !isBendCreationMode;
                isWiringMode = false; // Exit wiring mode if active
                isWireMergeMode = false; // Exit wire merge mode if active
                selectedPort = null;
                selectedWire = null;
                firstSelectedWireForMerge = null;
                selectedBendIndex = -1;
                java.lang.System.out.println("DEBUG: Bend creation mode toggled: " + (isBendCreationMode ? "ON" : "OFF"));
                break;
            case "wire_merge_mode":
                // Toggle wire merge mode
                isWireMergeMode = !isWireMergeMode;
                isWiringMode = false; // Exit wiring mode if active
                isBendCreationMode = false; // Exit bend creation mode if active
                selectedPort = null;
                selectedWire = null;
                firstSelectedWireForMerge = null;
                selectedBendIndex = -1;
                java.lang.System.out.println("Wire merge mode: " + (isWireMergeMode ? "ON - Click two wires to merge them" : "OFF"));
                break;
            case "system_movement_mode":
                // Toggle system movement mode (requires Scroll of Sisyphus)
                if (gameController.isAbilityActive(model.AbilityType.SCROLL_OF_SISYPHUS)) {
                    isSystemMovementMode = !isSystemMovementMode;
                    isWiringMode = false; // Exit other modes
                    isBendCreationMode = false;
                    isWireMergeMode = false;
                    selectedPort = null;
                    selectedWire = null;
                    firstSelectedWireForMerge = null;
                    selectedSystem = null;
                    selectedBendIndex = -1;
                    java.lang.System.out.println("System movement mode: " + (isSystemMovementMode ? "ON - Click and drag systems to move them" : "OFF"));
                } else {
                    java.lang.System.out.println("System movement requires Scroll of Sisyphus ability");
                }
                break;
            case "toggle_indicators":
                // System indicators are always ON - no toggle needed
                java.lang.System.out.println("System indicators are always visible");
                break;
            case "shop_toggle":
                // Toggle shop
                toggleShop();
                break;
            case "pause_resume":
                // Pause/resume game or open pause menu in editing mode
                handlePauseOrMenu();
                break;
            case "start_simulation":
                // Simulation auto-starts when all systems are connected - no manual start needed
                java.lang.System.out.println("Simulation starts automatically when all systems are connected");
                break;
            case "escape":
                // Exit current mode
                if (isWiringMode) {
                    isWiringMode = false;
                    selectedPort = null;
                } else if (isBendCreationMode) {
                    isBendCreationMode = false;
                    selectedWire = null;
                    selectedBendIndex = -1;
                } else if (isWireMergeMode) {
                    isWireMergeMode = false;
                    firstSelectedWireForMerge = null;
                    java.lang.System.out.println("Wire merge mode cancelled");
                } else if (isSystemMovementMode) {
                    isSystemMovementMode = false;
                    selectedSystem = null;
                    java.lang.System.out.println("System movement mode cancelled");
                } else if (gameController.isSimulationMode()) {
                    // Return to editing mode from simulation
                    gameController.enterEditingMode();
                }
                break;
            case "toggle_smooth_wires":
                // Toggle smooth wire curves
                gameController.toggleSmoothWires();
                java.lang.System.out.println("Smooth wire curves toggled: " + (gameController.isSmoothWires() ? "ON" : "OFF"));
                break;
        }
    }

    /**
     * Handles key release events.
     */
    public void handleKeyRelease(KeyEvent event) {
        KeyCode code = event.getCode();

        // Handle space key release for viewport panning
        if (code == KeyCode.SPACE) {
            isSpacePressed = false;
            return;
        }

        String action = reverseKeyBindings.get(code);

        if ("wiring_mode".equals(action)) {
            // Exit wiring mode
            isWiringMode = false;
            selectedPort = null;
        }
        // Note: Bend creation mode is a toggle (press B to toggle on/off),
        // so we intentionally do not disable it on key release.
        if ("system_movement_mode".equals(action)) {
            // Exit system movement mode
            isSystemMovementMode = false;
            selectedSystem = null;
        }
    }

    /**
     * Handles mouse press events.
     */
    public void handleMousePress(MouseEvent event) {
        if (gameController == null) return;

        java.lang.System.out.println("Mouse press at: " + event.getX() + ", " + event.getY() + " with button: " + event.getButton());

        if (event.getButton() == MouseButton.PRIMARY) {
            if (gameController.isEditingMode()) {
                if (isWiringMode) {
                    handleWiringMousePress(event);
                } else if (isBendCreationMode) {
                    java.lang.System.out.println("DEBUG: Mouse press in bend creation mode - processing bend creation");
                    handleBendCreationMousePress(event);
                } else if (isWireMergeMode) {
                    handleWireMergeMousePress(event);
                } else if (isSystemMovementMode) {
                    handleSystemMovementMousePress(event);
                } else {
                    // Check if clicking on a port to start wiring
                    Port port = findPortAtPosition(event.getX(), event.getY());
                    if (port != null) {
                        java.lang.System.out.println("Port found at mouse position: " + port.getShape() + " " + (port.isInput() ? "input" : "output") + " at " + port.getPosition());
                        // Start wiring from this port
                        selectedPort = port;
                        isWiringMode = true;
                        java.lang.System.out.println("Started wiring from port - drag to another port to connect");
                        showWirePreview(event);
                    } else {
                        java.lang.System.out.println("No port found at mouse position");
                        handleRegularMousePress(event);
                    }
                }
            } else {
                // In simulation mode, only allow regular mouse interactions
                handleRegularMousePress(event);
            }
        } else if (event.getButton() == MouseButton.SECONDARY) {
            // Right-click for wire removal (only in editing mode)
            if (gameController.isEditingMode()) {
                handleRightClickWireRemoval(event);
            }
        }
    }

    /**
     * Handles mouse drag events.
     */
    public void handleMouseDrag(MouseEvent event) {
        // Only allow wiring and bend operations in editing mode
        if (gameController.isEditingMode()) {
            if (isWiringMode && selectedPort != null) {
                // Show wire preview
                showWirePreview(event);
            } else if (isBendCreationMode && selectedWire != null && selectedBendIndex >= 0) {
                // Move bend with maximum freedom - convert screen coordinates to world coordinates for proper positioning
                Point2D worldPosition = gameController.getGameView().screenToWorld(event.getX(), event.getY());
                boolean success = gameController.getWiringController().moveBendFreely(
                        selectedWire, selectedBendIndex, worldPosition, gameController.getGameState()
                );

                if (!success) {
                    java.lang.System.out.println("Cannot move bend to this position (basic constraint violation)");
                }
            } else if (isSystemMovementMode && selectedSystem != null) {
                // Move system
                Point2D newPosition = new Point2D(event.getX(), event.getY());
                boolean success = gameController.getWiringController().moveSystem(
                        selectedSystem, newPosition, gameController.getGameState()
                );

                if (!success) {
                    java.lang.System.out.println("Cannot move system to this position (wire constraints or collision)");
                }
            }
        }
    }

    /**
     * Handles mouse release events.
     */
    public void handleMouseRelease(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            if (isWiringMode && selectedPort != null) {
                // Try to complete wire connection
                Port targetPort = findPortAtPosition(event.getX(), event.getY());
                if (targetPort != null && targetPort != selectedPort) {
                    // Try to create connection
                    createWireConnection(selectedPort, targetPort);
                }
                // Reset wiring state
                selectedPort = null;
                isWiringMode = false;
                clearWirePreview();
                java.lang.System.out.println("Wiring completed");
            } else if (isBendCreationMode) {
                // Clear bend selection
                selectedWire = null;
                selectedBendIndex = -1;
            } else if (isSystemMovementMode) {
                // Clear system selection
                selectedSystem = null;
                java.lang.System.out.println("System movement completed");
            } else if (isWireMergeMode) {
                // Wire merge mode is handled in press, nothing needed on release
            }
        }
    }

    /**
     * Handles temporal navigation with improved step sizes and visual feedback.
     */
    private void handleTemporalNavigation(int direction) {
        if (gameController != null && gameController.isSimulationMode()) {
            double currentProgress = gameController.getGameState().getTemporalProgress();
            double levelDuration = gameController.getGameState().getCurrentLevel().getLevelDuration();
            double timeStep = 0.5; // Smaller steps for more precise navigation
            double newProgress;

            if (direction > 0) {
                // Move forward in time
                newProgress = Math.min(currentProgress + timeStep, levelDuration);
                java.lang.System.out.println("Temporal Navigation: Forward to " + String.format("%.1f", newProgress) + "s");
            } else {
                // Move backward in time
                newProgress = Math.max(0.0, currentProgress - timeStep);
                java.lang.System.out.println("Temporal Navigation: Backward to " + String.format("%.1f", newProgress) + "s");
            }

            // Only update if time actually changed
            if (Math.abs(newProgress - currentProgress) > 0.01) {
                // Update packet positions and temporal progress
                gameController.updatePacketPositionsForTime(newProgress);
                java.lang.System.out.println("Time changed from " + String.format("%.1f", currentProgress) + "s to " + String.format("%.1f", newProgress) + "s");
            }
        } else if (gameController != null && gameController.isEditingMode()) {
            java.lang.System.out.println("Temporal navigation only available in simulation mode. Press R to start simulation.");
        }
    }

    /**
     * Toggles the shop view.
     */
    private void toggleShop() {
        if (gameController != null) {
            gameController.toggleShop();
        }
    }

    /**
     * Starts the simulation (Run button functionality).
     */
    private void startSimulation() {
        if (gameController != null) {
            gameController.enterSimulationMode();
        }
    }

    /**
     * Toggles system indicators on/off globally.
     * Indicators are drawn only if this global flag is ON and the system is fully connected.
     */
    private void toggleSystemIndicators() {
        if (gameController == null || gameController.getGameState() == null) {
            java.lang.System.out.println("ERROR: Cannot toggle indicators - gameController or gameState is null");
            return;
        }

        GameState state = gameController.getGameState();
        boolean oldFlag = state.isShowSystemIndicators();
        boolean newFlag = !oldFlag;
        state.setShowSystemIndicators(newFlag);

        java.lang.System.out.println("System indicators: " + (oldFlag ? "OFF" : "ON") + " -> " + (newFlag ? "ON" : "OFF"));

        // Request immediate view update to show/hide indicators
        if (gameController.getGameView() != null) {
            gameController.getGameView().update();
        } else {
            java.lang.System.out.println("ERROR: GameView is null, cannot request update");
        }
    }

    /**
     * Toggles pause/resume.
     */
    private void handlePauseOrMenu() {
        if (gameController == null) return;
        // If in simulation, toggle pause and show/hide pause overlay
        if (gameController.isSimulationMode()) {
            if (gameController.getGameState().isPaused()) {
                gameController.resumeGame();
                if (gameController.getPauseView() != null && gameController.getPauseView().isVisible()) {
                    gameController.getPauseView().hide();
                }
            } else {
                gameController.pauseGame();
                if (gameController.getPauseView() != null) {
                    gameController.getPauseView().show();
                }
            }
            return;
        }
        // If in editing mode, open the pause/menu overlay for navigation
        if (gameController.isEditingMode()) {
            if (gameController.getPauseView() != null) {
                gameController.getPauseView().toggleVisibility();
            }
        }
    }

    /**
     * Handles mouse press in wiring mode.
     */
    private void handleWiringMousePress(MouseEvent event) {
        // Find port at mouse position
        Port port = findPortAtPosition(event.getX(), event.getY());

        if (port != null) {
            if (selectedPort == null) {
                // Select first port
                selectedPort = port;
                showWirePreview(event);
            } else if (selectedPort != port) {
                // Try to create connection
                createWireConnection(selectedPort, port);
                selectedPort = null;
                clearWirePreview();
            }
        }
    }

    /**
     * Handles mouse press in bend creation mode.
     */
    private void handleBendCreationMousePress(MouseEvent event) {
        // Convert screen coordinates to world coordinates for proper wire detection
        Point2D worldClickPos = gameController.getGameView().screenToWorld(event.getX(), event.getY());
        java.lang.System.out.println("DEBUG: Bend creation click - Screen coordinates: (" + event.getX() + ", " + event.getY() + ")");
        java.lang.System.out.println("DEBUG: Bend creation click - World coordinates: (" + worldClickPos.getX() + ", " + worldClickPos.getY() + ")");
        java.lang.System.out.println("DEBUG: Viewport info - Scale: " + gameController.getGameView().getViewportScale() + ", Offset: " + gameController.getGameView().getViewportOffset());

        // First, check if clicking on an existing bend for movement
        WireConnection wireWithBend = findWireWithBendAtPosition(worldClickPos);
        if (wireWithBend != null) {
            selectedWire = wireWithBend;
            selectedBendIndex = findBendIndexAtPosition(wireWithBend, worldClickPos);
            java.lang.System.out.println("DEBUG: Selected bend " + selectedBendIndex + " for movement");
            return;
        }

        // If not clicking on a bend, try to add a new bend
        WireConnection wireAtPosition = findWireAtPosition(worldClickPos);

        if (wireAtPosition != null) {
            java.lang.System.out.println("DEBUG: Found wire for bend creation: " + wireAtPosition.getId());
            // Try to add a bend to this wire
            boolean success = gameController.getWiringController().addBendToWire(
                    wireAtPosition, worldClickPos, gameController.getGameState()
            );

            if (success) {
                // Play success sound if available
                if (gameController.getSoundManager() != null) {
                    gameController.getSoundManager().playWireConnectSound();
                }
                java.lang.System.out.println("DEBUG: Bend added successfully! Cost: 1 coin");
            } else {
                java.lang.System.out.println("DEBUG: Failed to add bend. Check coin balance or system collision.");
            }
        } else {
            java.lang.System.out.println("DEBUG: No wire found at click position.");
        }
    }

    /**
     * Finds a wire with a bend at the given position.
     */
    private WireConnection findWireWithBendAtPosition(Point2D position) {
        if (gameController == null || gameController.getGameState() == null) {
            return null;
        }

        for (WireConnection connection : gameController.getGameState().getWireConnections()) {
            if (!connection.isActive()) continue;

            for (WireBend bend : connection.getBends()) {
                if (position.distanceTo(bend.getPosition()) <= 15.0) { // Further increased bend radius for maximum ease of clicking
                    return connection;
                }
            }
        }

        return null;
    }

    /**
     * Finds the index of a bend at the given position.
     */
    private int findBendIndexAtPosition(WireConnection connection, Point2D position) {
        for (int i = 0; i < connection.getBends().size(); i++) {
            WireBend bend = connection.getBends().get(i);
            if (position.distanceTo(bend.getPosition()) <= 15.0) { // Further increased bend radius for maximum ease of clicking
                return i;
            }
        }
        return -1;
    }

    /**
     * Handles mouse release in wiring mode.
     */
    private void handleWiringMouseRelease(MouseEvent event) {
        // Clear wire preview
        clearWirePreview();
    }

    /**
     * Handles regular mouse press (non-wiring mode).
     */
    private void handleRegularMousePress(MouseEvent event) {
        // Handle system selection, menu clicks, etc.
        java.lang.System.out.println("Regular mouse press at: " + event.getX() + ", " + event.getY());
    }

    /**
     * Handles right-click wire removal.
     */
    private void handleRightClickWireRemoval(MouseEvent event) {
        // Convert screen coordinates to world coordinates for proper wire detection
        Point2D worldClickPos = gameController.getGameView().screenToWorld(event.getX(), event.getY());
        java.lang.System.out.println("Right-click wire removal: screen (" + event.getX() + ", " + event.getY() + ") -> world (" + worldClickPos.getX() + ", " + worldClickPos.getY() + ")");

        // Find wire at world position
        WireConnection wireToRemove = findWireAtPosition(worldClickPos);

        if (wireToRemove != null) {
            // Remove the wire and restore its length
            boolean success = gameController.getWiringController().removeWireConnection(
                    wireToRemove, gameController.getGameState()
            );

            if (success) {
                java.lang.System.out.println("Wire removed successfully! Length restored: " + wireToRemove.getTotalLength());

                // Play wire removal sound if available
                if (gameController.getSoundManager() != null) {
                    gameController.getSoundManager().playWireConnectSound(); // Reuse wire connect sound
                }
            } else {
                java.lang.System.out.println("Failed to remove wire.");
            }
        } else {
            java.lang.System.out.println("No wire found at click position for removal.");
        }
    }

    /**
     * Handles mouse press in wire merge mode.
     */
    private void handleWireMergeMousePress(MouseEvent event) {
        // Convert screen coordinates to world coordinates for proper wire detection
        Point2D worldClickPos = gameController.getGameView().screenToWorld(event.getX(), event.getY());
        java.lang.System.out.println("Wire merge click: screen (" + event.getX() + ", " + event.getY() + ") -> world (" + worldClickPos.getX() + ", " + worldClickPos.getY() + ")");

        // Find wire at world position
        WireConnection wireAtPosition = findWireAtPosition(worldClickPos);

        if (wireAtPosition != null) {
            if (firstSelectedWireForMerge == null) {
                // Select first wire
                firstSelectedWireForMerge = wireAtPosition;
                java.lang.System.out.println("First wire selected for merging. Click another wire to merge them.");
            } else if (firstSelectedWireForMerge != wireAtPosition) {
                // Try to merge the two selected wires
                boolean success = gameController.getWiringController().mergeWireConnections(
                        firstSelectedWireForMerge, wireAtPosition, gameController.getGameState()
                );

                if (success) {
                    java.lang.System.out.println("Wires merged successfully!");

                    // Play wire connect sound if available
                    if (gameController.getSoundManager() != null) {
                        gameController.getSoundManager().playWireConnectSound();
                    }
                } else {
                    java.lang.System.out.println("Failed to merge wires. Check if they share a common connection point.");
                }

                // Reset selection
                firstSelectedWireForMerge = null;
            } else {
                // Clicked the same wire twice - deselect
                java.lang.System.out.println("Same wire clicked - selection cleared.");
                firstSelectedWireForMerge = null;
            }
        } else {
            java.lang.System.out.println("No wire found at click position for merging.");
        }
    }

    /**
     * Finds a port at the given screen position.
     */
    private Port findPortAtPosition(double x, double y) {
        if (gameController == null || gameController.getGameState() == null) {
            java.lang.System.out.println("  findPortAtPosition: gameController or gameState is null");
            return null;
        }

        // Convert screen coordinates to world coordinates using the viewport transformation
        Point2D worldPosition = gameController.getGameView().screenToWorld(x, y);
        java.lang.System.out.println("  findPortAtPosition: screen (" + x + ", " + y + ") -> world (" + worldPosition.getX() + ", " + worldPosition.getY() + ")");

        // Check all systems for ports at the given position
        for (System system : gameController.getGameState().getSystems()) {
            java.lang.System.out.println("    Checking system: " + system.getClass().getSimpleName() + " at " + system.getPosition());

            // Check input ports
            for (Port port : system.getInputPorts()) {
                if (isPositionNearPort(worldPosition, port)) {
                    java.lang.System.out.println("      Found input port: " + port.getShape() + " at " + port.getPosition());
                    return port;
                }
            }

            // Check output ports
            for (Port port : system.getOutputPorts()) {
                if (isPositionNearPort(worldPosition, port)) {
                    java.lang.System.out.println("      Found output port: " + port.getShape() + " at " + port.getPosition());
                    return port;
                }
            }
        }

        java.lang.System.out.println("  findPortAtPosition: no port found at world position (" + worldPosition.getX() + ", " + worldPosition.getY() + ")");
        return null;
    }

    private boolean isPositionNearPort(Point2D position, Port port) {
        if (port == null || port.getPosition() == null) {
            return false;
        }

        double distance = position.distanceTo(port.getPosition());
        return distance <= 15.0; // 15 pixel radius for port detection
    }

    /**
     * Creates a wire connection between two ports.
     */
    private void createWireConnection(Port port1, Port port2) {
        java.lang.System.out.println("InputHandler.createWireConnection called with ports:");
        java.lang.System.out.println("  Port1: " + port1.getShape() + " " + (port1.isInput() ? "input" : "output") + " at " + port1.getPosition());
        java.lang.System.out.println("  Port2: " + port2.getShape() + " " + (port2.isInput() ? "input" : "output") + " at " + port2.getPosition());

        if (gameController == null || port1 == null || port2 == null) {
            java.lang.System.out.println("  FAILED: gameController or ports are null");
            return;
        }

        // Validate connection, show reason on-screen if invalid
        if (!canCreateConnection(port1, port2)) {
            String reason = lastConnectionFailureReason;
            if (reason == null || reason.isEmpty()) reason = "Invalid connection";
            java.lang.System.out.println("Connection rejected: " + reason);
            if (gameController.getGameView() != null) {
                gameController.getGameView().showToast("Connection rejected: " + reason, javafx.scene.paint.Color.ORANGE);
            }
            return;
        }

        java.lang.System.out.println("  Connection validation passed, proceeding with creation...");

        // Calculate wire length needed
        Point2D pos1 = port1.getPosition();
        Point2D pos2 = port2.getPosition();
        double wireLength = pos1.distanceTo(pos2);

        // Check if enough wire length is available
        GameState gameState = gameController.getGameState();
        double gameStateRemaining = gameState.getRemainingWireLength();
        double levelRemaining = gameState.getCurrentLevel() != null ? gameState.getCurrentLevel().getRemainingWireLength() : 0.0;

        java.lang.System.out.println("DEBUG: Wire length check - Need: " + String.format("%.1f", wireLength) +
                ", GameState has: " + String.format("%.1f", gameStateRemaining) +
                ", Level calc: " + String.format("%.1f", levelRemaining));

        if (wireLength > gameStateRemaining) {
            String msg = "Not enough wire length (need " + String.format("%.1f", wireLength) + ", have " + String.format("%.1f", gameStateRemaining) + ")";
            java.lang.System.out.println("Connection rejected: " + msg);
            if (gameController.getGameView() != null) {
                gameController.getGameView().showToast("Connection rejected: " + msg, javafx.scene.paint.Color.ORANGE);
            }
            return;
        }

        // Create wire connection with proper length
        WireConnection connection = new WireConnection(port1, port2, wireLength);
        java.lang.System.out.println("  WireConnection object created with ID: " + connection.getId());
        java.lang.System.out.println("  Path points: " + connection.getPathPoints().size() + " points");
        for (int i = 0; i < connection.getPathPoints().size(); i++) {
            Point2D point = connection.getPathPoints().get(i);
            java.lang.System.out.println("    Point " + i + ": (" + point.getX() + ", " + point.getY() + ")");
        }

        // Add to game state
        gameState.addWireConnection(connection);
        java.lang.System.out.println("  Wire connection added to game state. Total connections: " + gameState.getWireConnections().size());

        // Update port connection status
        port1.setConnected(true);
        port2.setConnected(true);

        // Immediately update system indicators for both connected systems
        port1.getParentSystem().update(0.0); // Force indicator update
        port2.getParentSystem().update(0.0); // Force indicator update

        // Deduct wire length from available budget
        gameState.setRemainingWireLength(gameState.getRemainingWireLength() - wireLength);

        java.lang.System.out.println("Wire connection created: length=" + String.format("%.1f", wireLength) +
                ", remaining=" + String.format("%.1f", gameState.getRemainingWireLength()));

        // Play connection sound
        if (gameController.getSoundManager() != null) {
            gameController.getSoundManager().playWireConnectSound();
        }
    }

    private boolean canCreateConnection(Port port1, Port port2) {
        java.lang.System.out.println("  Validating connection in InputHandler...");
        lastConnectionFailureReason = null;

        // Check if ports are from the same system
        if (port1.getParentSystem() == port2.getParentSystem()) {
            lastConnectionFailureReason = "Ports from the same system";
            java.lang.System.out.println("    FAILED: Ports from the same system");
            return false;
        }

        // Port shape compatibility check removed - all shapes can now connect

        // Check if one is input and one is output (fix output-to-output connections)
        if (port1.isInput() == port2.isInput()) {
            String portTypes = port1.isInput() ? "both input" : "both output";
            lastConnectionFailureReason = portTypes + " ports (must be input-to-output)";
            java.lang.System.out.println("    FAILED: " + portTypes + " ports (must be input-to-output)");
            return false;
        }

        // Check if connection already exists
        if (gameController.getGameState().hasWireConnection(port1, port2)) {
            lastConnectionFailureReason = "Connection already exists";
            java.lang.System.out.println("    FAILED: Connection already exists");
            return false;
        }

        // Check if EITHER port is already connected (fix multiple connections per port)
        if (port1.isConnected() || port2.isConnected()) {
            String connectedPort = port1.isConnected() ? "first" : "second";
            lastConnectionFailureReason = connectedPort + " port already connected";
            java.lang.System.out.println("    FAILED: " + connectedPort + " port already connected");
            return false;
        }

        java.lang.System.out.println("    Connection validation passed in InputHandler");
        return true;
    }

    // Holds last human-readable reason set by canCreateConnection
    private String lastConnectionFailureReason = null;

    /**
     * Shows wire preview during dragging.
     */
    private void showWirePreview(MouseEvent event) {
        if (selectedPort != null && gameController != null && gameController.getGameView() != null) {
            Point2D start = selectedPort.getPosition();

            // Convert screen coordinates to world coordinates for proper positioning
            Point2D end = gameController.getGameView().screenToWorld(event.getX(), event.getY());

            Port targetPort = findPortAtPosition(event.getX(), event.getY());
            boolean isValid = targetPort != null && canCreateConnection(selectedPort, targetPort);

            gameController.getGameView().showWirePreview(start, end, isValid);
        }
    }

    /**
     * Clears wire preview.
     */
    private void clearWirePreview() {
        if (gameController != null && gameController.getGameView() != null) {
            gameController.getGameView().clearWirePreview();
        }
    }

    /**
     * Gets whether wiring mode is active.
     */
    public boolean isWiringMode() {
        return isWiringMode;
    }

    /**
     * Gets whether bend creation mode is active.
     */
    public boolean isBendCreationMode() {
        return isBendCreationMode;
    }

    /**
     * Gets whether wire merge mode is active.
     */
    public boolean isWireMergeMode() {
        return isWireMergeMode;
    }

    /**
     * Starts ability targeting mode for Phase 2 abilities.
     */
    public void startAbilityTargeting(AbilityType abilityType) {
        this.isAbilityTargetingMode = true;
        this.pendingAbility = abilityType;

        // Exit other modes
        this.isWiringMode = false;
        this.isBendCreationMode = false;
        this.isWireMergeMode = false;
        this.isSystemMovementMode = false;
        this.selectedPort = null;
        this.selectedWire = null;

        String message = getAbilityTargetingMessage(abilityType);
        java.lang.System.out.println(message);
    }

    /**
     * Gets the targeting message for an ability.
     */
    private String getAbilityTargetingMessage(AbilityType abilityType) {
        switch (abilityType) {
            case SCROLL_OF_AERGIA:
                return "Scroll of Aergia activated - Click on a wire to set acceleration to zero";
            case SCROLL_OF_SISYPHUS:
                return "Scroll of Sisyphus activated - Click on a system to enable movement";
            case SCROLL_OF_ELIPHAS:
                return "Scroll of Eliphas activated - Click on a wire to realign packet centers";
            default:
                return "Ability activated - Click to target";
        }
    }

    /**
     * Handles ability targeting click.
     */
    private void handleAbilityTargeting(MouseEvent event) {
        if (!isAbilityTargetingMode || pendingAbility == null) {
            return;
        }

        Point2D clickPoint = new Point2D(event.getX(), event.getY());
        boolean success = gameController.activateAbilityAtPoint(pendingAbility, clickPoint);

        if (success) {
            java.lang.System.out.println("Ability " + pendingAbility.getDisplayName() + " activated successfully");
        } else {
            java.lang.System.out.println("Could not activate " + pendingAbility.getDisplayName() + " at that location");
        }

        // Exit targeting mode
        isAbilityTargetingMode = false;
        pendingAbility = null;
    }

    /**
     * Gets whether ability targeting mode is active.
     */
    public boolean isAbilityTargetingMode() {
        return isAbilityTargetingMode;
    }

    /**
     * Gets the pending ability for targeting.
     */
    public AbilityType getPendingAbility() {
        return pendingAbility;
    }

    /**
     * Gets the currently selected port.
     */
    public Port getSelectedPort() {
        return selectedPort;
    }

    /**
     * Gets the currently selected wire.
     */
    public WireConnection getSelectedWire() {
        return selectedWire;
    }

    /**
     * Gets the first selected wire for merging.
     */
    public WireConnection getFirstSelectedWireForMerge() {
        return firstSelectedWireForMerge;
    }

    /**
     * Gets the currently selected bend index.
     */
    public int getSelectedBendIndex() {
        return selectedBendIndex;
    }

    /**
     * Finds a wire at the given screen position.
     */
    private WireConnection findWireAtPosition(Point2D position) {
        if (gameController == null || gameController.getGameState() == null) {
            java.lang.System.out.println("DEBUG: findWireAtPosition - gameController or gameState is null");
            return null;
        }

        java.lang.System.out.println("DEBUG: findWireAtPosition - searching for wire at position: " + position);

        // Check all wire connections
        for (WireConnection connection : gameController.getGameState().getWireConnections()) {
            if (!connection.isActive()) {
                java.lang.System.out.println("DEBUG: Skipping inactive connection: " + connection.getId());
                continue;
            }

            java.lang.System.out.println("DEBUG: Checking active connection: " + connection.getId() + " with " + connection.getPathPoints().size() + " path points");

            // Check if position is near the wire path
            if (isPositionNearWire(position, connection)) {
                java.lang.System.out.println("DEBUG: Found wire at position: " + connection.getId());
                return connection;
            }
        }

        java.lang.System.out.println("DEBUG: No wire found at position: " + position);
        return null;
    }

    /**
     * Checks if a position is near a wire path.
     */
    private boolean isPositionNearWire(Point2D position, WireConnection connection) {
        // Use the same smooth curve path that's used for rendering to ensure click detection matches visual appearance
        boolean useSmoothCurves = true; // Default to smooth curves for consistency with rendering
        if (gameController != null && gameController.getGameState() != null) {
            Object setting = gameController.getGameState().getGameSettings().get("smoothWireCurves");
            if (setting != null) {
                useSmoothCurves = (Boolean) setting;
            }
        }

        List<Point2D> pathPoints = connection.getPathPoints(useSmoothCurves);
        java.lang.System.out.println("DEBUG: isPositionNearWire - checking " + pathPoints.size() + " path points (smooth: " + useSmoothCurves + ")");

        // Check each line segment of the wire
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point2D start = pathPoints.get(i);
            Point2D end = pathPoints.get(i + 1);
            java.lang.System.out.println("DEBUG: Checking segment " + i + ": (" + start.getX() + ", " + start.getY() + ") to (" + end.getX() + ", " + end.getY() + ")");

            if (isPositionNearLineSegment(position, start, end)) {
                java.lang.System.out.println("DEBUG: Position is near segment " + i);
                return true;
            }
        }

        java.lang.System.out.println("DEBUG: Position is not near any wire segment");
        return false;
    }

    /**
     * Checks if a position is near a line segment.
     */
    private boolean isPositionNearLineSegment(Point2D position, Point2D lineStart, Point2D lineEnd) {
        double distance = distanceToLineSegment(position, lineStart, lineEnd);
        boolean isNear = distance <= 25.0; // Increased from 20.0 to 25.0 pixel radius for better smooth curve detection
        java.lang.System.out.println("DEBUG: isPositionNearLineSegment - position: (" + position.getX() + ", " + position.getY() +
                "), lineStart: (" + lineStart.getX() + ", " + lineStart.getY() +
                "), lineEnd: (" + lineEnd.getX() + ", " + lineEnd.getY() +
                "), distance: " + distance + ", isNear: " + isNear);
        return isNear;
    }

    /**
     * Calculates the distance from a point to a line segment.
     */
    private double distanceToLineSegment(Point2D point, Point2D lineStart, Point2D lineEnd) {
        double A = point.getX() - lineStart.getX();
        double B = point.getY() - lineStart.getY();
        double C = lineEnd.getX() - lineStart.getX();
        double D = lineEnd.getY() - lineStart.getY();

        double dot = A * C + B * D;
        double lenSq = C * C + D * D;

        if (lenSq == 0) {
            // Line segment is actually a point
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
     * Handles mouse press events for system movement.
     */
    private void handleSystemMovementMousePress(MouseEvent event) {
        System system = findSystemAtPosition(event.getX(), event.getY());
        if (system != null && !(system instanceof model.ReferenceSystem)) {
            selectedSystem = system;
            java.lang.System.out.println("Selected system for movement - drag to move");
        } else {
            java.lang.System.out.println("No movable system found at position (reference systems cannot be moved)");
        }
    }

    /**
     * Finds a system at the given screen position.
     */
    private System findSystemAtPosition(double x, double y) {
        if (gameController == null || gameController.getGameState() == null) {
            return null;
        }

        Point2D clickPos = new Point2D(x, y);
        for (System system : gameController.getGameState().getSystems()) {
            Point2D systemPos = system.getPosition();
            if (systemPos != null && clickPos.distanceTo(systemPos) <= 25) { // 25 pixel radius
                return system;
            }
        }
        return null;
    }

    /**
     * Gets whether system movement mode is active.
     */
    public boolean isSystemMovementMode() {
        return isSystemMovementMode;
    }

    /**
     * Gets the currently selected system.
     */
    public System getSelectedSystem() {
        return selectedSystem;
    }

    /**
     * Gets whether the space key is currently pressed.
     */
    public boolean isSpacePressed() {
        return isSpacePressed;
    }
}
