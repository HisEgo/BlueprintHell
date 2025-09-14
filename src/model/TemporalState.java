package model;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Queue;
import java.util.ArrayDeque;

/**
 * Represents a snapshot of the game state at a specific point in time.
 * Used for temporal navigation to store and restore game states.
 */
public class TemporalState {
    private final double time;
    private final List<Packet> activePackets;
    private final Map<WireConnection, List<Packet>> packetsOnWires;
    private final Map<Port, Packet> packetsInPorts;
    private final Map<System, Queue<Packet>> systemStorage;
    private final Map<PacketInjection, Boolean> injectionStates;
    private final double temporalProgress;
    private final double levelTimer;
    private final int lostPacketsCount;
    private final double packetLoss;
    private final boolean isPaused;
    private final boolean isGameOver;
    private final boolean isLevelComplete;

    public TemporalState(double time, GameState gameState, GameLevel level) {
        this.time = time;
        this.temporalProgress = gameState.getTemporalProgress();
        this.levelTimer = gameState.getLevelTimer();
        this.lostPacketsCount = gameState.getLostPacketsCount();
        this.packetLoss = gameState.getPacketLoss();
        this.isPaused = gameState.isPaused();
        this.isGameOver = gameState.isGameOver();
        this.isLevelComplete = gameState.isLevelComplete();

        // Deep copy active packets
        this.activePackets = new ArrayList<>();
        for (Packet packet : gameState.getActivePackets()) {
            this.activePackets.add(createPacketCopy(packet));
        }

        // Deep copy packets on wires
        this.packetsOnWires = new HashMap<>();
        for (WireConnection connection : level.getWireConnections()) {
            List<Packet> wirePackets = new ArrayList<>();
            for (Packet packet : connection.getPacketsOnWire()) {
                wirePackets.add(createPacketCopy(packet));
            }
            this.packetsOnWires.put(connection, wirePackets);
        }

        // Deep copy packets in ports
        this.packetsInPorts = new HashMap<>();
        for (System system : level.getSystems()) {
            for (Port port : system.getInputPorts()) {
                if (port.getCurrentPacket() != null) {
                    this.packetsInPorts.put(port, createPacketCopy(port.getCurrentPacket()));
                }
            }
            for (Port port : system.getOutputPorts()) {
                if (port.getCurrentPacket() != null) {
                    this.packetsInPorts.put(port, createPacketCopy(port.getCurrentPacket()));
                }
            }
        }

        // Deep copy system storage
        this.systemStorage = new HashMap<>();
        for (System system : level.getSystems()) {
            Queue<Packet> storageCopy = new ArrayDeque<>();
            for (Packet packet : system.getStorage()) {
                storageCopy.add(createPacketCopy(packet));
            }
            this.systemStorage.put(system, storageCopy);
        }

        // Copy injection states
        this.injectionStates = new HashMap<>();
        for (PacketInjection injection : level.getPacketSchedule()) {
            this.injectionStates.put(injection, injection.isExecuted());
        }
    }

    /**
     * Creates a deep copy of a packet for temporal state storage.
     */
    private Packet createPacketCopy(Packet original) {
        // Create packet using the same type as original
        Packet copy = createPacketByType(original.getPacketType(), original.getCurrentPosition(), original.getMovementVector());
        
        // Copy all relevant properties
        copy.setPathProgress(original.getPathProgress());
        copy.setCurrentWire(original.getCurrentWire());
        copy.setTravelTime(original.getTravelTime());
        copy.setBaseSpeed(original.getBaseSpeed());
        copy.setActive(original.isActive());
        copy.setLost(original.isLost());
        copy.setReversing(original.isReversing());
        copy.setRetryDestination(original.isRetryDestination());
        copy.setSourcePosition(original.getSourcePosition());
        copy.setDestinationPosition(original.getDestinationPosition());
        copy.setBulkPacketId(original.getBulkPacketId());
        copy.setBulkPacketColor(original.getBulkPacketColor());
        copy.setCoinAwardPending(original.isCoinAwardPending());
        copy.setNoiseLevel(original.getNoiseLevel());
        copy.setSize(original.getSize());
        copy.setMaxTravelTime(original.getMaxTravelTime());
        
        return copy;
    }

    /**
     * Creates a packet instance based on packet type.
     */
    private Packet createPacketByType(PacketType packetType, Point2D position, Vec2D movementVector) {
        if (packetType == null) {
            return new MessengerPacket(PacketType.SQUARE_MESSENGER, position, movementVector);
        }

        switch (packetType) {
            case SQUARE_MESSENGER:
            case TRIANGLE_MESSENGER:
            case SMALL_MESSENGER:
            case BIT_PACKET:
                return new MessengerPacket(packetType, position, movementVector);

            case CONFIDENTIAL:
            case CONFIDENTIAL_PROTECTED:
                return new ConfidentialPacket(packetType, position, movementVector);

            case BULK_SMALL:
            case BULK_LARGE:
                return new BulkPacket(packetType, position, movementVector);

            case PROTECTED:
                return new ProtectedPacket(packetType, position, movementVector);

            case TROJAN:
                return new TrojanPacket(position, movementVector);

            default:
                return new MessengerPacket(PacketType.SQUARE_MESSENGER, position, movementVector);
        }
    }

    /**
     * Restores this temporal state to the game state and level.
     */
    public void restoreTo(GameState gameState, GameLevel level) {
        // Restore basic game state
        gameState.setTemporalProgress(this.temporalProgress);
        gameState.setLevelTimer(this.levelTimer);
        gameState.setLostPacketsCount(this.lostPacketsCount);
        gameState.setPacketLoss(this.packetLoss);
        gameState.setPaused(this.isPaused);
        gameState.setGameOver(this.isGameOver);
        gameState.setLevelComplete(this.isLevelComplete);

        // Clear current state
        gameState.clearActivePackets();
        clearPacketsFromWires(level);
        clearPacketsFromPorts(level);
        clearPacketsFromSystems(level);

        // Restore active packets
        for (Packet packet : this.activePackets) {
            gameState.getActivePackets().add(createPacketCopy(packet));
        }

        // Restore packets on wires
        for (Map.Entry<WireConnection, List<Packet>> entry : this.packetsOnWires.entrySet()) {
            WireConnection connection = entry.getKey();
            for (Packet packet : entry.getValue()) {
                Packet packetCopy = createPacketCopy(packet);
                // Use acceptPacket instead of addPacket
                if (connection.canAcceptPacket()) {
                    connection.acceptPacket(packetCopy);
                } else {
                    // If wire can't accept packet, add it directly to the list
                    connection.getPacketsOnWire().add(packetCopy);
                }
            }
        }

        // Restore packets in ports
        for (Map.Entry<Port, Packet> entry : this.packetsInPorts.entrySet()) {
            Port port = entry.getKey();
            Packet packet = createPacketCopy(entry.getValue());
            port.setCurrentPacket(packet);
        }

        // Restore system storage
        for (Map.Entry<System, Queue<Packet>> entry : this.systemStorage.entrySet()) {
            System system = entry.getKey();
            system.clearStorage();
            for (Packet packet : entry.getValue()) {
                system.getStorage().add(createPacketCopy(packet));
            }
        }

        // Restore injection states
        for (Map.Entry<PacketInjection, Boolean> entry : this.injectionStates.entrySet()) {
            PacketInjection injection = entry.getKey();
            injection.setExecuted(entry.getValue());
        }
    }

    private void clearPacketsFromWires(GameLevel level) {
        for (WireConnection connection : level.getWireConnections()) {
            connection.clearPackets();
        }
    }

    private void clearPacketsFromPorts(GameLevel level) {
        for (System system : level.getSystems()) {
            for (Port port : system.getInputPorts()) {
                port.releasePacket();
            }
            for (Port port : system.getOutputPorts()) {
                port.releasePacket();
            }
        }
    }

    private void clearPacketsFromSystems(GameLevel level) {
        for (System system : level.getSystems()) {
            system.clearStorage();
        }
    }

    // Getters
    public double getTime() { return time; }
    public List<Packet> getActivePackets() { return activePackets; }
    public Map<WireConnection, List<Packet>> getPacketsOnWires() { return packetsOnWires; }
    public Map<Port, Packet> getPacketsInPorts() { return packetsInPorts; }
    public Map<System, Queue<Packet>> getSystemStorage() { return systemStorage; }
    public Map<PacketInjection, Boolean> getInjectionStates() { return injectionStates; }
    public double getTemporalProgress() { return temporalProgress; }
    public double getLevelTimer() { return levelTimer; }
    public int getLostPacketsCount() { return lostPacketsCount; }
    public double getPacketLoss() { return packetLoss; }
    public boolean isPaused() { return isPaused; }
    public boolean isGameOver() { return isGameOver; }
    public boolean isLevelComplete() { return isLevelComplete; }
}
