package controller;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import model.GameState;
import model.GameLevel;
import model.Packet;
import model.AbilityType;
import model.ReferenceSystem;
import model.MessengerPacket;
import model.ProtectedPacket;
import model.WireConnection;
import model.Point2D;
import model.NormalSystem;
import model.SystemType;
import model.Port;
import model.PortShape;
import model.PacketInjection;
import model.PacketType;
import model.SpySystem;
import model.SaboteurSystem;
import model.VPNSystem;
import model.AntiTrojanSystem;
import model.DistributorSystem;
import model.MergerSystem;
import view.GameView;
import view.HUDView;
import view.LevelSelectView;
import view.SettingsView;
import view.GameOverView;
import view.ShopView;
import view.LevelCompleteView;
import view.PauseView;
import app.MainApp;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;

/**
 * Main game controller that manages the core game loop and logic.
 * Uses AnimationTimer for smooth updates and manages all game systems.
 */
public class GameController {
    private GameState gameState;
    private GameView gameView;
    private HUDView hudView;
    private LevelSelectView levelSelectView;
    private SettingsView settingsView;
    private GameOverView gameOverView;
    private ShopView shopView;
    private LevelCompleteView levelCompleteView;
    private PauseView pauseView;

    private InputHandler inputHandler;
    private MovementController movementController;
    private CollisionController collisionController;
    private WiringController wiringController;
    private GameFlowController gameFlowController;
    private AbilityManager abilityManager;
    private SoundManager soundManager;

    private AnimationTimer gameLoop;
    private AnimationTimer editingRenderLoop;
    private boolean isRunning;
    private boolean isEditingRenderLoopRunning;
    private long lastUpdateTime;

    // Phase 2 additions
    private GameSaveManager saveManager;
    private List<AbilityType> activeAbilities;
    private Map<AbilityType, Double> abilityCooldowns;
    private double currentTime;
    private MainApp mainApp;

    // Game loading mode - determines whether connections are preserved between levels


    // Temporal navigation and mode management
    private boolean isEditingMode;
    private boolean isSimulationMode;

    public GameController(GameState gameState) {
        this.gameState = gameState;
        this.isRunning = false;
        this.isEditingRenderLoopRunning = false;
        this.lastUpdateTime = 0;
        this.currentTime = 0.0;

        // Initialize Phase 2 components
        this.saveManager = new GameSaveManager();
        this.activeAbilities = new ArrayList<>();
        this.abilityCooldowns = new HashMap<>();

        // Initialize temporal navigation modes
        this.isEditingMode = true;  // Start in editing mode
        this.isSimulationMode = false;

        // Initialize game loading mode (default to fresh mode)


        initializeControllers();
        initializeViews();
        initializeGameLoop();
    }

    /**
     * Initializes all the sub-controllers.
     */
    private void initializeControllers() {
        inputHandler = new InputHandler(this);
        movementController = new MovementController();
        collisionController = new CollisionController(this);
        wiringController = new WiringController();
        gameFlowController = new GameFlowController(this);
        abilityManager = new AbilityManager(this, movementController);
        soundManager = new SoundManager();
    }

    /**
     * Initializes all the views.
     */
    private void initializeViews() {
        gameView = new GameView(this);
        hudView = new HUDView(this);
        levelSelectView = new LevelSelectView(this);
        settingsView = new SettingsView(this);
        gameOverView = new GameOverView(this);
        shopView = new ShopView(this);
        levelCompleteView = new LevelCompleteView(this);
        pauseView = new PauseView(this);

        // Add overlays to the game view scene graph
        gameView.addShopOverlay(shopView.getRoot());
        gameView.addHUDOverlay(hudView.getRoot());
        gameView.addPauseOverlay(pauseView.getRoot());

        // HUD indicator removed - HUD is always visible

        // Ensure HUD is visible by default
        hudView.forceShowAndUpdate();
        System.out.println("HUD initialized and forced to be visible");
    }

    /**
     * Sets the callback for returning to main menu from level select.
     */
    public void setMainMenuCallback(Runnable callback) {
        if (levelSelectView != null) {
            levelSelectView.setOnBackToMainMenu(callback);
        }
        if (settingsView != null) {
            settingsView.setOnBackToMainMenu(callback);
        }
        if (gameOverView != null) {
            gameOverView.setOnBackToMainMenu(callback);
        }
        if (levelCompleteView != null) {
            levelCompleteView.setOnBackToMainMenu(callback);
        }
    }

    /**
     * Ensures all navigation callbacks are properly set up.
     * This method should be called after all views are initialized.
     */
    public void setupNavigationCallbacks(Runnable mainMenuCallback, Runnable restartLevelCallback) {
        setMainMenuCallback(mainMenuCallback);
        setRestartLevelCallback(restartLevelCallback);
    }

    /**
     * Sets the callback for restarting the current level.
     */
    public void setRestartLevelCallback(Runnable callback) {
        if (gameOverView != null) {
            // Wrap provided callback to auto-route to level 1 on forced-restart conditions
            gameOverView.setOnRestartLevel(() -> {
                try {
                    model.GameLevel currentLevel = gameState.getCurrentLevel();
                    model.GameOverReason reason = gameState.getLastGameOverReason();
                    boolean isNotFirstLevel = currentLevel != null && currentLevel.getLevelId() != null &&
                            !"level1".equals(currentLevel.getLevelId());
                    // Treat a disconnected network in later levels as a forced restart to the beginning
                    if (reason == model.GameOverReason.NETWORK_DISCONNECTED && isNotFirstLevel && mainApp != null) {
                        mainApp.startGame("level1");
                    } else {
                        // Default behavior: restart the same level
                        if (callback != null) callback.run();
                    }
                } catch (Exception e) {
                    // Fallback: restart same level if anything goes wrong
                    if (callback != null) callback.run();
                }
            });
        }
        if (levelCompleteView != null) {
            levelCompleteView.setOnNextLevel(() -> {
                gameFlowController.nextLevel();
            });
        }
    }

    /**
     * Initializes the main game loop using AnimationTimer.
     */
    private void initializeGameLoop() {
        // Main simulation game loop (only runs during simulation mode)
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!isRunning || gameState.isPaused()) {
                    return;
                }

                double deltaTime = (now - lastUpdateTime) / 1_000_000_000.0; // Convert to seconds
                lastUpdateTime = now;

                // Update game logic
                update(deltaTime);

                // Update views
                Platform.runLater(() -> {
                    gameView.update();
                    hudView.update();
                });
            }
        };

        // Editing mode render loop (only for visual updates, no time progression)
        editingRenderLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                Platform.runLater(() -> {
                    gameView.update();
                    hudView.update();
                    
                    // Auto-start simulation when all indicators are on
                    if (isEditingMode) {
                        boolean allIndicatorsOn = areAllIndicatorsOn();
                        boolean refSystemsReady = areReferenceSystemsReady();
                        if (allIndicatorsOn && refSystemsReady) {
                            System.out.println("All system indicators are ON - Auto-starting simulation!");
                            enterSimulationMode();
                        }
                    }
                });
            }
        };
    }

    /**
     * Main update method called by the game loop.
     */
    private void update(double deltaTime) {
        // Only update simulation logic during simulation mode
        if (isSimulationMode) {
            // Update temporal progress (only during simulation)
            gameState.updateTemporalProgress(deltaTime);
            // Update level timer (only during simulation)
            gameState.updateLevelTimer(deltaTime);

            // Update current time for Phase 2
            currentTime += deltaTime;

            // Process packet injections from schedule
            processPacketInjections();

            // Debug: Print active packet count every few seconds
            if ((int)(gameState.getLevelTimer()) % 3 == 0 && gameState.getLevelTimer() > 0) {
                java.lang.System.out.println("Active packets: " + gameState.getActivePackets().size() + " at time " + gameState.getLevelTimer());
            }

            // Phase 2: Update system deactivation timers early
            updateSystemDeactivationTimers(deltaTime);

            // First, advance packets along wires
            updateWirePacketMovement(deltaTime);

            // Update packet movement with MovementController (for enhanced path-based movement)
            // Check smooth curve setting and pass it to MovementController
            boolean useSmoothCurves = true; // Default to smooth curves
            Object setting = gameState.getGameSettings().get("smoothWireCurves");
            if (setting instanceof Boolean) {
                useSmoothCurves = (Boolean) setting;
            }
            movementController.updatePackets(gameState.getActivePackets(), deltaTime, useSmoothCurves);

            // Apply ability effects to packet movement
            for (Packet packet : gameState.getActivePackets()) {
                movementController.applyAbilityEffects(packet, activeAbilities);
            }

            // First pass: transfer from wires to input ports (deliveries this frame)
            processWireConnections();

            // Immediately process inputs so arrivals are forwarded to outputs in the same frame
            updateSystems(deltaTime);

            // Anti-Trojan scan after system updates
            runAntiTrojanScans();

            // Second pass: move any packets placed on output ports to wires immediately
            processWireConnections();

            // Process system storage to outputs when ports become available (and push to wires)
            processSystemTransfers();

            // Check for collisions (only for packets on wires)
            collisionController.checkCollisions(getPacketsOnWires());
            
            // Immediately remove destroyed packets from wires after collision check
            removeDestroyedPacketsFromWiresImmediate();
        }

        // Check for packet loss and success (only during simulation)
        if (isSimulationMode) {
            checkPacketLossAndSuccess();

            // Check game flow conditions (only during simulation)
            gameFlowController.checkGameFlow();
        }

        // Phase 2 updates
        updateAbilityCooldowns(deltaTime);
        if (abilityManager != null) {
            abilityManager.update(deltaTime);
        }
        applyAbilityEffects();
        saveManager.updateSaveTimer(gameState, currentTime);

        // Update views
        Platform.runLater(() -> {
            gameView.update();
            hudView.update();
        });
    }

    /**
     * Processes packet injections from the level schedule based on current time.
     */
    private void processPacketInjections() {
        if (gameState.getCurrentLevel() == null) return;

        // Gate packet flow until reference systems (sources/destinations) are ready
        if (!areReferenceSystemsReady()) {
            return;
        }

        // Use temporal progress instead of real time for packet injections
        double currentTemporalTime = gameState.getTemporalProgress();

        // Debug output for packet injection tracking
        if ((int)(currentTemporalTime) % 5 == 0 && currentTemporalTime > 0) {
            int totalScheduled = gameState.getCurrentLevel().getPacketSchedule().size();
            int totalExecuted = (int) gameState.getCurrentLevel().getPacketSchedule().stream()
                    .filter(injection -> injection.isExecuted()).count();
            int totalDelivered = gameState.getTotalDeliveredPackets();
            int totalLost = gameState.getTotalLostPackets();

            java.lang.System.out.println("DEBUG: Packet injection progress - Time: " + String.format("%.1f", currentTemporalTime) +
                    "s, Scheduled: " + totalScheduled + ", Executed: " + totalExecuted +
                    ", Delivered: " + totalDelivered + ", Lost: " + totalLost);
        }

        // Debug: Always show packet injection status every few seconds
        if ((int)(currentTemporalTime) % 2 == 0 && currentTemporalTime > 0) {
            java.lang.System.out.println("DEBUG: Processing packet injections at temporal time " + String.format("%.1f", currentTemporalTime) +
                    " - Level timer: " + String.format("%.1f", gameState.getLevelTimer()) +
                    " - Packet schedule size: " + gameState.getCurrentLevel().getPacketSchedule().size());
        }

        for (PacketInjection injection : gameState.getCurrentLevel().getPacketSchedule()) {
            if (!injection.isExecuted() && injection.getTime() <= currentTemporalTime) {
                // Create a new packet for this injection attempt
                Packet packet = injection.createPacket();

                java.lang.System.out.println("DEBUG: Attempting packet injection " + packet.getClass().getSimpleName() +
                        " at time " + currentTemporalTime + " from system " + injection.getSourceSystem().getClass().getSimpleName());

                // Try to place the packet onto the first available wire from the source port
                boolean placed = tryPlacePacketOnOutgoingWire(packet, injection.getSourceSystem());

                if (placed) {
                    // Only now consider the packet active and mark the injection executed
                    gameState.addActivePacket(packet);
                    injection.setExecuted(true);
                    java.lang.System.out.println("DEBUG: Packet injected and placed on wire: " + packet.getClass().getSimpleName() +
                            " at time " + currentTemporalTime + " (active packets: " + gameState.getActivePacketCount() + ")");
                } else {
                    java.lang.System.out.println("DEBUG: Packet injection deferred - no available wire: " + packet.getClass().getSimpleName() +
                            " at time " + currentTemporalTime + ". Will retry when connections allow.");
                    // Do NOT mark executed; we'll retry in a subsequent frame when connections permit
                    debugPacketPlacementFailure(injection.getSourceSystem());
                }
            }
        }
    }

    /**
     * Checks if all system indicators are on (all systems are fully connected).
     */
    private boolean areAllIndicatorsOn() {
        if (gameState.getCurrentLevel() == null) return false;
        for (model.System system : gameState.getCurrentLevel().getSystems()) {
            if (!system.areAllPortsConnected()) { // Changed from system.isIndicatorVisible()
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true when reference systems are sufficiently wired for packet flow.
     * Relaxed per spec: only require that at least one source output and at least one
     * destination input are connected, rather than all of them. This aligns with the
     * connected-graph requirement without forcing full utilization of every port.
     */
    private boolean areReferenceSystemsReady() {
        if (gameState.getCurrentLevel() == null) return false;
        GameLevel level = gameState.getCurrentLevel();

        // Get all reference systems (not just sources/destinations since they can be both)
        List<model.ReferenceSystem> allReferenceSystems = level.getReferenceSystems();
        if (allReferenceSystems.isEmpty()) {
            java.lang.System.out.println("DEBUG: Reference systems not ready - no reference systems exist");
            return false;
        }

        // Check for at least one connected output port across all reference systems
        boolean anyOutputConnected = false;
        java.lang.System.out.println("DEBUG: Checking " + allReferenceSystems.size() + " reference systems for connected output ports");
        for (model.ReferenceSystem refSys : allReferenceSystems) {
            java.lang.System.out.println("DEBUG: Checking reference system " + refSys.getId() + " with " + refSys.getOutputPorts().size() + " output ports");
            for (Port out : refSys.getOutputPorts()) {
                java.lang.System.out.println("DEBUG: Reference output port " + out.getShape() + " connected: " + out.isConnected());
                if (out.isConnected()) {
                    anyOutputConnected = true;
                    break;
                }
            }
            if (anyOutputConnected) break;
        }
        if (!anyOutputConnected) {
            java.lang.System.out.println("DEBUG: Reference systems not ready - no reference system output ports connected");
            return false;
        }

        // Check for at least one connected input port across all reference systems
        boolean anyInputConnected = false;
        java.lang.System.out.println("DEBUG: Checking " + allReferenceSystems.size() + " reference systems for connected input ports");
        for (model.ReferenceSystem refSys : allReferenceSystems) {
            java.lang.System.out.println("DEBUG: Checking reference system " + refSys.getId() + " with " + refSys.getInputPorts().size() + " input ports");
            for (Port inPort : refSys.getInputPorts()) {
                java.lang.System.out.println("DEBUG: Reference input port " + inPort.getShape() + " connected: " + inPort.isConnected());
                if (inPort.isConnected()) {
                    anyInputConnected = true;
                    break;
                }
            }
            if (anyInputConnected) break;
        }
        if (!anyInputConnected) {
            java.lang.System.out.println("DEBUG: Reference systems not ready - no reference system input ports connected");
            return false;
        }

        java.lang.System.out.println("DEBUG: Reference systems are ready - packet injection can proceed");
        return true;
    }

    /**
     * Returns true if the network is connected (all systems can reach each other).
     * Packet flow is allowed when this returns true.
     */
    private boolean areAllSystemsFullyConnected() {
        if (gameState.getCurrentLevel() == null) return false;
        if (wiringController == null) return false;

        // Use network connectivity instead of individual port connections
        // This allows for more efficient network topologies while ensuring all systems can communicate
        return wiringController.isNetworkConnected(gameState);
    }

    /**
     * Updates all systems in the current level.
     */
    private void updateSystems(double deltaTime) {
        if (gameState.getCurrentLevel() == null) return;

        for (model.System system : gameState.getCurrentLevel().getSystems()) {
            if (system instanceof ReferenceSystem) {
                ((ReferenceSystem) system).update(gameState.getTemporalProgress());
            }
            // Award coins only once at the moment of entry: consume pending flags
            for (Port inputPort : system.getInputPorts()) {
                Packet p = inputPort.getCurrentPacket();
                if (p != null && p.isCoinAwardPending()) {
                    gameState.addCoins(p.getCoinValue());
                    p.setCoinAwardPending(false);
                }
            }
            system.processInputs();
            system.processStorage();
        }
    }

    /**
     * Triggers Anti-Trojan systems to scan and convert trojans in range.
     */
    private void runAntiTrojanScans() {
        if (gameState.getCurrentLevel() == null) return;
        for (model.System system : gameState.getCurrentLevel().getSystems()) {
            if (system instanceof AntiTrojanSystem) {
                ((AntiTrojanSystem) system).detectAndConvertTrojans();
            }
        }
    }

    /**
     * Updates system deactivation timers and indicator status.
     */
    private void updateSystemDeactivationTimers(double deltaTime) {
        if (gameState.getCurrentLevel() == null) return;

        for (model.System system : gameState.getCurrentLevel().getSystems()) {
            // Call the full update method to ensure indicators are updated
            system.update(deltaTime);
        }
    }

    /**
     * Updates packet movement on all wires.
     */
    private void updateWirePacketMovement(double deltaTime) {
        if (gameState.getCurrentLevel() == null) return;

        int totalPacketsOnWires = 0;
        // Check smooth curve setting and pass it to wire packet movement
        boolean useSmoothCurves = true; // Default to smooth curves
        Object setting = gameState.getGameSettings().get("smoothWireCurves");
        if (setting instanceof Boolean) {
            useSmoothCurves = (Boolean) setting;
        }
        
        for (WireConnection connection : gameState.getCurrentLevel().getWireConnections()) {
            if (connection.isActive()) {
                int packetsBefore = connection.getPacketsOnWire().size();
                connection.updatePacketMovement(deltaTime, useSmoothCurves);
                totalPacketsOnWires += connection.getPacketsOnWire().size();

                // Debug: Log packet movement if there are packets
                if (packetsBefore > 0) {
                    java.lang.System.out.println("DEBUG: Wire " + connection.getId().substring(0, 8) +
                            " has " + connection.getPacketsOnWire().size() + " packets, deltaTime=" + deltaTime);
                }
            }
        }

        // Debug: Log total packets on wires vs active packets
        if (totalPacketsOnWires != gameState.getActivePacketCount()) {
            java.lang.System.out.println("DEBUG: Packet count mismatch - On wires: " + totalPacketsOnWires +
                    ", Active: " + gameState.getActivePacketCount());
        }
    }

    /**
     * Processes wire connections to transfer packets.
     */
    private void processWireConnections() {
        if (gameState.getCurrentLevel() == null) return;

        for (WireConnection connection : gameState.getCurrentLevel().getWireConnections()) {
            if (connection.isActive()) {
                boolean transferred = connection.transferPacket();
                if (transferred) {
                    // Debug: Log successful packet transfers
                    java.lang.System.out.println("DEBUG: Packet transferred on wire " + connection.getId() +
                            " (packets on wire: " + connection.getPacketsOnWire().size() + ")");
                }
            }
        }
    }

    /**
     * Checks for packet loss and success, playing appropriate sound effects.
     */
    private void checkPacketLossAndSuccess() {
        if (gameState.getActivePackets() == null) return;

        List<Packet> packetsToRemove = new ArrayList<>();
        int lostThisFrame = 0;
        int deliveredThisFrame = 0;

        for (Packet packet : gameState.getActivePackets()) {
            // Check for packet loss OR delivery (inactive packets)
            boolean flaggedLost = packet.shouldBeLost() || packet.shouldBeDestroyedByTime() || packet.isLost();
            if (!packet.isActive() || flaggedLost) {
                if (packet.isActive() || packet.isLost()) {
                    // Packet was just lost - play lost sound
                    if (soundManager != null) {
                        soundManager.playPacketLostSound();
                    }
                    java.lang.System.out.println("DEBUG: Packet " + packet.getClass().getSimpleName() + " marked as lost/destroyed");
                    // Count as lost due to collision/impact/time/off-wire
                    gameState.incrementLostPackets();
                    lostThisFrame++;
                } else {
                    // Packet was delivered (made inactive by destination system)
                    java.lang.System.out.println("DEBUG: Removing delivered packet " + packet.getClass().getSimpleName() + " from active list");
                    deliveredThisFrame++;
                }
                packetsToRemove.add(packet);
            }
        }

        // Remove lost/delivered packets from active list
        gameState.getActivePackets().removeAll(packetsToRemove);
        
        // Also remove destroyed packets from wires to free up wire space
        removeDestroyedPacketsFromWires(packetsToRemove);

        if (!packetsToRemove.isEmpty()) {
            java.lang.System.out.println("DEBUG: Removed " + packetsToRemove.size() + " packets from active list. " +
                    "Active count now: " + gameState.getActivePacketCount() +
                    " (Lost: " + lostThisFrame + ", Delivered: " + deliveredThisFrame + ")");
        }

        // Update the packet loss percentage in the game state
        double currentPacketLoss = gameState.calculatePacketLossPercentage();
        gameState.setPacketLoss(currentPacketLoss);

        // Debug output for packet loss tracking
        if (currentPacketLoss > 0 || lostThisFrame > 0) {
            java.lang.System.out.println("DEBUG: Packet loss updated to: " + String.format("%.1f", currentPacketLoss) +
                    "% (Lost: " + gameState.getLostPacketsCount() + ", Total injected: " + gameState.getTotalInjectedPackets() +
                    ", Active: " + gameState.getActivePacketCount() + ")");
        }

        // Check for packets reaching reference systems (success)
        if (gameState.getCurrentLevel() != null) {
            for (model.System system : gameState.getCurrentLevel().getSystems()) {
                if (system instanceof ReferenceSystem && !((ReferenceSystem) system).isSource()) {
                    // Check if any packets reached this destination reference system
                    if (((ReferenceSystem) system).hasReceivedPackets()) {
                        // Play success sound for packets reaching destination
                        if (soundManager != null) {
                            soundManager.playPacketSuccessSound();
                        }
                    }
                }
            }
        }
    }

    /**
     * Loads a level by ID.
     */
    public void loadLevel(String levelId) {
        try {
            java.lang.System.out.println("DEBUG: loadLevel called with levelId: " + levelId);
            GameLevel level = createLevel(levelId);
            java.lang.System.out.println("DEBUG: Loading level: " + levelId +
                    ", duration: " + level.getLevelDuration() +
                    ", initialWireLength: " + level.getInitialWireLength() +
                    ", packetSchedule size: " + (level.getPacketSchedule() != null ? level.getPacketSchedule().size() : "null"));
            // Preserve coins from previous levels - don't reset them, but ensure minimum of 10
            int currentCoins = gameState.getCoins();
            if (currentCoins == 0) {
                currentCoins = 10; // Set initial coins to 10 if starting fresh
            }

            gameState.setCurrentLevel(level);
            // Initialize remaining wire length adjusted by any pre-existing connections in the level definition
            double initialBudget = level.getInitialWireLength();
            double preConsumed = level.getTotalWireLengthConsumed();
            double adjustedBudget = Math.max(0.0, initialBudget - preConsumed);
            gameState.setRemainingWireLength(adjustedBudget);
            // Keep the coins from previous levels instead of resetting to 0
            gameState.setCoins(currentCoins);
            gameState.setPacketLoss(0);
            gameState.setTemporalProgress(0);
            // Reset lost packets count for new level (each level should start fresh)
            gameState.setLostPacketsCount(0);

            // Clear all wire connections for fresh start
            level.setWireConnections(new ArrayList<>());
            java.lang.System.out.println("DEBUG: Cleared all wire connections for fresh start");

            // Ensure wire connections and packet sources are correctly rebound after JSON load
            updateWireConnectionPortReferences(level);
            restorePortConnectionsFromWires(level);
            rebindPacketInjectionSources(level);

            // Set the level in the game view
            gameView.setLevel(level);

            // Clear active packets
            gameState.setActivePackets(new ArrayList<>());
            
            // Save the initial state for restart functionality (after resetting lost packets)
            gameState.saveLevelStartState();

            // Ensure we start in editing mode
            enterEditingMode();

        } catch (Exception e) {
            java.lang.System.err.println("Failed to load level: " + levelId + " - " + e.getMessage());
            e.printStackTrace();
        }
    }







    /**
     * Creates a reference system with appropriate ports and positioning.
     */
    private ReferenceSystem createReferenceSystem(double x, double y, boolean isSource, String id) {
        ReferenceSystem system = new ReferenceSystem(new Point2D(x, y), isSource);
        system.setId(id);

        if (isSource) {
            // Source systems have output ports only
            system.addOutputPort(new Port(PortShape.SQUARE, system, new Point2D(x + 20, y), false));
            system.addOutputPort(new Port(PortShape.TRIANGLE, system, new Point2D(x + 20, y + 20), false));
            system.addOutputPort(new Port(PortShape.SQUARE, system, new Point2D(x + 20, y + 40), false));
        } else {
            // Destination systems have input ports only
            system.addInputPort(new Port(PortShape.SQUARE, system, new Point2D(x - 20, y), true));
            system.addInputPort(new Port(PortShape.TRIANGLE, system, new Point2D(x - 20, y + 20), true));
            system.addInputPort(new Port(PortShape.SQUARE, system, new Point2D(x - 20, y + 40), true));
        }

        return system;
    }

    /**
     * Generates packet injection schedule for new reference systems based on level complexity.
     */
    private List<PacketInjection> generatePacketScheduleForLevel(String levelId, List<model.System> newReferenceSystems) {
        List<PacketInjection> packetSchedule = new ArrayList<>();

        // Find source systems from the new reference systems
        List<ReferenceSystem> newSources = new ArrayList<>();
        for (model.System system : newReferenceSystems) {
            if (system instanceof ReferenceSystem && ((ReferenceSystem) system).isSource()) {
                newSources.add((ReferenceSystem) system);
            }
        }

        // Generate packet injections based on level complexity
        int baseInjectionCount = 5; // Base number of injections per source
        int levelMultiplier = getLevelMultiplier(levelId);

        for (ReferenceSystem source : newSources) {
            double baseTime = 2.0; // Start injecting at 2 seconds
            double timeInterval = 3.0; // Inject every 3 seconds

            for (int i = 0; i < baseInjectionCount * levelMultiplier; i++) {
                double injectionTime = baseTime + (i * timeInterval);

                // Alternate between different packet types for variety
                PacketType packetType;
                switch (i % 3) {
                    case 0:
                        packetType = PacketType.SMALL_MESSENGER;
                        break;
                    case 1:
                        packetType = PacketType.SQUARE_MESSENGER;
                        break;
                    case 2:
                        packetType = PacketType.TRIANGLE_MESSENGER;
                        break;
                    default:
                        packetType = PacketType.SMALL_MESSENGER;
                }

                PacketInjection injection = new PacketInjection(injectionTime, packetType, source);
                packetSchedule.add(injection);
            }
        }

        java.lang.System.out.println("DEBUG: Generated " + packetSchedule.size() + " packet injections for new reference systems in " + levelId);
        return packetSchedule;
    }

    /**
     * Gets the level multiplier for determining packet injection complexity.
     */
    private int getLevelMultiplier(String levelId) {
        switch (levelId) {
            case "level1": return 1;
            case "level2": return 2;
            case "level3": return 3;
            case "level4": return 4;
            case "level5": return 5;
            default: return 1;
        }
    }

    /**
     * Restarts the current level with a fresh start (clears all connections).
     * Restores coins and lost packets to their pre-level state.
     */
    public void restartLevelPreservingPrevious() {
        try {
            GameLevel currentLevel = gameState.getCurrentLevel();
            if (currentLevel == null || currentLevel.getLevelId() == null) {
                return;
            }

            String levelId = currentLevel.getLevelId();

            // Restore state to what it was before the level started
            gameState.restoreToLevelStart();
            
            // Clear abilities and cooldowns (they should not persist across restarts)
            activeAbilities.clear();
            abilityCooldowns.clear();
            
            // Reload the level fresh (this will also save the restored state as new level start state)
            loadLevel(levelId);

            java.lang.System.out.println("DEBUG: Level restarted with restored coins and lost packets");

            // Keep user in editing mode
            enterEditingMode();
        } catch (Exception e) {
            java.lang.System.err.println("Failed to restart level: " + e.getMessage());
        }
    }

    /**
     * Checks if two systems are equivalent (same type and position).
     */
    private boolean systemsAreEquivalent(model.System system1, model.System system2) {
        if (system1.getClass() != system2.getClass()) {
            return false;
        }

        Point2D pos1 = system1.getPosition();
        Point2D pos2 = system2.getPosition();

        if (pos1 == null || pos2 == null) {
            return pos1 == pos2;
        }

        // Consider systems equivalent if they're very close (within 10 pixels)
        double distance = pos1.distanceTo(pos2);
        return distance < 10.0;
    }

    /**
     * Restores port connection status from wire connections.
     * Fixes cases where port references might get out of sync.
     */
    private void restorePortConnectionsFromWires(GameLevel level) {
        // First, mark all ports as disconnected
        for (model.System system : level.getSystems()) {
            for (Port port : system.getInputPorts()) {
                port.setConnected(false);
            }
            for (Port port : system.getOutputPorts()) {
                port.setConnected(false);
            }
        }

        // Then, mark ports as connected based on active wire connections
        for (WireConnection connection : level.getWireConnections()) {
            if (connection.isActive()) {
                Port sourcePort = connection.getSourcePort();
                Port destPort = connection.getDestinationPort();

                if (sourcePort != null) {
                    sourcePort.setConnected(true);
                }
                if (destPort != null) {
                    destPort.setConnected(true);
                }
            }
        }

        // Update system indicators after restoring port connections
        for (model.System system : level.getSystems()) {
            system.update(0.0); // Force indicator update
        }

        java.lang.System.out.println("DEBUG: Restored port connections from " + level.getWireConnections().size() + " wire connections");
    }

    /**
     * Updates wire connection port references to match the current level's system port instances.
     * This is critical for preventing packets from moving outside wires during level loading.
     */
    private void updateWireConnectionPortReferences(GameLevel level) {
        java.lang.System.out.println("DEBUG: Updating wire connection port references...");

        int updatedConnections = 0;

        for (WireConnection connection : level.getWireConnections()) {
            Port originalSourcePort = connection.getSourcePort();
            Port originalDestPort = connection.getDestinationPort();

            // Find the matching ports in the current level's systems
            Port newSourcePort = findMatchingPort(originalSourcePort, level.getSystems());
            Port newDestPort = findMatchingPort(originalDestPort, level.getSystems());

            if (newSourcePort != null && newDestPort != null) {
                // Update the wire connection to use the current level's port instances
                connection.updatePortReferences(newSourcePort, newDestPort);
                updatedConnections++;

                java.lang.System.out.println("DEBUG: Updated wire connection port references: " +
                        originalSourcePort.getPosition() + " -> " + originalDestPort.getPosition());
            } else {
                java.lang.System.err.println("ERROR: Could not find matching ports for wire connection: " +
                        (originalSourcePort != null ? originalSourcePort.getPosition() : "null") + " -> " +
                        (originalDestPort != null ? originalDestPort.getPosition() : "null"));
            }
        }

        java.lang.System.out.println("DEBUG: Updated " + updatedConnections + " wire connection port references");
    }

    /**
     * Ensures PacketInjection source systems reference the correct instances in the given level.
     */
    private void rebindPacketInjectionSources(GameLevel level) {
        if (level == null || level.getPacketSchedule() == null) return;
        java.util.Map<String, model.System> byId = new java.util.HashMap<>();
        for (model.System s : level.getSystems()) {
            byId.put(s.getId(), s);
        }
        for (PacketInjection inj : level.getPacketSchedule()) {
            model.System src = inj.getSourceSystem();
            if (src == null) continue;
            // If the source object isn't one from this level's list, replace with the matching one by position/type
            if (!level.getSystems().contains(src)) {
                model.System replacement = findEquivalentSystem(src, level.getSystems());
                if (replacement != null) {
                    inj.setSourceSystem(replacement);
                }
            }
        }
    }

    /**
     * Finds an equivalent system in the list by type and approximate position.
     */
    private model.System findEquivalentSystem(model.System target, java.util.List<model.System> systems) {
        for (model.System s : systems) {
            if (s.getClass() != target.getClass()) continue;
            Point2D p1 = s.getPosition();
            Point2D p2 = target.getPosition();
            if (p1 != null && p2 != null && p1.distanceTo(p2) < 1.0) {
                return s;
            }
        }
        return null;
    }

    /**
     * Finds a port in the current level's systems that matches the given port by position and type.
     */
    private Port findMatchingPort(Port targetPort, List<model.System> systems) {
        if (targetPort == null) return null;

        Point2D targetPosition = targetPort.getPosition();
        PortShape targetShape = targetPort.getShape();
        boolean targetIsInput = targetPort.isInput();

        for (model.System system : systems) {
            // Check output ports
            for (Port port : system.getOutputPorts()) {
                if (portsMatch(port, targetPosition, targetShape, targetIsInput)) {
                    return port;
                }
            }

            // Check input ports
            for (Port port : system.getInputPorts()) {
                if (portsMatch(port, targetPosition, targetShape, targetIsInput)) {
                    return port;
                }
            }
        }

        return null;
    }

    /**
     * Checks if a port matches the target criteria.
     */
    private boolean portsMatch(Port port, Point2D targetPosition, PortShape targetShape, boolean targetIsInput) {
        if (port.isInput() != targetIsInput) return false;
        if (port.getShape() != targetShape) return false;

        Point2D portPosition = port.getPosition();
        if (portPosition == null || targetPosition == null) return false;

        // Consider ports matching if they're very close (within 5 pixels)
        double distance = portPosition.distanceTo(targetPosition);
        return distance < 5.0;
    }

    /**
     * Estimates the minimum wire length needed for a minimal spanning tree connection.
     */
    private double estimateMinimumWireNeeded(List<model.System> systems) {
        if (systems.size() < 2) return 0.0;

        double totalDistance = 0.0;
        // Simple heuristic: calculate distances between consecutive systems when sorted by position
        List<model.System> sortedSystems = new ArrayList<>(systems);
        sortedSystems.sort((s1, s2) -> {
            Point2D p1 = s1.getPosition();
            Point2D p2 = s2.getPosition();
            if (p1 == null || p2 == null) return 0;
            return Double.compare(p1.getX() + p1.getY(), p2.getX() + p2.getY());
        });

        for (int i = 0; i < sortedSystems.size() - 1; i++) {
            Point2D pos1 = sortedSystems.get(i).getPosition();
            Point2D pos2 = sortedSystems.get(i + 1).getPosition();
            if (pos1 != null && pos2 != null) {
                totalDistance += pos1.distanceTo(pos2);
            }
        }

        return totalDistance;
    }

    /**
     * Creates a level based on the level ID.
     * First tries to load from JSON to get the level template.
     * The JSON file contains the level template with systems and packet schedule,
     * but wire connections are created by the user during gameplay.
     */
    private GameLevel createLevel(String levelId) {
        java.lang.System.out.println("DEBUG: createLevel called with levelId: " + levelId);

        // Always try to load from JSON first to get the level template
        GameLevel jsonLevel = loadLevelFromJSON(levelId);
        if (jsonLevel != null) {
            java.lang.System.out.println("DEBUG: Successfully loaded level template from JSON: " + levelId);
            java.lang.System.out.println("DEBUG: JSON level has " + jsonLevel.getSystems().size() + " systems");
            java.lang.System.out.println("DEBUG: JSON level has " + jsonLevel.getPacketSchedule().size() + " packet injections");
            java.lang.System.out.println("DEBUG: JSON level has " + jsonLevel.getWireConnections().size() + " wire connections (should be 0)");
            return jsonLevel;
        }

        // Only fall back to hardcoded levels if JSON loading completely fails
        java.lang.System.out.println("DEBUG: JSON loading failed, falling back to hardcoded level creation");
        switch (levelId) {
            case "level1":
                java.lang.System.out.println("DEBUG: Using hardcoded createLevel1() for level1");
                return createLevel1();
            case "level2": return createLevel2();
            case "level3": return createLevel3();
            case "level4": return createLevel4();
            case "level5": return createLevel5();
            default:
                java.lang.System.out.println("DEBUG: Using hardcoded createLevel1() as default");
                return createLevel1();
        }
    }

    /**
     * Loads a level from its JSON configuration file.
     */
    private GameLevel loadLevelFromJSON(String levelId) {
        try {
            String resourcePath = "/levels/" + levelId + ".json";
            java.lang.System.out.println("DEBUG: Attempting to load JSON from: " + resourcePath);

            InputStream inputStream = getClass().getResourceAsStream(resourcePath);

            if (inputStream == null) {
                java.lang.System.err.println("Could not find JSON resource: " + resourcePath);
                return null;
            }

            java.lang.System.out.println("DEBUG: Found JSON resource, attempting to deserialize...");

            ObjectMapper mapper = new ObjectMapper();
            GameLevel level = mapper.readValue(inputStream, GameLevel.class);
            inputStream.close();

            java.lang.System.out.println("DEBUG: JSON deserialization completed successfully");

            // Validate the loaded level
            if (level.getLevelId() == null || level.getName() == null) {
                java.lang.System.err.println("Invalid JSON data for level: " + levelId);
                return null;
            }

            // Debug: Check what was loaded from JSON
            java.lang.System.out.println("DEBUG: JSON loaded level: " + level.getName());
            java.lang.System.out.println("DEBUG: JSON packetSchedule size: " + (level.getPacketSchedule() != null ? level.getPacketSchedule().size() : "null"));
            java.lang.System.out.println("DEBUG: JSON packetInjectionSchedule size: " + (level.getPacketInjectionSchedule() != null ? level.getPacketInjectionSchedule().size() : "null"));

            // Normalize references so ports know their parent systems and systems know their parent level
            // This ensures wire transfers can see packets on system output ports
            if (level.getSystems() != null) {
                level.setSystems(level.getSystems());
            }

            java.lang.System.out.println("Loaded level from JSON: " + level.getName() +
                    " with " + level.getSystems().size() + " systems");

            // Convert JSON packet schedule to PacketInjection objects
            level.convertPacketScheduleFromJSON();
            java.lang.System.out.println("Converted " + level.getPacketSchedule().size() + " packet injections");

            return level;

        } catch (Exception e) {
            java.lang.System.err.println("Failed to load level from JSON: " + levelId + " - " + e.getMessage());
            java.lang.System.err.println("Exception type: " + e.getClass().getSimpleName());
            e.printStackTrace();
            return null;
        }
    }

    private GameLevel createLevel1() {
        java.lang.System.out.println("DEBUG: createLevel1() method called - creating new level with 30 packets from 2 reference systems and 4 normal systems");

        // Enhanced Level 1 - Two reference systems and four normal systems
        GameLevel level = new GameLevel();
        level.setLevelId("level1");
        level.setName("Enhanced Foundation");
        level.setInitialWireLength(6000.0); // Increased for more complex network
        level.setDuration(90.0); // Standard duration

        // Create 2 reference systems
        ReferenceSystem refSystem1 = new ReferenceSystem(new Point2D(100, 200));
        ReferenceSystem refSystem2 = new ReferenceSystem(new Point2D(700, 500));

        // Create 4 normal systems
        model.System normalSystem1 = new NormalSystem(new Point2D(300, 150), SystemType.NORMAL);
        model.System normalSystem2 = new NormalSystem(new Point2D(300, 350), SystemType.NORMAL);
        model.System normalSystem3 = new NormalSystem(new Point2D(500, 150), SystemType.NORMAL);
        model.System normalSystem4 = new NormalSystem(new Point2D(500, 350), SystemType.NORMAL);

        // Reference System 1 ports: 3 input + 3 output (one of each type)
        refSystem1.addInputPort(new Port(PortShape.SQUARE, refSystem1, new Point2D(80, 180), true));
        refSystem1.addInputPort(new Port(PortShape.TRIANGLE, refSystem1, new Point2D(80, 200), true));
        refSystem1.addInputPort(new Port(PortShape.HEXAGON, refSystem1, new Point2D(80, 220), true));
        refSystem1.addOutputPort(new Port(PortShape.SQUARE, refSystem1, new Point2D(120, 180), false));
        refSystem1.addOutputPort(new Port(PortShape.TRIANGLE, refSystem1, new Point2D(120, 200), false));
        refSystem1.addOutputPort(new Port(PortShape.HEXAGON, refSystem1, new Point2D(120, 220), false));

        // Reference System 2 ports: 3 input + 3 output (one of each type)
        refSystem2.addInputPort(new Port(PortShape.SQUARE, refSystem2, new Point2D(680, 480), true));
        refSystem2.addInputPort(new Port(PortShape.TRIANGLE, refSystem2, new Point2D(680, 500), true));
        refSystem2.addInputPort(new Port(PortShape.HEXAGON, refSystem2, new Point2D(680, 520), true));
        refSystem2.addOutputPort(new Port(PortShape.SQUARE, refSystem2, new Point2D(720, 480), false));
        refSystem2.addOutputPort(new Port(PortShape.TRIANGLE, refSystem2, new Point2D(720, 500), false));
        refSystem2.addOutputPort(new Port(PortShape.HEXAGON, refSystem2, new Point2D(720, 520), false));

        // Normal System 1: 2 input (triangle, square) + 3 output (one of each)
        normalSystem1.addInputPort(new Port(PortShape.TRIANGLE, normalSystem1, new Point2D(280, 140), true));
        normalSystem1.addInputPort(new Port(PortShape.SQUARE, normalSystem1, new Point2D(280, 160), true));
        normalSystem1.addOutputPort(new Port(PortShape.SQUARE, normalSystem1, new Point2D(320, 140), false));
        normalSystem1.addOutputPort(new Port(PortShape.TRIANGLE, normalSystem1, new Point2D(320, 150), false));
        normalSystem1.addOutputPort(new Port(PortShape.HEXAGON, normalSystem1, new Point2D(320, 160), false));

        // Normal System 2: 2 input (hexagon, triangle) + 1 output (hexagon)
        normalSystem2.addInputPort(new Port(PortShape.HEXAGON, normalSystem2, new Point2D(280, 340), true));
        normalSystem2.addInputPort(new Port(PortShape.TRIANGLE, normalSystem2, new Point2D(280, 360), true));
        normalSystem2.addOutputPort(new Port(PortShape.HEXAGON, normalSystem2, new Point2D(320, 350), false));

        // Normal System 3: 2 input (hexagon, square) + 2 output (hexagon, square)
        normalSystem3.addInputPort(new Port(PortShape.HEXAGON, normalSystem3, new Point2D(480, 140), true));
        normalSystem3.addInputPort(new Port(PortShape.SQUARE, normalSystem3, new Point2D(480, 160), true));
        normalSystem3.addOutputPort(new Port(PortShape.HEXAGON, normalSystem3, new Point2D(520, 140), false));
        normalSystem3.addOutputPort(new Port(PortShape.SQUARE, normalSystem3, new Point2D(520, 160), false));

        // Normal System 4: 2 input (square, triangle) + 2 output (square, triangle)
        normalSystem4.addInputPort(new Port(PortShape.SQUARE, normalSystem4, new Point2D(480, 340), true));
        normalSystem4.addInputPort(new Port(PortShape.TRIANGLE, normalSystem4, new Point2D(480, 360), true));
        normalSystem4.addOutputPort(new Port(PortShape.SQUARE, normalSystem4, new Point2D(520, 340), false));
        normalSystem4.addOutputPort(new Port(PortShape.TRIANGLE, normalSystem4, new Point2D(520, 360), false));

        level.getSystems().addAll(Arrays.asList(refSystem1, refSystem2, normalSystem1, normalSystem2, normalSystem3, normalSystem4));

        // Debug: Print port counts
        int totalInputPorts = 0;
        int totalOutputPorts = 0;
        for (model.System system : level.getSystems()) {
            totalInputPorts += system.getInputPorts().size();
            totalOutputPorts += system.getOutputPorts().size();
        }
        java.lang.System.out.println("DEBUG: Level 1 created with " + totalInputPorts + " input ports and " + totalOutputPorts + " output ports (Total: " + (totalInputPorts + totalOutputPorts) + ")");

        // Packet injection schedule: 30 packets total (15 from each reference system)
        // Reference System 1: 15 packets (5 of each type)
        double time = 2.0;
        for (int i = 0; i < 5; i++) {
            level.getPacketSchedule().add(new PacketInjection(time, PacketType.SMALL_MESSENGER, refSystem1));
            level.getPacketSchedule().add(new PacketInjection(time + 1.0, PacketType.SQUARE_MESSENGER, refSystem1));
            level.getPacketSchedule().add(new PacketInjection(time + 2.0, PacketType.TRIANGLE_MESSENGER, refSystem1));
            time += 6.0; // Space out packets
        }

        // Reference System 2: 15 packets (5 of each type)
        time = 4.0; // Start slightly offset from first system
        for (int i = 0; i < 5; i++) {
            level.getPacketSchedule().add(new PacketInjection(time, PacketType.SMALL_MESSENGER, refSystem2));
            level.getPacketSchedule().add(new PacketInjection(time + 1.0, PacketType.SQUARE_MESSENGER, refSystem2));
            level.getPacketSchedule().add(new PacketInjection(time + 2.0, PacketType.TRIANGLE_MESSENGER, refSystem2));
            time += 6.0; // Space out packets
        }

        java.lang.System.out.println("DEBUG: Level 1 created with " + level.getPacketSchedule().size() + " packet injections (30 total: 10 small hexagons, 10 squares, 10 triangles)");

        return level;
    }

    private GameLevel createLevel2() {
        // Intermediate Level - Add spy systems and confidential packets
        GameLevel level = new GameLevel();
        level.setLevelId("level2");
        level.setName("Intermediate - Spy Networks");
        level.setInitialWireLength(2700.0); // Increased budget for new systems that require longer connections
        level.setDuration(90.0);

        // Build on level 1 systems
        ReferenceSystem source = new ReferenceSystem(new Point2D(100, 300), true);

        ReferenceSystem destination = new ReferenceSystem(new Point2D(700, 300), false);

        model.System normalSystem1 = new NormalSystem(new Point2D(400, 200), SystemType.NORMAL);
        model.System normalSystem2 = new NormalSystem(new Point2D(400, 400), SystemType.NORMAL);

        // Add new spy systems
        SpySystem spySystem1 = new SpySystem(new Point2D(300, 150));
        SpySystem spySystem2 = new SpySystem(new Point2D(500, 450));

        // Add ports (same as level 1 plus spy ports)
        source.addOutputPort(new Port(PortShape.SQUARE, source, new Point2D(120, 300), false));
        source.addOutputPort(new Port(PortShape.TRIANGLE, source, new Point2D(120, 320), false));
        // Extra output on source for level 2
        source.addOutputPort(new Port(PortShape.SQUARE, source, new Point2D(120, 340), false));

        spySystem1.addInputPort(new Port(PortShape.SQUARE, spySystem1, new Point2D(280, 150), true));
        spySystem1.addOutputPort(new Port(PortShape.TRIANGLE, spySystem1, new Point2D(320, 150), false));
        // Extra input/output on spySystem1
        spySystem1.addInputPort(new Port(PortShape.TRIANGLE, spySystem1, new Point2D(280, 160), true));
        spySystem1.addOutputPort(new Port(PortShape.SQUARE, spySystem1, new Point2D(320, 160), false));

        spySystem2.addInputPort(new Port(PortShape.TRIANGLE, spySystem2, new Point2D(480, 450), true));
        spySystem2.addOutputPort(new Port(PortShape.SQUARE, spySystem2, new Point2D(520, 450), false));
        // Extra output on spySystem2
        spySystem2.addOutputPort(new Port(PortShape.TRIANGLE, spySystem2, new Point2D(520, 460), false));
        // Additional input on spySystem2 to balance port counts (fixes 21-port issue)
        spySystem2.addInputPort(new Port(PortShape.SQUARE, spySystem2, new Point2D(480, 460), true));

        normalSystem1.addInputPort(new Port(PortShape.SQUARE, normalSystem1, new Point2D(380, 200), true));
        normalSystem1.addOutputPort(new Port(PortShape.TRIANGLE, normalSystem1, new Point2D(420, 200), false));
        // Extra input/output on normalSystem1
        normalSystem1.addInputPort(new Port(PortShape.TRIANGLE, normalSystem1, new Point2D(380, 210), true));
        normalSystem1.addOutputPort(new Port(PortShape.TRIANGLE, normalSystem1, new Point2D(420, 210), false));

        normalSystem2.addInputPort(new Port(PortShape.TRIANGLE, normalSystem2, new Point2D(380, 400), true));
        normalSystem2.addOutputPort(new Port(PortShape.SQUARE, normalSystem2, new Point2D(420, 400), false));
        // Extra output on normalSystem2
        normalSystem2.addOutputPort(new Port(PortShape.SQUARE, normalSystem2, new Point2D(420, 410), false));
        // Additional input port to balance the level (fixes the 13-port issue)
        normalSystem2.addInputPort(new Port(PortShape.TRIANGLE, normalSystem2, new Point2D(380, 420), true));

        destination.addInputPort(new Port(PortShape.TRIANGLE, destination, new Point2D(680, 300), true));
        destination.addInputPort(new Port(PortShape.SQUARE, destination, new Point2D(680, 320), true));
        // Extra input on destination
        destination.addInputPort(new Port(PortShape.SQUARE, destination, new Point2D(680, 340), true));

        // Add systems via level.addSystem to ensure parentLevel is set for each system
        level.addSystem(source);
        level.addSystem(destination);
        level.addSystem(normalSystem1);
        level.addSystem(normalSystem2);
        level.addSystem(spySystem1);
        level.addSystem(spySystem2);

        // Debug: Print port counts to verify the fix
        int totalInputPorts = 0;
        int totalOutputPorts = 0;
        for (model.System system : level.getSystems()) {
            totalInputPorts += system.getInputPorts().size();
            totalOutputPorts += system.getOutputPorts().size();
        }
        java.lang.System.out.println("DEBUG: Level 2 created with " + totalInputPorts + " input ports and " + totalOutputPorts + " output ports (Total: " + (totalInputPorts + totalOutputPorts) + ")");

        // Early injections for immediate visual activity
        level.getPacketSchedule().add(new PacketInjection(0.0, PacketType.SMALL_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(1.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(2.5, PacketType.SQUARE_MESSENGER, source));

        // Enhanced packet schedule with confidential packets (15 packets total)
        level.getPacketSchedule().add(new PacketInjection(5.0, PacketType.SMALL_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(10.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(15.0, PacketType.SQUARE_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(20.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(25.0, PacketType.SMALL_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(30.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(35.0, PacketType.SQUARE_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(40.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(45.0, PacketType.SMALL_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(50.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(55.0, PacketType.SQUARE_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(60.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(65.0, PacketType.SMALL_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(70.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(75.0, PacketType.SQUARE_MESSENGER, source));

        java.lang.System.out.println("DEBUG: Hardcoded Level 2 created with " + level.getPacketSchedule().size() + " packet injections");

        return level;
    }

    private GameLevel createLevel3() {
        // Advanced Level - Add saboteur systems and VPN protection
        GameLevel level = new GameLevel();
        level.setLevelId("level3");
        level.setName("Advanced - Saboteurs & VPN");
        level.setInitialWireLength(3300.0);
        level.setDuration(120.0);

        // Build on previous levels
        ReferenceSystem source = new ReferenceSystem(new Point2D(100, 300), true);

        ReferenceSystem destination = new ReferenceSystem(new Point2D(700, 300), false);

        model.System normalSystem1 = new NormalSystem(new Point2D(400, 200), SystemType.NORMAL);
        model.System normalSystem2 = new NormalSystem(new Point2D(400, 400), SystemType.NORMAL);
        SpySystem spySystem1 = new SpySystem(new Point2D(300, 150));
        SpySystem spySystem2 = new SpySystem(new Point2D(500, 450));

        // Add new systems
        SaboteurSystem saboteur = new SaboteurSystem(new Point2D(600, 200));
        VPNSystem vpnSystem = new VPNSystem(new Point2D(200, 450));

        // Add ports
        source.addOutputPort(new Port(PortShape.SQUARE, source, new Point2D(120, 300), false));
        source.addOutputPort(new Port(PortShape.TRIANGLE, source, new Point2D(120, 320), false));

        vpnSystem.addInputPort(new Port(PortShape.SQUARE, vpnSystem, new Point2D(180, 450), true));
        vpnSystem.addOutputPort(new Port(PortShape.TRIANGLE, vpnSystem, new Point2D(220, 450), false));

        spySystem1.addInputPort(new Port(PortShape.SQUARE, spySystem1, new Point2D(280, 150), true));
        spySystem1.addOutputPort(new Port(PortShape.TRIANGLE, spySystem1, new Point2D(320, 150), false));

        normalSystem1.addInputPort(new Port(PortShape.SQUARE, normalSystem1, new Point2D(380, 200), true));
        normalSystem1.addOutputPort(new Port(PortShape.TRIANGLE, normalSystem1, new Point2D(420, 200), false));

        saboteur.addInputPort(new Port(PortShape.TRIANGLE, saboteur, new Point2D(580, 200), true));
        saboteur.addOutputPort(new Port(PortShape.SQUARE, saboteur, new Point2D(620, 200), false));

        normalSystem2.addInputPort(new Port(PortShape.TRIANGLE, normalSystem2, new Point2D(380, 400), true));
        normalSystem2.addOutputPort(new Port(PortShape.SQUARE, normalSystem2, new Point2D(420, 400), false));

        spySystem2.addInputPort(new Port(PortShape.TRIANGLE, spySystem2, new Point2D(480, 450), true));
        spySystem2.addOutputPort(new Port(PortShape.SQUARE, spySystem2, new Point2D(520, 450), false));

        destination.addInputPort(new Port(PortShape.TRIANGLE, destination, new Point2D(680, 300), true));
        destination.addInputPort(new Port(PortShape.SQUARE, destination, new Point2D(680, 320), true));

        level.getSystems().addAll(Arrays.asList(source, destination, normalSystem1, normalSystem2,
                spySystem1, spySystem2, saboteur, vpnSystem));

        // Debug: Print port counts to verify the fix
        int totalInputPorts = 0;
        int totalOutputPorts = 0;
        for (model.System system : level.getSystems()) {
            totalInputPorts += system.getInputPorts().size();
            totalOutputPorts += system.getOutputPorts().size();
        }
        java.lang.System.out.println("DEBUG: Level 3 created with " + totalInputPorts + " input ports and " + totalOutputPorts + " output ports (Total: " + (totalInputPorts + totalOutputPorts) + ")");

        // Early injections for immediate visual activity
        level.getPacketSchedule().add(new PacketInjection(0.0, PacketType.SMALL_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(1.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(2.5, PacketType.SQUARE_MESSENGER, source));

        // Enhanced packet schedule with trojans and protected packets (18 packets total)
        level.getPacketSchedule().add(new PacketInjection(5.0, PacketType.SMALL_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(10.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(15.0, PacketType.SQUARE_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(20.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(25.0, PacketType.TRIANGLE_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(30.0, PacketType.SMALL_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(35.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(40.0, PacketType.SQUARE_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(45.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(50.0, PacketType.TRIANGLE_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(55.0, PacketType.SMALL_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(60.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(65.0, PacketType.SQUARE_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(70.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(75.0, PacketType.TRIANGLE_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(80.0, PacketType.SMALL_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(85.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(90.0, PacketType.SQUARE_MESSENGER, source));

        java.lang.System.out.println("DEBUG: Hardcoded Level 3 created with " + level.getPacketSchedule().size() + " packet injections");

        return level;
    }

    private GameLevel createLevel4() {
        // Expert Level - Add anti-trojan systems and bulk packets
        GameLevel level = new GameLevel();
        level.setLevelId("level4");
        level.setName("Expert - Anti-Trojan & Bulk");
        level.setInitialWireLength(3900.0);
        level.setDuration(150.0);

        // Build on previous levels
        ReferenceSystem source = new ReferenceSystem(new Point2D(100, 300), true);

        ReferenceSystem destination = new ReferenceSystem(new Point2D(700, 300), false);

        model.System normalSystem1 = new NormalSystem(new Point2D(400, 200), SystemType.NORMAL);
        model.System normalSystem2 = new NormalSystem(new Point2D(400, 400), SystemType.NORMAL);
        SpySystem spySystem1 = new SpySystem(new Point2D(300, 150));
        SpySystem spySystem2 = new SpySystem(new Point2D(500, 450));
        SaboteurSystem saboteur = new SaboteurSystem(new Point2D(600, 200));
        VPNSystem vpnSystem = new VPNSystem(new Point2D(200, 450));

        // Add new systems
        AntiTrojanSystem antiTrojan = new AntiTrojanSystem(new Point2D(350, 350));
        DistributorSystem distributor = new DistributorSystem(new Point2D(150, 200));
        MergerSystem merger = new MergerSystem(new Point2D(650, 400));

        // Add ports
        source.addOutputPort(new Port(PortShape.SQUARE, source, new Point2D(120, 300), false));
        source.addOutputPort(new Port(PortShape.TRIANGLE, source, new Point2D(120, 320), false));

        distributor.addInputPort(new Port(PortShape.SQUARE, distributor, new Point2D(130, 200), true));
        distributor.addOutputPort(new Port(PortShape.TRIANGLE, distributor, new Point2D(170, 200), false));

        vpnSystem.addInputPort(new Port(PortShape.SQUARE, vpnSystem, new Point2D(180, 450), true));
        vpnSystem.addOutputPort(new Port(PortShape.TRIANGLE, vpnSystem, new Point2D(220, 450), false));

        spySystem1.addInputPort(new Port(PortShape.SQUARE, spySystem1, new Point2D(280, 150), true));
        spySystem1.addOutputPort(new Port(PortShape.TRIANGLE, spySystem1, new Point2D(320, 150), false));

        normalSystem1.addInputPort(new Port(PortShape.SQUARE, normalSystem1, new Point2D(380, 200), true));
        normalSystem1.addOutputPort(new Port(PortShape.TRIANGLE, normalSystem1, new Point2D(420, 200), false));

        antiTrojan.addInputPort(new Port(PortShape.SQUARE, antiTrojan, new Point2D(330, 350), true));
        antiTrojan.addOutputPort(new Port(PortShape.TRIANGLE, antiTrojan, new Point2D(370, 350), false));

        saboteur.addInputPort(new Port(PortShape.TRIANGLE, saboteur, new Point2D(580, 200), true));
        saboteur.addOutputPort(new Port(PortShape.SQUARE, saboteur, new Point2D(620, 200), false));

        normalSystem2.addInputPort(new Port(PortShape.TRIANGLE, normalSystem2, new Point2D(380, 400), true));
        normalSystem2.addOutputPort(new Port(PortShape.SQUARE, normalSystem2, new Point2D(420, 400), false));

        spySystem2.addInputPort(new Port(PortShape.TRIANGLE, spySystem2, new Point2D(480, 450), true));
        spySystem2.addOutputPort(new Port(PortShape.SQUARE, spySystem2, new Point2D(520, 450), false));

        merger.addInputPort(new Port(PortShape.SQUARE, merger, new Point2D(630, 400), true));
        merger.addOutputPort(new Port(PortShape.TRIANGLE, merger, new Point2D(670, 400), false));

        destination.addInputPort(new Port(PortShape.TRIANGLE, destination, new Point2D(680, 300), true));
        destination.addInputPort(new Port(PortShape.SQUARE, destination, new Point2D(680, 320), true));
        // Additional input port to balance the level (fixes the 21-port issue)
        destination.addInputPort(new Port(PortShape.SQUARE, destination, new Point2D(680, 340), true));

        level.getSystems().addAll(Arrays.asList(source, destination, normalSystem1, normalSystem2,
                spySystem1, spySystem2, saboteur, vpnSystem,
                antiTrojan, distributor, merger));

        // Debug: Print port counts to verify the fix
        int totalInputPorts = 0;
        int totalOutputPorts = 0;
        for (model.System system : level.getSystems()) {
            totalInputPorts += system.getInputPorts().size();
            totalOutputPorts += system.getOutputPorts().size();
        }
        java.lang.System.out.println("DEBUG: Level 4 created with " + totalInputPorts + " input ports and " + totalOutputPorts + " output ports (Total: " + (totalInputPorts + totalOutputPorts) + ")");

        // Early injections for immediate visual activity
        level.getPacketSchedule().add(new PacketInjection(0.0, PacketType.SMALL_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(1.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(2.5, PacketType.SQUARE_MESSENGER, source));

        // Enhanced packet schedule with bulk packets (20 packets total)
        level.getPacketSchedule().add(new PacketInjection(5.0, PacketType.SMALL_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(10.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(15.0, PacketType.BULK_SMALL, source));
        level.getPacketSchedule().add(new PacketInjection(20.0, PacketType.SQUARE_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(25.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(30.0, PacketType.BULK_LARGE, source));
        level.getPacketSchedule().add(new PacketInjection(35.0, PacketType.SMALL_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(40.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(45.0, PacketType.BULK_SMALL, source));
        level.getPacketSchedule().add(new PacketInjection(50.0, PacketType.SQUARE_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(55.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(60.0, PacketType.BULK_LARGE, source));
        level.getPacketSchedule().add(new PacketInjection(65.0, PacketType.SMALL_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(70.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(75.0, PacketType.BULK_SMALL, source));
        level.getPacketSchedule().add(new PacketInjection(80.0, PacketType.SQUARE_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(85.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(90.0, PacketType.BULK_LARGE, source));
        level.getPacketSchedule().add(new PacketInjection(95.0, PacketType.SMALL_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(100.0, PacketType.CONFIDENTIAL, source));

        java.lang.System.out.println("DEBUG: Hardcoded Level 4 created with " + level.getPacketSchedule().size() + " packet injections");

        return level;
    }

    private GameLevel createLevel5() {
        // Master Level - All systems and complex packet types
        GameLevel level = new GameLevel();
        level.setLevelId("level5");
        level.setName("Master - Complete Network");
        level.setInitialWireLength(4500.0);
        level.setDuration(180.0);

        // Build on all previous levels
        ReferenceSystem source = new ReferenceSystem(new Point2D(100, 300), true);

        ReferenceSystem destination = new ReferenceSystem(new Point2D(700, 300), false);

        model.System normalSystem1 = new NormalSystem(new Point2D(400, 200), SystemType.NORMAL);
        model.System normalSystem2 = new NormalSystem(new Point2D(400, 400), SystemType.NORMAL);
        SpySystem spySystem1 = new SpySystem(new Point2D(300, 150));
        SpySystem spySystem2 = new SpySystem(new Point2D(500, 450));
        SaboteurSystem saboteur = new SaboteurSystem(new Point2D(600, 200));
        VPNSystem vpnSystem = new VPNSystem(new Point2D(200, 450));
        AntiTrojanSystem antiTrojan = new AntiTrojanSystem(new Point2D(350, 350));
        DistributorSystem distributor = new DistributorSystem(new Point2D(150, 200));
        MergerSystem merger = new MergerSystem(new Point2D(650, 400));

        // Early injections for immediate visual activity
        level.getPacketSchedule().add(new PacketInjection(0.0, PacketType.SMALL_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(1.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(2.5, PacketType.SQUARE_MESSENGER, source));

        // Add additional systems for complexity
        model.System normalSystem3 = new NormalSystem(new Point2D(250, 250), SystemType.NORMAL);
        model.System normalSystem4 = new NormalSystem(new Point2D(550, 350), SystemType.NORMAL);
        VPNSystem vpnSystem2 = new VPNSystem(new Point2D(450, 100));
        AntiTrojanSystem antiTrojan2 = new AntiTrojanSystem(new Point2D(300, 500));

        // Add all ports
        source.addOutputPort(new Port(PortShape.SQUARE, source, new Point2D(120, 300), false));
        source.addOutputPort(new Port(PortShape.TRIANGLE, source, new Point2D(120, 320), false));

        distributor.addInputPort(new Port(PortShape.SQUARE, distributor, new Point2D(130, 200), true));
        distributor.addOutputPort(new Port(PortShape.TRIANGLE, distributor, new Point2D(170, 200), false));

        vpnSystem.addInputPort(new Port(PortShape.SQUARE, vpnSystem, new Point2D(180, 450), true));
        vpnSystem.addOutputPort(new Port(PortShape.TRIANGLE, vpnSystem, new Point2D(220, 450), false));

        vpnSystem2.addInputPort(new Port(PortShape.SQUARE, vpnSystem2, new Point2D(430, 100), true));
        vpnSystem2.addOutputPort(new Port(PortShape.TRIANGLE, vpnSystem2, new Point2D(470, 100), false));

        spySystem1.addInputPort(new Port(PortShape.SQUARE, spySystem1, new Point2D(280, 150), true));
        spySystem1.addOutputPort(new Port(PortShape.TRIANGLE, spySystem1, new Point2D(320, 150), false));

        normalSystem3.addInputPort(new Port(PortShape.SQUARE, normalSystem3, new Point2D(230, 250), true));
        normalSystem3.addOutputPort(new Port(PortShape.TRIANGLE, normalSystem3, new Point2D(270, 250), false));

        normalSystem1.addInputPort(new Port(PortShape.SQUARE, normalSystem1, new Point2D(380, 200), true));
        normalSystem1.addOutputPort(new Port(PortShape.TRIANGLE, normalSystem1, new Point2D(420, 200), false));

        antiTrojan.addInputPort(new Port(PortShape.SQUARE, antiTrojan, new Point2D(330, 350), true));
        antiTrojan.addOutputPort(new Port(PortShape.TRIANGLE, antiTrojan, new Point2D(370, 350), false));

        antiTrojan2.addInputPort(new Port(PortShape.SQUARE, antiTrojan2, new Point2D(280, 500), true));
        antiTrojan2.addOutputPort(new Port(PortShape.TRIANGLE, antiTrojan2, new Point2D(320, 500), false));

        saboteur.addInputPort(new Port(PortShape.TRIANGLE, saboteur, new Point2D(580, 200), true));
        saboteur.addOutputPort(new Port(PortShape.SQUARE, saboteur, new Point2D(620, 200), false));

        normalSystem4.addInputPort(new Port(PortShape.TRIANGLE, normalSystem4, new Point2D(530, 350), true));
        normalSystem4.addOutputPort(new Port(PortShape.SQUARE, normalSystem4, new Point2D(570, 350), false));

        normalSystem2.addInputPort(new Port(PortShape.TRIANGLE, normalSystem2, new Point2D(380, 400), true));
        normalSystem2.addOutputPort(new Port(PortShape.SQUARE, normalSystem2, new Point2D(420, 400), false));

        spySystem2.addInputPort(new Port(PortShape.TRIANGLE, spySystem2, new Point2D(480, 450), true));
        spySystem2.addOutputPort(new Port(PortShape.SQUARE, spySystem2, new Point2D(520, 450), false));

        merger.addInputPort(new Port(PortShape.SQUARE, merger, new Point2D(630, 400), true));
        merger.addOutputPort(new Port(PortShape.TRIANGLE, merger, new Point2D(670, 400), false));

        destination.addInputPort(new Port(PortShape.TRIANGLE, destination, new Point2D(680, 300), true));
        destination.addInputPort(new Port(PortShape.SQUARE, destination, new Point2D(680, 320), true));

        level.getSystems().addAll(Arrays.asList(source, destination, normalSystem1, normalSystem2, normalSystem3, normalSystem4,
                spySystem1, spySystem2, saboteur, vpnSystem, vpnSystem2,
                antiTrojan, antiTrojan2, distributor, merger));

        // Debug: Print port counts to verify the fix
        int totalInputPorts = 0;
        int totalOutputPorts = 0;
        for (model.System system : level.getSystems()) {
            totalInputPorts += system.getInputPorts().size();
            totalOutputPorts += system.getOutputPorts().size();
        }
        java.lang.System.out.println("DEBUG: Level 5 created with " + totalInputPorts + " input ports and " + totalOutputPorts + " output ports (Total: " + (totalInputPorts + totalOutputPorts) + ")");

        // Complex packet schedule with all packet types (25 packets total)
        level.getPacketSchedule().add(new PacketInjection(5.0, PacketType.SMALL_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(10.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(15.0, PacketType.BULK_SMALL, source));
        level.getPacketSchedule().add(new PacketInjection(20.0, PacketType.SQUARE_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(25.0, PacketType.CONFIDENTIAL_PROTECTED, source));
        level.getPacketSchedule().add(new PacketInjection(30.0, PacketType.BULK_LARGE, source));
        level.getPacketSchedule().add(new PacketInjection(35.0, PacketType.TRIANGLE_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(40.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(45.0, PacketType.SMALL_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(50.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(55.0, PacketType.BULK_SMALL, source));
        level.getPacketSchedule().add(new PacketInjection(60.0, PacketType.SQUARE_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(65.0, PacketType.CONFIDENTIAL_PROTECTED, source));
        level.getPacketSchedule().add(new PacketInjection(70.0, PacketType.BULK_LARGE, source));
        level.getPacketSchedule().add(new PacketInjection(75.0, PacketType.TRIANGLE_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(80.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(85.0, PacketType.SMALL_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(90.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(95.0, PacketType.BULK_SMALL, source));
        level.getPacketSchedule().add(new PacketInjection(100.0, PacketType.SQUARE_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(105.0, PacketType.CONFIDENTIAL_PROTECTED, source));
        level.getPacketSchedule().add(new PacketInjection(110.0, PacketType.BULK_LARGE, source));
        level.getPacketSchedule().add(new PacketInjection(115.0, PacketType.TRIANGLE_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(120.0, PacketType.CONFIDENTIAL, source));
        level.getPacketSchedule().add(new PacketInjection(125.0, PacketType.SMALL_MESSENGER, source));
        level.getPacketSchedule().add(new PacketInjection(130.0, PacketType.CONFIDENTIAL, source));

        java.lang.System.out.println("DEBUG: Hardcoded Level 5 created with " + level.getPacketSchedule().size() + " packet injections");

        return level;
    }

    /**
     * Starts the game. Ensures a level is loaded before starting.
     */
    public void startGame() {
        // Ensure a level is loaded before starting the game
        if (gameState.getCurrentLevel() == null) {
            java.lang.System.out.println("DEBUG: No level loaded, loading level1 by default");
            loadLevel("level1");
        }

        // If schedule looks wrong (e.g., fallback hardcoded 3 injections), try to reload from JSON
        if (gameState.getCurrentLevel() != null && gameState.getCurrentLevel().getPacketSchedule() != null) {
            int sz = gameState.getCurrentLevel().getPacketSchedule().size();
            if (sz == 3) {
                java.lang.System.out.println("DEBUG: Detected 3-packet schedule; forcing JSON reload of level1");
                loadLevel("level1");
            }
        }

        // Re-enable auto-save when starting/resuming game
        saveManager.setAutoSaveEnabled(true);

        // Set the current level in the game view
        if (gameState.getCurrentLevel() != null) {
            gameView.setLevel(gameState.getCurrentLevel());
        }

        // Ensure we start in editing mode (not simulation mode)
        enterEditingMode();

        // Start the editing render loop for visual updates only (no time progression)
        editingRenderLoop.start();
        isEditingRenderLoopRunning = true;

        // Request focus for the game view to enable keyboard input
        Platform.runLater(() -> {
            gameView.requestFocus();
            // Force focus after a short delay to ensure the scene is fully loaded
            Platform.runLater(() -> {
                gameView.requestFocus();
                java.lang.System.out.println("Game started in EDITING MODE - Press R to start simulation");
            });
        });
    }

    /**
     * Pauses the game.
     */
    public void pauseGame() {
        gameState.setPaused(true);
        soundManager.pauseBackgroundMusic();
    }

    /**
     * Resumes the game.
     */
    public void resumeGame() {
        gameState.setPaused(false);
        lastUpdateTime = java.lang.System.nanoTime(); // Reset time to prevent delta time jump
        soundManager.resumeBackgroundMusic();
    }

    /**
     * Stops the game.
     */
    public void stopGame() {
        isRunning = false;
        gameLoop.stop();
        editingRenderLoop.stop();
        isEditingRenderLoopRunning = false;
        soundManager.stopBackgroundMusic();
    }

    /**
     * Gets the game view.
     */
    public GameView getGameView() {
        return gameView;
    }

    /**
     * Gets the HUD view.
     */
    public HUDView getHudView() {
        return hudView;
    }

    /**
     * Gets the level select view.
     */
    public LevelSelectView getLevelSelectView() {
        return levelSelectView;
    }

    /**
     * Gets the settings view.
     */
    public SettingsView getSettingsView() {
        return settingsView;
    }

    /**
     * Gets the game over view.
     */
    public GameOverView getGameOverView() {
        return gameOverView;
    }

    /**
     * Gets the shop view.
     */
    public ShopView getShopView() {
        return shopView;
    }

    /**
     * Gets the level complete view.
     */
    public LevelCompleteView getLevelCompleteView() {
        return levelCompleteView;
    }

    /**
     * Gets the pause view.
     */
    public view.PauseView getPauseView() {
        return pauseView;
    }

    /**
     * Gets the input handler.
     */
    public InputHandler getInputHandler() {
        return inputHandler;
    }

    /**
     * Gets the game state.
     */
    public GameState getGameState() {
        return gameState;
    }

    /**
     * Gets the sound manager.
     */
    public SoundManager getSoundManager() {
        return soundManager;
    }

    /**
     * Gets the wiring controller.
     */
    public WiringController getWiringController() {
        return wiringController;
    }

    /**
     * Gets the save manager.
     */
    public GameSaveManager getSaveManager() {
        return saveManager;
    }

    /**
     * Enters editing mode - allows wiring and bend creation.
     */
    public void enterEditingMode() {
        isEditingMode = true;
        isSimulationMode = false;
        isRunning = false;

        // Stop main simulation game loop
        gameLoop.stop();

        // Start editing render loop for visual updates only
        if (!isEditingRenderLoopRunning) {
            editingRenderLoop.start();
            isEditingRenderLoopRunning = true;
        }

        soundManager.pauseBackgroundMusic();

        // Reset temporal progress and level timer to 0 for editing
        gameState.setTemporalProgress(0.0);
        gameState.setLevelTimer(0.0);
        gameState.setPaused(false);

        // Clear active packets for fresh start
        gameState.setActivePackets(new ArrayList<>());

        // Reset packet injection schedule
        if (gameState.getCurrentLevel() != null) {
            for (PacketInjection injection : gameState.getCurrentLevel().getPacketSchedule()) {
                injection.setExecuted(false);
            }
        }

        java.lang.System.out.println("Entered EDITING MODE - You can now edit wiring and bends");
    }

    /**
     * Enters simulation mode - starts packet movement and temporal progression.
     */
    public void enterSimulationMode() {
        java.lang.System.out.println("DEBUG: Attempting to enter simulation mode...");

        // Relaxed constraint: Only require that all ports on each system are connected
        // This allows disconnected network components as long as each system has all its ports wired
        if (wiringController != null && !wiringController.areAllPortsConnected(gameState)) {
            int[] portCounts = wiringController.getPortConnectivityCounts(gameState);
            java.lang.System.out.println("Cannot start simulation: not all ports are connected (" +
                    portCounts[0] + "/" + portCounts[1] + " ports connected). All ports must be consumed.");
            java.lang.System.out.println("DEBUG: Port connectivity check failed - need to wire all ports first");
            return;
        }

        // Additionally require reference systems to be ready (at least one source and one destination connected)
        if (!areReferenceSystemsReady()) {
            java.lang.System.out.println("Cannot start simulation: reference systems are not ready (connect a source and a destination)");
            java.lang.System.out.println("DEBUG: Reference systems check failed - need to connect source to destination");
            return;
        }

        isEditingMode = false;
        isSimulationMode = true;
        isRunning = true;
        lastUpdateTime = java.lang.System.nanoTime();

        // Stop editing render loop
        editingRenderLoop.stop();
        isEditingRenderLoopRunning = false;

        // Start the main simulation game loop
        gameLoop.start();
        soundManager.playBackgroundMusic();

        java.lang.System.out.println("Entered SIMULATION MODE - Press P to pause, arrow keys for temporal navigation");
    }

    /**
     * Checks if currently in editing mode.
     */
    public boolean isEditingMode() {
        return isEditingMode;
    }

    /**
     * Checks if currently in simulation mode.
     */
    public boolean isSimulationMode() {
        return isSimulationMode;
    }

    /**
     * Updates packet positions based on temporal progress (for temporal navigation).
     * Enhanced to properly handle rewind scenarios and maintain game state consistency.
     */
    public void updatePacketPositionsForTime(double targetTime) {
        if (!isSimulationMode) return;

        double currentTime = gameState.getTemporalProgress();
        double timeDelta = targetTime - currentTime;

        if (Math.abs(timeDelta) < 0.01) return; // No significant change

        java.lang.System.out.println("Temporal navigation: " + String.format("%.2f", currentTime) + "s -> " + String.format("%.2f", targetTime) + "s");

        if (timeDelta < 0) {
            // Rewinding: need to reset and recalculate state
            handleTemporalRewind(targetTime);
        } else {
            // Fast-forwarding: need to simulate intermediate steps
            handleTemporalFastForward(targetTime, timeDelta);
        }

        // Update temporal progress and keep level timer in sync during temporal navigation
        gameState.setTemporalProgress(targetTime);
        gameState.setLevelTimer(targetTime);

        // Update visual display to show temporal navigation changes
        Platform.runLater(() -> {
            gameView.update();
            hudView.update();
        });
    }

    /**
     * Handles temporal rewind by resetting game state and recalculating up to target time.
     */
    private void handleTemporalRewind(double targetTime) {
        java.lang.System.out.println("Handling temporal rewind to " + String.format("%.2f", targetTime) + "s");

        // Reset all packet injection states
        resetPacketInjectionStates();

        // Clear all active packets
        gameState.clearActivePackets();

        // Clear packets from all wires
        clearPacketsFromWires();

        // Clear packets from all system ports and storage
        clearPacketsFromSystems();

        // Recalculate state from time 0 to target time
        simulateToTime(targetTime);
    }

    /**
     * Handles temporal fast-forward by simulating intermediate steps.
     */
    private void handleTemporalFastForward(double targetTime, double timeDelta) {
        java.lang.System.out.println("Handling temporal fast-forward by " + String.format("%.2f", timeDelta) + "s");

        // Simulate movement in smaller time steps for accuracy
        double timeStep = 0.1; // 100ms steps
        double remainingTime = timeDelta;

        while (remainingTime > 0.001) {
            double stepSize = Math.min(timeStep, remainingTime);

            // Update packet positions
            // Check smooth curve setting and pass it to MovementController
            boolean useSmoothCurves = true; // Default to smooth curves
            Object setting = gameState.getGameSettings().get("smoothWireCurves");
            if (setting instanceof Boolean) {
                useSmoothCurves = (Boolean) setting;
            }
            movementController.updatePackets(gameState.getActivePackets(), stepSize, useSmoothCurves);

            // Update temporal progress temporarily for injection processing
            double currentTemporalTime = gameState.getTemporalProgress() + (timeDelta - remainingTime) + stepSize;

            // Process any packet injections for this time step
            processPacketInjectionsForTime(currentTemporalTime);

            // Process other game mechanics for this step
            updateWirePacketMovement(stepSize);
            processWireConnections();
            processSystemTransfers();

            remainingTime -= stepSize;
        }
    }

    /**
     * Resets all packet injection states so they can be re-injected during rewind.
     */
    private void resetPacketInjectionStates() {
        if (gameState.getCurrentLevel() == null) return;

        for (PacketInjection injection : gameState.getCurrentLevel().getPacketSchedule()) {
            injection.setExecuted(false);
        }
        java.lang.System.out.println("Reset " + gameState.getCurrentLevel().getPacketSchedule().size() + " packet injection states");
    }

    /**
     * Clears all packets from wire connections.
     */
    private void clearPacketsFromWires() {
        if (gameState.getCurrentLevel() == null) return;

        int clearedCount = 0;
        for (WireConnection connection : gameState.getCurrentLevel().getWireConnections()) {
            clearedCount += connection.getPacketsOnWire().size();
            connection.clearPackets();
        }
        java.lang.System.out.println("Cleared " + clearedCount + " packets from wires");
    }

    /**
     * Clears all packets from system ports and storage.
     */
    private void clearPacketsFromSystems() {
        if (gameState.getCurrentLevel() == null) return;

        int clearedCount = 0;
        for (model.System system : gameState.getCurrentLevel().getSystems()) {
            // Clear input ports
            for (Port port : system.getInputPorts()) {
                if (port.getCurrentPacket() != null) {
                    clearedCount++;
                    port.releasePacket();
                }
            }

            // Clear output ports
            for (Port port : system.getOutputPorts()) {
                if (port.getCurrentPacket() != null) {
                    clearedCount++;
                    port.releasePacket();
                }
            }

            // Clear system storage
            clearedCount += system.getStorage().size();
            system.clearStorage();
        }
        java.lang.System.out.println("Cleared " + clearedCount + " packets from systems");
    }

    /**
     * Simulates the game from time 0 to the specified target time.
     */
    private void simulateToTime(double targetTime) {
        java.lang.System.out.println("Simulating from 0.0s to " + String.format("%.2f", targetTime) + "s");

        double timeStep = 0.1; // 100ms simulation steps
        double currentSimTime = 0.0;

        while (currentSimTime < targetTime) {
            double stepSize = Math.min(timeStep, targetTime - currentSimTime);

            // Process packet injections for this time
            processPacketInjectionsForTime(currentSimTime + stepSize);

            // Update packet movement
            // Check smooth curve setting and pass it to MovementController
            boolean useSmoothCurves = true; // Default to smooth curves
            Object setting = gameState.getGameSettings().get("smoothWireCurves");
            if (setting instanceof Boolean) {
                useSmoothCurves = (Boolean) setting;
            }
            movementController.updatePackets(gameState.getActivePackets(), stepSize, useSmoothCurves);

            // Process game mechanics
            updateWirePacketMovement(stepSize);
            processWireConnections();
            processSystemTransfers();

            currentSimTime += stepSize;
        }

        java.lang.System.out.println("Simulation complete. Active packets: " + gameState.getActivePackets().size());
    }

    /**
     * Processes packet injections for a specific time (for temporal navigation).
     * Enhanced to work with the new temporal navigation system.
     */
    private void processPacketInjectionsForTime(double targetTime) {
        if (gameState.getCurrentLevel() == null) return;

        // Gate temporal injections consistently with normal simulation:
        // only require sources and destinations to be ready
        if (!areReferenceSystemsReady()) {
            return;
        }

        for (PacketInjection injection : gameState.getCurrentLevel().getPacketSchedule()) {
            if (!injection.isExecuted() && injection.getTime() <= targetTime) {
                // Create packet instance to attempt placement
                Packet packet = injection.createPacket();

                // Try to place on outgoing wire immediately for temporal jumps
                boolean placed = tryPlacePacketOnOutgoingWire(packet, injection.getSourceSystem());

                if (placed) {
                    gameState.addActivePacket(packet);
                    injection.setExecuted(true);
                    java.lang.System.out.println("Temporal injection: " + packet.getClass().getSimpleName() +
                            " at " + String.format("%.2f", injection.getTime()) + "s (placed on wire)");
                } else {
                    java.lang.System.out.println("Temporal injection deferred: no available wire for " + packet.getClass().getSimpleName());
                }
            }
        }
    }

    /**
     * Gets all packets currently on wires for collision detection.
     */
    private List<Packet> getPacketsOnWires() {
        List<Packet> wirePackets = new ArrayList<>();
        if (gameState.getCurrentLevel() == null) return wirePackets;

        for (WireConnection connection : gameState.getCurrentLevel().getWireConnections()) {
            if (connection.isActive()) {
                wirePackets.addAll(connection.getPacketsOnWire());
            }
        }
        return wirePackets;
    }

    /**
     * Removes destroyed packets from all wire connections to free up wire space.
     */
    private void removeDestroyedPacketsFromWires(List<Packet> packetsToRemove) {
        if (gameState.getCurrentLevel() == null || packetsToRemove.isEmpty()) return;

        int totalRemoved = 0;
        for (WireConnection connection : gameState.getCurrentLevel().getWireConnections()) {
            if (connection.isActive()) {
                int beforeCount = connection.getPacketsOnWire().size();
                // Remove destroyed packets from this wire
                connection.getPacketsOnWire().removeAll(packetsToRemove);
                int afterCount = connection.getPacketsOnWire().size();
                int removedFromWire = beforeCount - afterCount;
                totalRemoved += removedFromWire;
                
                if (removedFromWire > 0) {
                    System.out.println("DEBUG: Removed " + removedFromWire + " destroyed packets from wire " + 
                                     connection.getId().substring(0, 8) + " (remaining: " + afterCount + ")");
                }
            }
        }
        
        if (totalRemoved > 0) {
            System.out.println("DEBUG: Total " + totalRemoved + " destroyed packets removed from wires - wires now free for new packets");
        }
    }

    /**
     * Immediately removes destroyed packets from wires after collision detection.
     * This ensures wires are freed up immediately when packets are destroyed by noise.
     */
    private void removeDestroyedPacketsFromWiresImmediate() {
        if (gameState.getCurrentLevel() == null) return;

        int totalRemoved = 0;
        List<Packet> destroyedPackets = new ArrayList<>();
        
        for (WireConnection connection : gameState.getCurrentLevel().getWireConnections()) {
            if (connection.isActive()) {
                List<Packet> packetsOnWire = connection.getPacketsOnWire();
                int beforeCount = packetsOnWire.size();
                
                // First, mark destroyed packets as inactive and collect them
                for (Packet packet : packetsOnWire) {
                    if (packet.shouldBeLost() && packet.isActive()) {
                        packet.setActive(false);
                        destroyedPackets.add(packet);
                        System.out.println("DEBUG: IMMEDIATE - Marked packet " + packet.getPacketType() + 
                                         " as inactive due to noise damage (noise: " + packet.getNoiseLevel() + 
                                         ", size: " + packet.getSize() + ")");
                    }
                }
                
                // Then remove inactive packets from wire
                packetsOnWire.removeIf(packet -> !packet.isActive());
                
                int afterCount = packetsOnWire.size();
                int removedFromWire = beforeCount - afterCount;
                totalRemoved += removedFromWire;
                
                if (removedFromWire > 0) {
                    System.out.println("DEBUG: IMMEDIATE - Removed " + removedFromWire + " destroyed packets from wire " + 
                                     connection.getId().substring(0, 8) + " (remaining: " + afterCount + ")");
                }
            }
        }
        
        // Also remove destroyed packets from active packets list and count them as lost
        if (!destroyedPackets.isEmpty()) {
            for (Packet packet : destroyedPackets) {
                gameState.incrementLostPackets();
            }
            gameState.getActivePackets().removeAll(destroyedPackets);
            System.out.println("DEBUG: IMMEDIATE - Removed " + destroyedPackets.size() + " destroyed packets from active list and counted as lost");
        }
        
        if (totalRemoved > 0) {
            System.out.println("DEBUG: IMMEDIATE - Total " + totalRemoved + " destroyed packets removed from wires - wires now free!");
        }
    }

    /**
     * Processes system transfers - moves packets from input ports into systems and from systems to next wires.
     * This is the key missing piece that aligns with the specifications.
     */
    private void processSystemTransfers() {
        if (gameState.getCurrentLevel() == null) return;

        for (model.System system : gameState.getCurrentLevel().getSystems()) {
            if (!system.isActive()) continue;

            // Process packets from storage to output ports (when ports become available)
            processStorageToOutputs(system);

            // Also push any packets currently sitting on output ports onto their outgoing wires
            for (Port outputPort : system.getOutputPorts()) {
                if (outputPort.getCurrentPacket() != null) {
                    // Attempt immediate transfer to the connected wire
                    boolean moved = tryTransferPortPacketToWire(outputPort);
                    if (moved) {
                        java.lang.System.out.println("DEBUG: Packet pushed from output port to wire for system " +
                                system.getClass().getSimpleName());
                    }
                }
            }
        }
    }

    /**
     * Processes packets from system storage to available output ports and then to wires.
     * Implements the specification requirement: "Transfers packets to compatible, empty output ports"
     */
    private void processStorageToOutputs(model.System system) {
        if (system.getStorage().isEmpty()) return;

        List<Packet> storage = new ArrayList<>(system.getStorage());
        for (Packet packet : storage) {
            if (!packet.isActive()) {
                system.getStorage().remove(packet);
                continue;
            }

            // Find available compatible output port with available wire
            Port availablePort = findAvailableOutputPortWithWire(system, packet);
            if (availablePort != null) {
                // Remove from storage and place on output port
                system.getStorage().remove(packet);
                availablePort.acceptPacket(packet);

                // Apply exit speed doubling if packet is exiting through incompatible port
                boolean isCompatible = availablePort.isCompatibleWithPacket(packet);
                if (!isCompatible && packet instanceof MessengerPacket) {
                    ((MessengerPacket) packet).applyExitSpeedMultiplier(true);
                    java.lang.System.out.println("DEBUG: Applied 2x exit speed for storage->port incompatible exit");
                } else if (!isCompatible && packet instanceof ProtectedPacket) {
                    ((ProtectedPacket) packet).applyExitSpeedMultiplier(true);
                    java.lang.System.out.println("DEBUG: Applied 2x exit speed for storage->port protected packet incompatible exit");
                }

                // Try to immediately transfer to wire (if wire is available)
                tryTransferPortPacketToWire(availablePort);

                java.lang.System.out.println("DEBUG: Packet transferred from storage to " + 
                        (isCompatible ? "compatible" : "incompatible") + " output port in " +
                        system.getClass().getSimpleName());

                // Only process one packet per update cycle to prevent overwhelming
                break;
            }
        }
    }

    /**
     * Finds an available output port that has a connected wire with capacity.
     * This aligns with Phase 1 spec: "Transfers packets to compatible, empty output ports"
     */
    private Port findAvailableOutputPortWithWire(model.System system, Packet packet) {
        // First try to find compatible ports
        for (Port outputPort : system.getOutputPorts()) {
            if (outputPort.isEmpty() && outputPort.isCompatibleWithPacket(packet)) {
                if (hasAvailableOutgoingWire(outputPort)) {
                    return outputPort;
                }
            }
        }

        // If no compatible ports available, try any empty port (as per spec: "else stores them")
        for (Port outputPort : system.getOutputPorts()) {
            if (outputPort.isEmpty() && hasAvailableOutgoingWire(outputPort)) {
                return outputPort;
            }
        }

        return null;
    }

    /**
     * Checks if a port has an available outgoing wire connection.
     */
    private boolean hasAvailableOutgoingWire(Port port) {
        if (gameState.getCurrentLevel() == null) return false;

        for (WireConnection connection : gameState.getCurrentLevel().getWireConnections()) {
            if (!connection.isActive()) continue;

            // Check if this port is the source of an outgoing connection
            if (connection.getSourcePort() == port && connection.canAcceptPacket()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Attempts to transfer a packet from a port to its connected wire.
     */
    private boolean tryTransferPortPacketToWire(Port port) {
        if (port.getCurrentPacket() == null) return false;
        if (gameState.getCurrentLevel() == null) return false;

        // Find the wire connection starting from this port
        for (WireConnection connection : gameState.getCurrentLevel().getWireConnections()) {
            if (!connection.isActive()) continue;

            if (connection.getSourcePort() == port && connection.canAcceptPacket()) {
                Packet packet = port.releasePacket();
                boolean accepted = connection.acceptPacket(packet);
                if (accepted) {
                    java.lang.System.out.println("DEBUG: Packet transferred from output port to wire " +
                            connection.getId());
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Attempts to place a newly injected packet onto a connected outgoing wire from the given source system.
     */
    private boolean tryPlacePacketOnOutgoingWire(Packet packet, model.System sourceSystem) {
        if (sourceSystem == null || gameState.getCurrentLevel() == null) return false;

        // First try compatible connected ports, then any connected port
        List<Port> compatibleConnectedPorts = new ArrayList<>();
        List<Port> anyConnectedPorts = new ArrayList<>();
        
        for (Port out : sourceSystem.getOutputPorts()) {
            if (!out.isConnected()) continue;
            
            if (out.isCompatibleWithPacket(packet)) {
                compatibleConnectedPorts.add(out);
            } else {
                anyConnectedPorts.add(out);
            }
        }
        
        // Try compatible ports first, then any connected ports
        List<Port> portsToTry = new ArrayList<>();
        portsToTry.addAll(compatibleConnectedPorts);
        portsToTry.addAll(anyConnectedPorts);
        
        java.lang.System.out.println("DEBUG: tryPlacePacketOnOutgoingWire for " + packet.getPacketType() + 
                " - compatible: " + compatibleConnectedPorts.size() + ", any: " + anyConnectedPorts.size());
        
        for (Port out : portsToTry) {

            // Find any wire connection involving this port, correct direction if needed
            for (WireConnection connection : gameState.getCurrentLevel().getWireConnections()) {
                if (!connection.isActive()) continue;
                if (!connection.canAcceptPacket()) continue;

                // Loose matching: identity OR equals OR near-same position
                boolean involvesPort = false;
                Port connSrc = connection.getSourcePort();
                Port connDst = connection.getDestinationPort();
                if (connSrc == out || connDst == out) {
                    involvesPort = true;
                } else if (connSrc != null && connDst != null) {
                    if (connSrc.equals(out) || connDst.equals(out)) {
                        involvesPort = true;
                    } else if (out.getPosition() != null) {
                        if (connSrc != null && connSrc.getPosition() != null &&
                                out.getPosition().distanceTo(connSrc.getPosition()) < 1.0) {
                            involvesPort = true;
                        }
                        if (!involvesPort && connDst != null && connDst.getPosition() != null &&
                                out.getPosition().distanceTo(connDst.getPosition()) < 1.0) {
                            involvesPort = true;
                        }
                    }
                }
                if (!involvesPort) continue;

                // Ensure connection direction is from this output port to the opposite input port
                if (connection.getDestinationPort() == out && connection.getSourcePort() != out) {
                    Port other = connection.getSourcePort();
                    connection.updatePortReferences(out, other);
                }

                if (connection.getSourcePort() != out) {
                    // Direction still not correct or connection malformed
                    continue;
                }

                // Initialize packet position at the port and load on the wire
                if (out.getPosition() != null) {
                    packet.setCurrentPosition(out.getPosition());
                }
                boolean accepted = connection.acceptPacket(packet);
                if (accepted) {
                    java.lang.System.out.println("Packet successfully placed on wire from " + sourceSystem.getClass().getSimpleName());
                    return true;
                }
            }
        }

        java.lang.System.out.println("Failed to place packet - no available wires from " + sourceSystem.getClass().getSimpleName());
        return false;
    }

    /**
     * Debug helper to understand why packet placement failed.
     */
    private void debugPacketPlacementFailure(model.System sourceSystem) {
        java.lang.System.out.println("DEBUG: Analyzing packet placement failure for " + sourceSystem.getClass().getSimpleName());
        java.lang.System.out.println("DEBUG: Output ports: " + sourceSystem.getOutputPorts().size());

        for (int i = 0; i < sourceSystem.getOutputPorts().size(); i++) {
            Port port = sourceSystem.getOutputPorts().get(i);
            java.lang.System.out.println("DEBUG: Port " + i + " connected: " + port.isConnected() +
                    ", shape: " + port.getShape());
        }

        java.lang.System.out.println("DEBUG: Total wire connections in level: " +
                gameState.getCurrentLevel().getWireConnections().size());

        int activeConnections = 0;
        for (WireConnection conn : gameState.getCurrentLevel().getWireConnections()) {
            if (conn.isActive()) {
                activeConnections++;
                if (conn.getSourcePort().getParentSystem() == sourceSystem) {
                    java.lang.System.out.println("DEBUG: Found connection from this system, can accept: " +
                            conn.canAcceptPacket() + ", packets on wire: " + conn.getPacketsOnWire().size());
                }
            }
        }
        java.lang.System.out.println("DEBUG: Active connections: " + activeConnections);
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public MainApp getMainApp() {
        return mainApp;
    }

    /**
     * Toggles the shop view.
     */
    public void toggleShop() {
        java.lang.System.out.println("toggleShop called");
        if (shopView != null) {
            java.lang.System.out.println("Shop is currently visible: " + shopView.isVisible());
            if (shopView.isVisible()) {
                // Hide shop and resume game
                java.lang.System.out.println("Hiding shop");
                shopView.toggleVisibility();

                // Shop overlay removal is now handled by popup in ShopView

                resumeGame();
            } else {
                // Show shop and pause game
                java.lang.System.out.println("Showing shop");
                pauseGame();
                shopView.toggleVisibility();

                // Shop overlay is now handled by popup in ShopView
                java.lang.System.out.println("Shop visibility toggled");
            }
        } else {
            java.lang.System.out.println("Shop view is null");
        }
    }

    /**
     * Activates an ability for the current level.
     */
    public boolean activateAbility(AbilityType abilityType) {
        if (abilityCooldowns.containsKey(abilityType)) {
            return false; // Still on cooldown
        }

        int cost = abilityType.getCost();
        if (gameState.getCoins() >= cost) {
            gameState.setCoins(gameState.getCoins() - cost);
            activeAbilities.add(abilityType);

            // Set cooldown based on ability type
            double cooldown = getAbilityCooldownDuration(abilityType);
            abilityCooldowns.put(abilityType, cooldown);

            return true;
        }
        return false;
    }

    /**
     * Gets the cooldown duration for an ability.
     */
    private double getAbilityCooldownDuration(AbilityType abilityType) {
        switch (abilityType) {
            case O_ATAR: return 10.0; // 10 seconds
            case O_AIRYAMAN: return 5.0; // 5 seconds
            case O_ANAHITA: return 10.0; // 10 seconds
            case SCROLL_OF_AERGIA: return 20.0; // 20 seconds
            case SCROLL_OF_SISYPHUS: return 30.0; // 30 seconds
            case SCROLL_OF_ELIPHAS: return 25.0; // 25 seconds
            default: return 10.0;
        }
    }

    /**
     * Updates ability cooldowns.
     */
    private void updateAbilityCooldowns(double deltaTime) {
        List<AbilityType> expiredCooldowns = new ArrayList<>();

        for (Map.Entry<AbilityType, Double> entry : abilityCooldowns.entrySet()) {
            double remaining = entry.getValue() - deltaTime;
            if (remaining <= 0) {
                expiredCooldowns.add(entry.getKey());
            } else {
                abilityCooldowns.put(entry.getKey(), remaining);
            }
        }

        for (AbilityType ability : expiredCooldowns) {
            abilityCooldowns.remove(ability);
        }
    }

    /**
     * Gets the list of active abilities.
     */
    public List<AbilityType> getActiveAbilities() {
        return new ArrayList<>(activeAbilities);
    }

    /**
     * Gets the remaining cooldown for an ability.
     */
    public double getAbilityCooldown(AbilityType abilityType) {
        return abilityCooldowns.getOrDefault(abilityType, 0.0);
    }

    /**
     * Checks if an ability is available (not on cooldown).
     */
    public boolean isAbilityAvailable(AbilityType abilityType) {
        return !abilityCooldowns.containsKey(abilityType);
    }

    /**
     * Checks if an ability is currently active.
     */
    public boolean isAbilityActive(AbilityType abilityType) {
        return activeAbilities.contains(abilityType);
    }

    /**
     * Applies ability effects to the game state.
     */
    public void applyAbilityEffects() {
        // Apply Phase 1 abilities
        if (isAbilityActive(AbilityType.O_ATAR)) {
            // Disable shockwaves for 10s
            // This is handled in CollisionController
        }

        if (isAbilityActive(AbilityType.O_AIRYAMAN)) {
            // Disable collisions for 5s
            // This is handled in CollisionController
        }

        if (isAbilityActive(AbilityType.O_ANAHITA)) {
            // Set all packet noise to 0
            for (Packet packet : gameState.getActivePackets()) {
                packet.setNoiseLevel(0.0);
            }
        }

        // Phase 2 abilities are now handled by AbilityManager
        if (abilityManager != null) {
            abilityManager.applyEffects(gameState.getActivePackets());
        }
    }

    /**
     * Activates a Phase 2 ability at a specific point.
     */
    public boolean activateAbilityAtPoint(AbilityType abilityType, Point2D point) {
        if (abilityManager == null) {
            return false;
        }

        return abilityManager.activateAbility(abilityType, point);
    }

    /**
     * Checks if a system can be moved (for Sisyphus ability).
     */
    public boolean canMoveSystemWithAbility(model.System system) {
        return abilityManager != null && abilityManager.canMoveSystem(system);
    }

    /**
     * Moves a system using ability (for Sisyphus).
     */
    public boolean moveSystemWithAbility(model.System system, Point2D newPosition) {
        return abilityManager != null && abilityManager.moveSystem(system, newPosition);
    }

    /**
     * Handles save file check on game start.
     */
    public boolean checkForSaveFile() {
        return saveManager.hasSaveFile();
    }

    /**
     * Loads a saved game.
     */
    public boolean loadSavedGame() {
        try {
            GameState savedState = saveManager.loadGame();
            this.gameState = savedState;
            return true;
        } catch (GameSaveManager.GameLoadException e) {
            java.lang.System.err.println("Failed to load saved game: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes the save file.
     */
    public void deleteSaveFile() {
        saveManager.deleteSaveFile();
    }

    /**
     * Enables or disables auto-save functionality.
     */
    public void setAutoSaveEnabled(boolean enabled) {
        saveManager.setAutoSaveEnabled(enabled);
    }

    /**
     * Toggles the smooth wire curves setting.
     * Only allowed in editing mode to prevent changes during simulation.
     */
    public void toggleSmoothWires() {
        if (!isEditingMode) {
            java.lang.System.out.println("Cannot change wire curve mode during simulation. Return to editing mode first.");
            return;
        }
        
        if (gameState != null) {
            Object setting = gameState.getGameSettings().get("smoothWireCurves");
            boolean currentSetting = true; // Default to true
            if (setting instanceof Boolean) {
                currentSetting = (Boolean) setting;
            }
            
            // Toggle the setting
            boolean newSetting = !currentSetting;
            gameState.getGameSettings().put("smoothWireCurves", newSetting);
            
            // Recalculate remaining wire length based on new curve mode
            if (gameState.getCurrentLevel() != null) {
                double initialWireLength = gameState.getCurrentLevel().getInitialWireLength();
                double totalUsedWire = wiringController.getTotalWireLengthUsed(gameState, newSetting);
                double newRemainingWireLength = initialWireLength - totalUsedWire;
                
                // Ensure remaining wire length doesn't go negative
                if (newRemainingWireLength < 0) {
                    newRemainingWireLength = 0;
                }
                
                gameState.setRemainingWireLength(newRemainingWireLength);
            }

            // Request view update to show the change immediately
            if (gameView != null) {
                gameView.update();
            }
        }
    }

    /**
     * Checks if smooth wire curves are enabled.
     */
    public boolean isSmoothWires() {
        if (gameState != null) {
            Object setting = gameState.getGameSettings().get("smoothWireCurves");
            if (setting instanceof Boolean) {
                return (Boolean) setting;
            }
        }
        return true; // Default to smooth curves
    }
}
