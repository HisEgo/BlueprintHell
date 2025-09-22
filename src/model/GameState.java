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
        this.coins = 20;
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
        this.coins = 20;
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

    @JsonIgnore
    public double getDoubleSetting(String key, double defaultValue) {
        Object v = gameSettings.get(key);
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        return defaultValue;
    }

    public void incrementLostPackets() {
        this.lostPacketsCount++;
    }



    public int getLostPacketsCount() {
        return lostPacketsCount;
    }

    public void setLostPacketsCount(int lostPacketsCount) {
        this.lostPacketsCount = lostPacketsCount;
    }

    public boolean isShowSystemIndicators() {
        return showSystemIndicators;
    }

    public void setShowSystemIndicators(boolean showSystemIndicators) {
        this.showSystemIndicators = showSystemIndicators;
    }

    public void addActivePacket(Packet packet) {
        activePackets.add(packet);
    }

    public void removeActivePacket(Packet packet) {
        activePackets.remove(packet);
    }

    @JsonIgnore
    public int getActivePacketCount() {
        return activePackets.size();
    }

    public void setActivePacketCount(int count) {
        // This is computed from activePackets.size(), so we ignore the setter
    }

    public boolean consumeWireLength(double amount) {
        if (remainingWireLength >= amount) {
            remainingWireLength -= amount;
            return true;
        }
        return false;
    }

    public void addCoins(int amount) {
        coins += amount;
    }

    public boolean spendCoins(int amount) {
        if (coins >= amount) {
            coins -= amount;
            return true;
        }
        return false;
    }

    public void updateTemporalProgress(double deltaTime) {
        if (!isPaused && !isGameOver && !isLevelComplete) {
            temporalProgress += deltaTime;
            // Only increment level timer during simulation mode (not editing mode)
            // Level timer should be controlled by GameController based on current mode
        }
    }

    public void updateLevelTimer(double deltaTime) {
        if (!isPaused && !isGameOver && !isLevelComplete) {
            levelTimer += deltaTime;
        }
    }

    public double calculatePacketLossPercentage() {
        if (currentLevel == null) return 0.0;

        int totalInjected = getTotalInjectedPackets();
        int totalLost = getTotalLostPackets();

        if (totalInjected == 0) return 0.0;

        return (double) totalLost / totalInjected * 100.0;
    }

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

    public int getTotalLostPackets() {
        int lost = lostPacketsCount;
        
        // Count lost packets from active packets
        for (Packet packet : activePackets) {
            if (!packet.isActive() && packet.isLost()) {
                lost++;
            }
        }
        
        // Count lost packets from all systems (including spy systems)
        if (currentLevel != null) {
            for (model.System system : currentLevel.getSystems()) {
                if (system instanceof SpySystem) {
                    // Spy systems destroy confidential packets
                    for (Packet packet : system.getStorage()) {
                        if (!packet.isActive() && packet.isLost()) {
                            lost++;
                        }
                    }
                }
            }
        }
        
        return lost;
    }



    public void clearActivePackets() {
        activePackets.clear();
    }



    public int getTotalDeliveredPackets() {
        int delivered = 0;
        for (ReferenceSystem destSystem : currentLevel.getDestinationSystems()) {
            delivered += destSystem.getReceivedPacketCount();
        }
        return delivered;
    }

    public boolean shouldEndGame() {
        // Primary rule: excessive loss triggers Game Over
        boolean tooManyLost = calculatePacketLossPercentage() > 50.0;

        // Time limit rule: if level duration exceeded, check delivery rate
        boolean timeExceeded = false;
        if (currentLevel != null && levelTimer > currentLevel.getLevelDuration()) {
            // Calculate delivery success rate
            int totalInjected = getTotalInjectedPackets();
            int totalDelivered = getTotalDeliveredPackets();
            
            if (totalInjected > 0) {
                double deliveryRate = (double) totalDelivered / totalInjected;
                // Game Over only if less than 50% of packets were delivered
                if (deliveryRate < 0.5) {
                    timeExceeded = true;
                }
                // If 50% or more packets were delivered, let level complete instead
            } else {
                // No packets injected, consider it a failure after grace period
                if (levelTimer > currentLevel.getLevelDuration() + 5.0) {
                    timeExceeded = true;
                }
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

    public boolean shouldCompleteLevel() {
        if (currentLevel == null) return false;

        // Don't complete level if simulation just started (need at least 1 second)
        if (levelTimer < 1.0) return false;

        // Treat time elapsed as a valid level completion path
        boolean timeElapsed = levelTimer >= currentLevel.getLevelDuration();


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

    public void resetForLevel(GameLevel level) {
        this.currentLevel = level;
        this.remainingWireLength = level.getInitialWireLength();
        this.temporalProgress = 0.0;
        this.packetLoss = 0.0;
        this.coins = 20;
        this.activePackets.clear();
        this.levelTimer = 0.0;
        this.isPaused = false;
        this.isGameOver = false;
        this.isLevelComplete = false;
        this.lostPacketsCount = 0;
        this.showSystemIndicators = true; // Indicators are always ON
    }

    public void saveLevelStartState() {
        this.levelStartState = new LevelStartState(coins, lostPacketsCount, remainingWireLength);
    }
    
    public void restoreToLevelStart() {
        if (levelStartState != null) {
            this.coins = levelStartState.getCoins();
            this.lostPacketsCount = levelStartState.getLostPacketsCount();
            this.remainingWireLength = levelStartState.getRemainingWireLength();
        } else {
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

    @SuppressWarnings("unchecked")
    public <T> T getSetting(String key, T defaultValue) {
        return (T) gameSettings.getOrDefault(key, defaultValue);
    }

    public void setSetting(String key, Object value) {
        gameSettings.put(key, value);
    }

    public List<System> getSystems() {
        if (currentLevel != null) {
            return currentLevel.getSystems();
        }
        return new ArrayList<>();
    }

    public List<WireConnection> getWireConnections() {
        if (currentLevel != null) {
            return currentLevel.getWireConnections();
        }
        return new ArrayList<>();
    }

    public void addWireConnection(WireConnection connection) {
        if (currentLevel != null) {
            currentLevel.addWireConnection(connection);
        }
    }

    public void removeWireConnection(WireConnection connection) {
        if (currentLevel != null) {
            currentLevel.removeWireConnection(connection);
        }
    }

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
