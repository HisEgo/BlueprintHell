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

    public KeyCode getKeyBinding(String action) {
        return keyBindings.get(action);
    }

    public void resetKeyBindings() {
        this.keyBindings = new HashMap<>(DEFAULT_BINDINGS);
        this.reverseKeyBindings.clear();
        for (Map.Entry<String, KeyCode> entry : keyBindings.entrySet()) {
            reverseKeyBindings.put(entry.getValue(), entry.getKey());
        }
    }

    public Map<String, KeyCode> getAllKeyBindings() {
        return new HashMap<>(keyBindings);
    }

    public void handleKeyPress(KeyEvent event) {
        KeyCode code = event.getCode();
        

        // Handle space key for viewport panning
        if (code == KeyCode.SPACE) {
            isSpacePressed = true;
            
            return; // Don't process as a regular key binding
        }

        String action = reverseKeyBindings.get(code);
        

        if (action == null) {
            
            return; // No binding for this key
        }

        switch (action) {
            case "temporal_backward":
                // Temporal navigation disabled - use time slider instead
                // handleTemporalNavigation(-1);
                break;
            case "temporal_forward":
                // Temporal navigation disabled - use time slider instead
                // handleTemporalNavigation(1);
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
                    
                }
                break;
            case "toggle_indicators":
                // System indicators are always ON - no toggle needed
                
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
                // Simulation is now started via Run button in UI
                
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
                    
                } else if (isSystemMovementMode) {
                    isSystemMovementMode = false;
                    selectedSystem = null;
                    
                } else if (gameController.isSimulationMode()) {
                    // Return to editing mode from simulation
                    gameController.enterEditingMode();
                }
                break;
            case "toggle_smooth_wires":
                // Toggle smooth wire curves - only allowed in editing mode
                if (gameController.isEditingMode()) {
                    gameController.toggleSmoothWires();
                    
                } else {
                    
                }
                break;
        }
    }

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

    public void handleMousePress(MouseEvent event) {
        if (gameController == null) return;

        

        if (event.getButton() == MouseButton.PRIMARY) {
            if (gameController.isEditingMode()) {
                if (isWiringMode) {
                    handleWiringMousePress(event);
                } else if (isBendCreationMode) {
                    
                    handleBendCreationMousePress(event);
                } else if (isWireMergeMode) {
                    handleWireMergeMousePress(event);
                } else if (isSystemMovementMode) {
                    handleSystemMovementMousePress(event);
                } else {
                    // Check if clicking on a port to start wiring
                    Port port = findPortAtPosition(event.getX(), event.getY());
                    if (port != null) {
                        
                        // Start wiring from this port
                        selectedPort = port;
                        isWiringMode = true;
                        
                        showWirePreview(event);
                    } else {
                        
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
                    
                }
            } else if (isSystemMovementMode && selectedSystem != null) {
                // Move system
                Point2D newPosition = new Point2D(event.getX(), event.getY());
                boolean success = gameController.getWiringController().moveSystem(
                        selectedSystem, newPosition, gameController.getGameState()
                );

                if (!success) {
                    
                }
            }
        }
    }

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
                
            } else if (isBendCreationMode) {
                // Clear bend selection
                selectedWire = null;
                selectedBendIndex = -1;
            } else if (isSystemMovementMode) {
                // Clear system selection
                selectedSystem = null;
                
            } else if (isWireMergeMode) {
                // Wire merge mode is handled in press, nothing needed on release
            }
        }
    }

    private void handleTemporalNavigation(int direction) {
        if (gameController != null && gameController.isSimulationMode()) {
            double currentProgress = gameController.getGameState().getTemporalProgress();
            double levelDuration = gameController.getGameState().getCurrentLevel().getLevelDuration();
            double timeStep = 0.1; // Even smaller steps for more precise navigation
            double newProgress;

            if (direction > 0) {
                // Move forward in time
                newProgress = Math.min(currentProgress + timeStep, levelDuration);
                
            } else {
                // Move backward in time
                newProgress = Math.max(0.0, currentProgress - timeStep);
                
            }

            // Only update if time actually changed
            if (Math.abs(newProgress - currentProgress) > 0.01) {
                // Update packet positions and temporal progress
                gameController.updatePacketPositionsForTime(newProgress);
                
            }
        } else if (gameController != null && gameController.isEditingMode()) {
            // In editing mode, temporal navigation is not available
            //System.out.println("Temporal navigation only available in simulation mode");
        }
    }

    private void toggleShop() {
        if (gameController != null) {
            gameController.toggleShop();
        }
    }

    private void startSimulation() {
        if (gameController != null) {
            gameController.enterSimulationMode();
        }
    }

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

    private void handleBendCreationMousePress(MouseEvent event) {
        // Convert screen coordinates to world coordinates for proper wire detection
        Point2D worldClickPos = gameController.getGameView().screenToWorld(event.getX(), event.getY());
        

        // First, check if clicking on an existing bend for movement
        WireConnection wireWithBend = findWireWithBendAtPosition(worldClickPos);
        if (wireWithBend != null) {
            selectedWire = wireWithBend;
            selectedBendIndex = findBendIndexAtPosition(wireWithBend, worldClickPos);
            
            return;
        }

        // If not clicking on a bend, try to add a new bend
        WireConnection wireAtPosition = findWireAtPosition(worldClickPos);

        if (wireAtPosition != null) {
            
            // Try to add a bend to this wire
            // Get smooth curve setting to calculate wire length correctly
            boolean useSmoothCurves = true; // Default to smooth curves
            if (gameController.getGameState() != null) {
                Object setting = gameController.getGameState().getGameSettings().get("smoothWireCurves");
                if (setting instanceof Boolean) {
                    useSmoothCurves = (Boolean) setting;
                }
            }
            
            boolean success = gameController.getWiringController().addBendToWire(
                    wireAtPosition, worldClickPos, gameController.getGameState(), useSmoothCurves
            );

            if (success) {
                // Play success sound if available
                if (gameController.getSoundManager() != null) {
                    gameController.getSoundManager().playWireConnectSound();
                }
                
            } else {
                
            }
        } else {
            
        }
    }

    private WireConnection findWireWithBendAtPosition(Point2D position) {
        if (gameController == null || gameController.getGameState() == null) {
            return null;
        }

        WireConnection closestWire = null;
        double closestDistance = Double.MAX_VALUE;
        double maxBendRadius = 15.0; // Bend selection radius

        // Check all wire connections and find the closest bend
        for (WireConnection connection : gameController.getGameState().getWireConnections()) {
            if (!connection.isActive()) continue;

            for (WireBend bend : connection.getBends()) {
                double distance = position.distanceTo(bend.getPosition());
                if (distance <= maxBendRadius && distance < closestDistance) {
                    closestWire = connection;
                    closestDistance = distance;
                }
            }
        }

        return closestWire;
    }

    private int findBendIndexAtPosition(WireConnection connection, Point2D position) {
        int closestBendIndex = -1;
        double closestDistance = Double.MAX_VALUE;
        double maxBendRadius = 15.0; // Bend selection radius

        for (int i = 0; i < connection.getBends().size(); i++) {
            WireBend bend = connection.getBends().get(i);
            double distance = position.distanceTo(bend.getPosition());
            if (distance <= maxBendRadius && distance < closestDistance) {
                closestBendIndex = i;
                closestDistance = distance;
            }
        }
        return closestBendIndex;
    }

    private void handleWiringMouseRelease(MouseEvent event) {
        // Clear wire preview
        clearWirePreview();
    }

    private void handleRegularMousePress(MouseEvent event) {
        // Handle system selection, menu clicks, etc.
        
    }

    private void handleRightClickWireRemoval(MouseEvent event) {
        // Convert screen coordinates to world coordinates for proper wire detection
        Point2D worldClickPos = gameController.getGameView().screenToWorld(event.getX(), event.getY());
        

        // Find wire at world position
        WireConnection wireToRemove = findWireAtPosition(worldClickPos);

        if (wireToRemove != null) {
            // Remove the wire and restore its length
            boolean success = gameController.getWiringController().removeWireConnection(
                    wireToRemove, gameController.getGameState()
            );

            if (success) {
                

                // Play wire removal sound if available
                if (gameController.getSoundManager() != null) {
                    gameController.getSoundManager().playWireConnectSound(); // Reuse wire connect sound
                }
            } else {
                
            }
        } else {
            
        }
    }

    private void handleWireMergeMousePress(MouseEvent event) {
        // Convert screen coordinates to world coordinates for proper wire detection
        Point2D worldClickPos = gameController.getGameView().screenToWorld(event.getX(), event.getY());
        

        // Find wire at world position
        WireConnection wireAtPosition = findWireAtPosition(worldClickPos);

        if (wireAtPosition != null) {
            if (firstSelectedWireForMerge == null) {
                // Select first wire
                firstSelectedWireForMerge = wireAtPosition;
                
            } else if (firstSelectedWireForMerge != wireAtPosition) {
                // Try to merge the two selected wires
                boolean success = gameController.getWiringController().mergeWireConnections(
                        firstSelectedWireForMerge, wireAtPosition, gameController.getGameState()
                );

                if (success) {
                    
                    // Play wire connect sound if available
                    if (gameController.getSoundManager() != null) {
                        gameController.getSoundManager().playWireConnectSound();
                    }
                } else {
                    
                }

                // Reset selection
                firstSelectedWireForMerge = null;
            } else {
                // Clicked the same wire twice - deselect
                
                firstSelectedWireForMerge = null;
            }
        } else {
            
        }
    }

    private Port findPortAtPosition(double x, double y) {
        if (gameController == null || gameController.getGameState() == null) {
            
            return null;
        }

        // Convert screen coordinates to world coordinates using the viewport transformation
        Point2D worldPosition = gameController.getGameView().screenToWorld(x, y);
        

        // Check all systems for ports at the given position
        for (System system : gameController.getGameState().getSystems()) {
            

            // Check input ports
            for (Port port : system.getInputPorts()) {
                if (isPositionNearPort(worldPosition, port)) {
                    
                    return port;
                }
            }

            // Check output ports
            for (Port port : system.getOutputPorts()) {
                if (isPositionNearPort(worldPosition, port)) {
                    
                    return port;
                }
            }
        }

        
        return null;
    }

    private boolean isPositionNearPort(Point2D position, Port port) {
        if (port == null || port.getPosition() == null) {
            return false;
        }

        double distance = position.distanceTo(port.getPosition());
        return distance <= 15.0; // 15 pixel radius for port detection
    }

    private void createWireConnection(Port port1, Port port2) {
        

        if (gameController == null || port1 == null || port2 == null) {
            
            return;
        }

        // Validate connection, show reason on-screen if invalid
        if (!canCreateConnection(port1, port2)) {
            String reason = lastConnectionFailureReason;
            if (reason == null || reason.isEmpty()) reason = "Invalid connection";
            
            if (gameController.getGameView() != null) {
                gameController.getGameView().showToast("Connection rejected: " + reason, javafx.scene.paint.Color.ORANGE);
            }
            return;
        }

        

        // Calculate wire length needed
        Point2D pos1 = port1.getPosition();
        Point2D pos2 = port2.getPosition();
        double wireLength = pos1.distanceTo(pos2);

        // Check if enough wire length is available
        GameState gameState = gameController.getGameState();
        double gameStateRemaining = gameState.getRemainingWireLength();
        double levelRemaining = gameState.getCurrentLevel() != null ? gameState.getCurrentLevel().getRemainingWireLength() : 0.0;

        

        if (wireLength > gameStateRemaining) {
            String msg = "Not enough wire length (need " + String.format("%.1f", wireLength) + ", have " + String.format("%.1f", gameStateRemaining) + ")";
            
            if (gameController.getGameView() != null) {
                gameController.getGameView().showToast("Connection rejected: " + msg, javafx.scene.paint.Color.ORANGE);
            }
            return;
        }

        // Create wire connection with proper length
        WireConnection connection = new WireConnection(port1, port2, wireLength);
        

        // Add to game state
        gameState.addWireConnection(connection);
        

        // Update port connection status
        port1.setConnected(true);
        port2.setConnected(true);

        // Immediately update system indicators for both connected systems
        port1.getParentSystem().update(0.0); // Force indicator update
        port2.getParentSystem().update(0.0); // Force indicator update

        // Deduct wire length from available budget
        gameState.setRemainingWireLength(gameState.getRemainingWireLength() - wireLength);

        

        // Play connection sound
        if (gameController.getSoundManager() != null) {
            gameController.getSoundManager().playWireConnectSound();
        }
    }

    private boolean canCreateConnection(Port port1, Port port2) {
        
        lastConnectionFailureReason = null;

        // Check if ports are from the same system
        if (port1.getParentSystem() == port2.getParentSystem()) {
            lastConnectionFailureReason = "Ports from the same system";
            
            return false;
        }

        // Port shape compatibility check removed - all shapes can now connect

        // Check if one is input and one is output (fix output-to-output connections)
        if (port1.isInput() == port2.isInput()) {
            String portTypes = port1.isInput() ? "both input" : "both output";
            lastConnectionFailureReason = portTypes + " ports (must be input-to-output)";
            
            return false;
        }

        // Check if connection already exists
        if (gameController.getGameState().hasWireConnection(port1, port2)) {
            lastConnectionFailureReason = "Connection already exists";
            
            return false;
        }

        // Check if EITHER port is already connected (fix multiple connections per port)
        if (port1.isConnected() || port2.isConnected()) {
            String connectedPort = port1.isConnected() ? "first" : "second";
            lastConnectionFailureReason = connectedPort + " port already connected";
            
            return false;
        }

        
        return true;
    }

    // Holds last human-readable reason set by canCreateConnection
    private String lastConnectionFailureReason = null;

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

    private void clearWirePreview() {
        if (gameController != null && gameController.getGameView() != null) {
            gameController.getGameView().clearWirePreview();
        }
    }

    public boolean isWiringMode() {
        return isWiringMode;
    }

    public boolean isBendCreationMode() {
        return isBendCreationMode;
    }

    public boolean isWireMergeMode() {
        return isWireMergeMode;
    }

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

    public boolean isAbilityTargetingMode() {
        return isAbilityTargetingMode;
    }

    public AbilityType getPendingAbility() {
        return pendingAbility;
    }

    public Port getSelectedPort() {
        return selectedPort;
    }

    public WireConnection getSelectedWire() {
        return selectedWire;
    }

    public WireConnection getFirstSelectedWireForMerge() {
        return firstSelectedWireForMerge;
    }

    public int getSelectedBendIndex() {
        return selectedBendIndex;
    }

    private WireConnection findWireAtPosition(Point2D position) {
        if (gameController == null || gameController.getGameState() == null) {
            return null;
        }

        WireConnection closestWire = null;
        double closestDistance = Double.MAX_VALUE;
        double maxSelectionRadius = 20.0; // Reduced from 25.0 for more precise selection

        // Check all wire connections and find the closest one
        for (WireConnection connection : gameController.getGameState().getWireConnections()) {
            if (!connection.isActive()) {
                continue;
            }

            // Calculate distance to this wire
            double distance = getDistanceToWire(position, connection);
            
            // Only consider wires within selection radius
            if (distance <= maxSelectionRadius && distance < closestDistance) {
                closestWire = connection;
                closestDistance = distance;
            }
        }

        return closestWire;
    }

    private double getDistanceToWire(Point2D position, WireConnection connection) {
        // Use the same smooth curve path that's used for rendering
        boolean useSmoothCurves = true;
        if (gameController != null && gameController.getGameState() != null) {
            Object setting = gameController.getGameState().getGameSettings().get("smoothWireCurves");
            if (setting != null) {
                useSmoothCurves = (Boolean) setting;
            }
        }

        List<Point2D> pathPoints = connection.getPathPoints(useSmoothCurves);
        double minDistance = Double.MAX_VALUE;

        // Check each line segment of the wire
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point2D start = pathPoints.get(i);
            Point2D end = pathPoints.get(i + 1);
            
            double segmentDistance = distanceToLineSegment(position, start, end);
            if (segmentDistance < minDistance) {
                minDistance = segmentDistance;
            }
        }

        return minDistance;
    }

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

    private boolean isPositionNearLineSegment(Point2D position, Point2D lineStart, Point2D lineEnd) {
        double distance = distanceToLineSegment(position, lineStart, lineEnd);
        boolean isNear = distance <= 20.0; // Reduced to 20.0 for more precise selection
        return isNear;
    }

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

    private void handleSystemMovementMousePress(MouseEvent event) {
        System system = findSystemAtPosition(event.getX(), event.getY());
        if (system != null && !(system instanceof model.ReferenceSystem)) {
            selectedSystem = system;
            java.lang.System.out.println("Selected system for movement - drag to move");
        } else {
            java.lang.System.out.println("No movable system found at position (reference systems cannot be moved)");
        }
    }

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

    public boolean isSystemMovementMode() {
        return isSystemMovementMode;
    }

    public System getSelectedSystem() {
        return selectedSystem;
    }

    public boolean isSpacePressed() {
        return isSpacePressed;
    }
}
