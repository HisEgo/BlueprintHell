package model;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import java.util.ArrayDeque;
import java.util.Set;
import java.util.HashSet;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Stores the game state at the beginning of a level for restart functionality.
 */
class LevelStartState {
    private final int coins;
    private final int lostPacketsCount;
    private final double remainingWireLength;
    
    public LevelStartState(int coins, int lostPacketsCount, double remainingWireLength) {
        this.coins = coins;
        this.lostPacketsCount = lostPacketsCount;
        this.remainingWireLength = remainingWireLength;
    }
    
    public int getCoins() { return coins; }
    public int getLostPacketsCount() { return lostPacketsCount; }
    public double getRemainingWireLength() { return remainingWireLength; }
}

/**
 * Represents the current state of the game.
 * Tracks wire length, temporal progress, packet loss, coins, and active packets.
 * POJO class for serialization support.
 */
public class GameState {
    private double remainingWireLength;
    private double temporalProgress;
    private double packetLoss;
    private int coins;
    private List<Packet> activePackets;
    private GameLevel currentLevel;
    private double levelTimer;
    private boolean isPaused;
    private boolean isGameOver;
    private boolean isLevelComplete;
    @JsonIgnore // avoid serializing arbitrary objects like AWT geometry causing recursion
    private Map<String, Object> gameSettings;
    private int lostPacketsCount;
    // Controls whether system indicators are displayed at all (toggled by I key)
    private boolean showSystemIndicators;
    // Tracks the most recent game over reason for UI display
    private GameOverReason lastGameOverReason;
    // Stores the state before level start for restart functionality
    @JsonIgnore
    private LevelStartState levelStartState;

    public GameState() {
        this.activePackets = new ArrayList<>();
        this.gameSettings = new HashMap<>();
        this.isPaused = false;
        this.isGameOver = false;
        this.isLevelComplete = false;
        this.levelTimer = 0.0;
        this.lostPacketsCount = 0;
        this.showSystemIndicators = true; // Indicators are always ON
        // Default settings
        this.gameSettings.put("offWireLossThreshold", 20.0);
        this.gameSettings.put("smoothWireCurves", true); // Enable smooth wire curves by default
        this.lastGameOverReason = GameOverReason.NONE;
    }

    public GameState(GameLevel level) {
        this();
        this.currentLevel = level;
        this.remainingWireLength = level.getInitialWireLength();
        this.temporalProgress = 0.0;
        this.packetLoss = 0.0;
        this.coins = 0;
        this.lostPacketsCount = 0;
        this.showSystemIndicators = true;
        this.lastGameOverReason = GameOverReason.NONE;
    }

    public double getRemainingWireLength() {
        return remainingWireLength;
    }

    public void setRemainingWireLength(double remainingWireLength) {
        this.remainingWireLength = remainingWireLength;
    }

    public double getTemporalProgress() {
        return temporalProgress;
    }

    public void setTemporalProgress(double temporalProgress) {
        this.temporalProgress = temporalProgress;
    }

    public double getPacketLoss() {
        return packetLoss;
    }

    public void setPacketLoss(double packetLoss) {
        this.packetLoss = packetLoss;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public List<Packet> getActivePackets() {
        return activePackets;
    }

    public void setActivePackets(List<Packet> activePackets) {
        this.activePackets = activePackets;
    }

    public GameLevel getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(GameLevel currentLevel) {
        this.currentLevel = currentLevel;
    }

    public double getLevelTimer() {
        return levelTimer;
    }

    public void setLevelTimer(double levelTimer) {
        this.levelTimer = levelTimer;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public void setGameOver(boolean gameOver) {
        isGameOver = gameOver;
    }

    public boolean isLevelComplete() {
        return isLevelComplete;
    }

    public void setLevelComplete(boolean levelComplete) {
        isLevelComplete = levelComplete;
    }

    public Map<String, Object> getGameSettings() {
        return gameSettings;
    }

    public void setGameSettings(Map<String, Object> gameSettings) {
        this.gameSettings = gameSettings;
    }

    /**
     * Gets a double setting with fallback.
     */
    @JsonIgnore
    public double getDoubleSetting(String key, double defaultValue) {
        Object v = gameSettings.get(key);
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        return defaultValue;
    }

    /**
     * Increments the lost packets counter.
     */
    public void incrementLostPackets() {
        this.lostPacketsCount++;
    }



    /**
     * Gets the number of lost packets counted so far.
     */
    public int getLostPacketsCount() {
        return lostPacketsCount;
    }

    /**
     * Sets the number of lost packets (for save/restore compatibility).
     */
    public void setLostPacketsCount(int lostPacketsCount) {
        this.lostPacketsCount = lostPacketsCount;
    }

    /**
     * Returns whether system indicators should be drawn.
     */
    public boolean isShowSystemIndicators() {
        return showSystemIndicators;
    }

    /**
     * Sets whether system indicators should be drawn.
     */
    public void setShowSystemIndicators(boolean showSystemIndicators) {
        this.showSystemIndicators = showSystemIndicators;
    }

    /**
     * Adds a packet to the active packets list.
     */
    public void addActivePacket(Packet packet) {
        activePackets.add(packet);
    }

    /**
     * Removes a packet from the active packets list.
     */
    public void removeActivePacket(Packet packet) {
        activePackets.remove(packet);
    }

    /**
     * Gets the number of active packets.
     */
    @JsonIgnore
    public int getActivePacketCount() {
        return activePackets.size();
    }

    /**
     * JSON serialization compatibility - adds activePacketCount property for save files.
     */
    public void setActivePacketCount(int count) {
        // This is computed from activePackets.size(), so we ignore the setter
    }

    /**
     * Consumes wire length.
     */
    public boolean consumeWireLength(double amount) {
        if (remainingWireLength >= amount) {
            remainingWireLength -= amount;
            return true;
        }
        return false;
    }

    /**
     * Adds coins to the player's balance.
     */
    public void addCoins(int amount) {
        coins += amount;
    }

    /**
     * Spends coins if the player has enough.
     */
    public boolean spendCoins(int amount) {
        if (coins >= amount) {
            coins -= amount;
            return true;
        }
        return false;
    }

    /**
     * Updates the temporal progress.
     */
    public void updateTemporalProgress(double deltaTime) {
        if (!isPaused && !isGameOver && !isLevelComplete) {
            temporalProgress += deltaTime;
            // Only increment level timer during simulation mode (not editing mode)
            // Level timer should be controlled by GameController based on current mode
        }
    }

    /**
     * Updates the level timer (should only be called during simulation mode).
     */
    public void updateLevelTimer(double deltaTime) {
        if (!isPaused && !isGameOver && !isLevelComplete) {
            levelTimer += deltaTime;
        }
    }

    /**
     * Calculates the packet loss percentage.
     */
    public double calculatePacketLossPercentage() {
        if (currentLevel == null) return 0.0;

        int totalInjected = getTotalInjectedPackets();
        int totalLost = getTotalLostPackets();

        if (totalInjected == 0) return 0.0;

        return (double) totalLost / totalInjected * 100.0;
    }

    /**
     * Gets the total number of packets injected into the level.
     */
    public int getTotalInjectedPackets() {
        if (currentLevel == null) return 0;
        // Prefer new direct packetSchedule list when present
        if (currentLevel.getPacketSchedule() != null && !currentLevel.getPacketSchedule().isEmpty()) {
            return currentLevel.getPacketSchedule().size();
        }
        // Backward compatibility: legacy map-based schedule
        int total = 0;
        if (currentLevel.getPacketInjectionSchedule() != null) {
            for (List<Packet> packets : currentLevel.getPacketInjectionSchedule().values()) {
                total += packets.size();
            }
        }
        return total;
    }

    /**
     * Gets the total number of packets lost.
     */
    public int getTotalLostPackets() {
        int lost = lostPacketsCount;
        for (Packet packet : activePackets) {
            if (!packet.isActive() || packet.shouldBeLost() || packet.shouldBeDestroyedByTime()) {
                lost++;
            }
        }
        return lost;
    }



    /**
     * Clears all active packets (for temporal navigation rewind).
     */
    public void clearActivePackets() {
        activePackets.clear();
        java.lang.System.out.println("Cleared all active packets from game state");
    }



    /**
     * Gets the total number of packets successfully delivered.
     */
    public int getTotalDeliveredPackets() {
        int delivered = 0;
        for (ReferenceSystem destSystem : currentLevel.getDestinationSystems()) {
            delivered += destSystem.getReceivedPacketCount();
        }
        return delivered;
    }

    /**
     * Checks if the game should end due to packet loss exceeding 50% or time limit exceeded.
     */
    public boolean shouldEndGame() {
        // Primary rule: excessive loss triggers Game Over
        boolean tooManyLost = calculatePacketLossPercentage() > 50.0;

        // Time limit rule: if level duration exceeded and packets are still active, Game Over
        boolean timeExceeded = false;
        if (currentLevel != null && levelTimer > currentLevel.getLevelDuration()) {
            // Only trigger Game Over if there are still active packets or ongoing simulation
            if (!activePackets.isEmpty() || levelTimer > currentLevel.getLevelDuration() + 5.0) {
                timeExceeded = true;
            }
        }

        // Phase 2 enhancement: network disconnected (no route from any source to any destination)
        boolean networkDisconnected = isNetworkTopologicallyDisconnected();

        // Phase 2 enhancement: excessive failed systems (permanent failures)
        boolean excessiveFailures = hasExcessiveFailedSystems();

        if (tooManyLost) {
            lastGameOverReason = GameOverReason.EXCESSIVE_PACKET_LOSS;
            return true;
        }
        if (timeExceeded) {
            lastGameOverReason = GameOverReason.TIME_LIMIT_EXCEEDED;
            return true;
        }
        if (networkDisconnected) {
            lastGameOverReason = GameOverReason.NETWORK_DISCONNECTED;
            return true;
        }
        if (excessiveFailures) {
            lastGameOverReason = GameOverReason.EXCESSIVE_SYSTEM_FAILURES;
            return true;
        }
        lastGameOverReason = GameOverReason.NONE;
        return false;
    }

    public GameOverReason getLastGameOverReason() {
        return lastGameOverReason;
    }

    /**
     * Checks if the level should be completed.
     */
    public boolean shouldCompleteLevel() {
        if (currentLevel == null) return false;

        // Don't complete level if simulation just started (need at least 1 second)
        if (levelTimer < 1.0) return false;

        // Treat time elapsed as a valid level completion path
        boolean timeElapsed = levelTimer >= currentLevel.getLevelDuration();

        // Debug output to understand what's happening
        if (levelTimer > 0 && (int)levelTimer % 2 == 0) { // Print every 2 seconds
            java.lang.System.out.println("DEBUG shouldCompleteLevel: levelTimer=" + levelTimer +
                    ", levelDuration=" + currentLevel.getLevelDuration() +
                    ", timeElapsed=" + timeElapsed +
                    ", activePackets.size=" + activePackets.size());
        }

        // Don't complete level if no packets have been injected yet
        boolean anyPacketsInjected = false;
        if (currentLevel.getPacketSchedule() != null && !currentLevel.getPacketSchedule().isEmpty()) {
            anyPacketsInjected = currentLevel.getPacketSchedule().stream()
                    .anyMatch(injection -> injection.isExecuted());
        }
        // Support legacy JSON schedule map as an indication that injections exist
        if (!anyPacketsInjected && currentLevel.getPacketInjectionSchedule() != null &&
                !currentLevel.getPacketInjectionSchedule().isEmpty()) {
            anyPacketsInjected = true;
        }
        // Also consider deliveries as evidence that injections occurred
        if (!anyPacketsInjected && getTotalDeliveredPackets() > 0) {
            anyPacketsInjected = true;
        }
        if (!anyPacketsInjected) return false;

        // Only consider completion if we've had some time for packets to be processed
        // This prevents instant completion when activePackets list is empty at start
        if (levelTimer < 2.0) return false;

        // Check if all packets have been processed
        // Only consider packets as processed if they've been successfully delivered or legitimately lost
        // Don't count packets that failed to be placed on wires as "processed"
        boolean allPacketsProcessed = false;

        if (activePackets.isEmpty()) {
            // Check if all scheduled packets have been successfully processed
            // A packet is considered processed if it was executed AND either:
            // 1. It's no longer active (delivered, lost, or destroyed), OR
            // 2. It was never successfully placed on a wire (failed injection)
            boolean allScheduledPacketsProcessed = currentLevel.getPacketSchedule().stream()
                    .allMatch(injection -> injection.isExecuted());
            allPacketsProcessed = allScheduledPacketsProcessed;
        } else {
            // Some packets are still active, so not all are processed
            allPacketsProcessed = false;
        }

        // Check if packet loss is acceptable
        boolean acceptableLoss = calculatePacketLossPercentage() <= 50.0;

        // Level completes when all packets processed with acceptable loss, or when time has elapsed

        // For early levels (level1, level2), wait for all packets to be injected and processed
        // This ensures accurate packet loss calculation
        String levelId = currentLevel.getLevelId();
        boolean isEarlyLevel = levelId != null && ("level1".equals(levelId) || "level2".equals(levelId));

        if (isEarlyLevel) {
            // For early levels, require all packets to be injected and processed
            // This prevents premature completion and ensures accurate packet loss calculation
            if (allPacketsProcessed && acceptableLoss) {
                // Only complete if we've had enough time for all packets to be processed
                // For level1, this should be around 50 seconds (the level duration)
                if (levelTimer >= currentLevel.getLevelDuration() * 0.5) { // Allow completion after 50% of time
                    java.lang.System.out.println("DEBUG: Early level completing after all packets processed: " +
                            "levelTimer=" + levelTimer + ", levelDuration=" + currentLevel.getLevelDuration() +
                            ", totalDelivered=" + getTotalDeliveredPackets() + ", totalLost=" + getTotalLostPackets());
                    return true;
                }
            }

            // Don't allow early completion for early levels - wait for proper packet processing
            return false;
        }

        // For later levels, allow early completion with acceptable loss
        boolean earlyCompletion = false;
        if (allPacketsProcessed && acceptableLoss && levelTimer >= 5.0) {
            int totalDelivered = getTotalDeliveredPackets();
            if (totalDelivered >= 1) {
                earlyCompletion = true;
            }
        }

        // Timer-based completion: always allow for early levels (level1, level2), regardless of loss
        if (timeElapsed) {
            if (isEarlyLevel) {
                return true;
            }
            // For later levels, still require acceptable loss on timer expiry
            if (acceptableLoss) {
                return true;
            }
        }

        return earlyCompletion;
    }

    /**
     * Resets the game state for a new level.
     */
    public void resetForLevel(GameLevel level) {
        this.currentLevel = level;
        this.remainingWireLength = level.getInitialWireLength();
        this.temporalProgress = 0.0;
        this.packetLoss = 0.0;
        this.coins = 0;
        this.activePackets.clear();
        this.levelTimer = 0.0;
        this.isPaused = false;
        this.isGameOver = false;
        this.isLevelComplete = false;
        this.lostPacketsCount = 0;
        this.showSystemIndicators = true; // Indicators are always ON
    }

    /**
     * Saves the current state as the level start state for restart functionality.
     */
    public void saveLevelStartState() {
        this.levelStartState = new LevelStartState(coins, lostPacketsCount, remainingWireLength);
        java.lang.System.out.println("DEBUG: Saved level start state - coins: " + coins + ", lost: " + lostPacketsCount + ", wireLength: " + remainingWireLength);
    }
    
    /**
     * Restores the state to what it was at the beginning of the level.
     */
    public void restoreToLevelStart() {
        if (levelStartState != null) {
            this.coins = levelStartState.getCoins();
            this.lostPacketsCount = levelStartState.getLostPacketsCount();
            this.remainingWireLength = levelStartState.getRemainingWireLength();
            java.lang.System.out.println("DEBUG: Restored level start state - coins: " + coins + ", lost: " + lostPacketsCount + ", wireLength: " + remainingWireLength);
        } else {
            java.lang.System.out.println("DEBUG: No level start state to restore");
        }
        
        // Reset other level-specific state
        this.packetLoss = 0.0;
        this.temporalProgress = 0.0;
        this.levelTimer = 0.0;
        this.isPaused = false;
        this.isGameOver = false;
        this.isLevelComplete = false;
        this.activePackets.clear();
        this.lastGameOverReason = GameOverReason.NONE;
    }

    /**
     * Determines whether the network is topologically disconnected, i.e.,
     * there is no directed path from any source reference system to any
     * destination reference system using active, non-destroyed wires and
     * non-failed systems. Temporary deactivation is ignored to avoid
     * spurious Game Over due to short cooldowns.
     */
    private boolean isNetworkTopologicallyDisconnected() {
        if (currentLevel == null) return false;

        List<ReferenceSystem> sources = currentLevel.getSourceSystems();
        List<ReferenceSystem> destinations = currentLevel.getDestinationSystems();
        if (sources.isEmpty() || destinations.isEmpty()) return false;

        // Build adjacency: directed edges along active, non-destroyed wires
        Map<String, List<String>> directed = new HashMap<>();
        for (model.System system : currentLevel.getSystems()) {
            directed.put(system.getId(), new ArrayList<>());
        }
        for (WireConnection wire : currentLevel.getWireConnections()) {
            if (wire == null || !wire.isActive() || wire.isDestroyed()) continue;
            Port src = wire.getSourcePort();
            Port dst = wire.getDestinationPort();
            if (src == null || dst == null) continue;
            model.System srcSys = src.getParentSystem();
            model.System dstSys = dst.getParentSystem();
            if (srcSys == null || dstSys == null) continue;
            // Only traverse through systems that have not permanently failed
            if (srcSys.hasFailed() || dstSys.hasFailed()) continue;
            directed.computeIfAbsent(srcSys.getId(), k -> new ArrayList<>()).add(dstSys.getId());
        }

        // BFS from each source to see if any destination is reachable (directed)
        Set<String> destinationIds = new HashSet<>();
        for (ReferenceSystem d : destinations) {
            destinationIds.add(d.getId());
        }
        for (ReferenceSystem s : sources) {
            if (s.hasFailed()) continue;
            if (isAnyDestinationReachableBFS(s.getId(), destinationIds, directed)) {
                return false; // At least one path exists → not disconnected
            }
        }

        // Fallback for early levels (level1/level2): treat connectivity as undirected
        // to avoid false Game Over when wires form a valid route visually but one
        // connection is reversed. This improves UX without affecting later levels.
        String lid = currentLevel.getLevelId();
        boolean isEarlyLevel = "level1".equals(lid) || "level2".equals(lid);
        if (isEarlyLevel) {
            Map<String, List<String>> undirected = new HashMap<>();
            for (model.System system : currentLevel.getSystems()) {
                undirected.put(system.getId(), new ArrayList<>());
            }
            for (WireConnection wire : currentLevel.getWireConnections()) {
                if (wire == null || !wire.isActive() || wire.isDestroyed()) continue;
                Port src = wire.getSourcePort();
                Port dst = wire.getDestinationPort();
                if (src == null || dst == null) continue;
                model.System a = src.getParentSystem();
                model.System b = dst.getParentSystem();
                if (a == null || b == null) continue;
                if (a.hasFailed() || b.hasFailed()) continue;
                undirected.computeIfAbsent(a.getId(), k -> new ArrayList<>()).add(b.getId());
                undirected.computeIfAbsent(b.getId(), k -> new ArrayList<>()).add(a.getId());
            }
            for (ReferenceSystem s : sources) {
                if (s.hasFailed()) continue;
                if (isAnyDestinationReachableBFS(s.getId(), destinationIds, undirected)) {
                    return false; // Considered connected for early levels
                }
            }
        }

        // No source can reach any destination
        return true;
    }

    private boolean isAnyDestinationReachableBFS(String startId, Set<String> destinationIds,
                                                 Map<String, List<String>> adjacency) {
        Queue<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(startId);
        visited.add(startId);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (destinationIds.contains(current)) return true;
            List<String> neighbors = adjacency.getOrDefault(current, List.of());
            for (String next : neighbors) {
                if (!visited.contains(next)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return false;
    }

    /**
     * Checks if the proportion of permanently failed (not temporarily deactivated)
     * systems exceeds the configured threshold (default 50%). Reference systems are
     * included in the calculation only if they have failed.
     */
    private boolean hasExcessiveFailedSystems() {
        if (currentLevel == null) return false;
        List<model.System> systems = currentLevel.getSystems();
        if (systems == null || systems.isEmpty()) return false;

        int failed = 0;
        int total = 0;
        for (model.System sys : systems) {
            if (sys == null) continue;
            total++;
            if (sys.hasFailed()) failed++;
        }
        if (total == 0) return false;

        double percentFailed = (failed * 100.0) / total;
        double threshold = getSetting("failedSystemsGameOverPercent", 50.0);
        return percentFailed > threshold;
    }

    /**
     * Gets a game setting value.
     */
    @SuppressWarnings("unchecked")
    public <T> T getSetting(String key, T defaultValue) {
        return (T) gameSettings.getOrDefault(key, defaultValue);
    }

    /**
     * Sets a game setting value.
     */
    public void setSetting(String key, Object value) {
        gameSettings.put(key, value);
    }

    /**
     * Gets all systems in the current level.
     */
    public List<System> getSystems() {
        if (currentLevel != null) {
            return currentLevel.getSystems();
        }
        return new ArrayList<>();
    }

    /**
     * Gets all wire connections in the current level.
     */
    public List<WireConnection> getWireConnections() {
        if (currentLevel != null) {
            return currentLevel.getWireConnections();
        }
        return new ArrayList<>();
    }

    /**
     * Adds a wire connection to the current level.
     */
    public void addWireConnection(WireConnection connection) {
        if (currentLevel != null) {
            currentLevel.addWireConnection(connection);
        }
    }

    /**
     * Removes a wire connection from the current level.
     */
    public void removeWireConnection(WireConnection connection) {
        if (currentLevel != null) {
            currentLevel.removeWireConnection(connection);
        }
    }

    /**
     * Checks if a wire connection exists between two ports.
     */
    public boolean hasWireConnection(Port port1, Port port2) {
        if (currentLevel == null) return false;

        for (WireConnection connection : currentLevel.getWireConnections()) {
            if (connection.isActive()) {
                Port source = connection.getSourcePort();
                Port dest = connection.getDestinationPort();

                if ((source == port1 && dest == port2) || (source == port2 && dest == port1)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "GameState{" +
                "remainingWireLength=" + remainingWireLength +
                ", temporalProgress=" + temporalProgress +
                ", packetLoss=" + packetLoss +
                ", coins=" + coins +
                ", activePackets=" + activePackets.size() +
                ", levelTimer=" + levelTimer +
                ", paused=" + isPaused +
                ", gameOver=" + isGameOver +
                ", levelComplete=" + isLevelComplete +
                '}';
    }
}