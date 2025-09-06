package model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Abstract base class for all packets in the network simulation.
 * POJO class for serialization support.
 * Enhanced for Phase 2 with new packet types and mechanics.
 */
public abstract class Packet {
    private int size;
    private double noiseLevel;
    private Point2D currentPosition;
    private Vec2D movementVector;
    private String id;
    private boolean isActive;
    // Tracks explicit loss events (e.g., off-wire) to differentiate from deliveries
    private boolean wasLost;
    private PacketType packetType;
    private double travelTime;

    // Path-based movement tracking for wire movement
    private double pathProgress; // Progress along wire path (0.0 to 1.0)
    private WireConnection currentWire; // Reference to current wire (if on wire)
    private double baseSpeed; // Base speed for uniform motion
    private double maxTravelTime;
    private boolean isReversing;
    private boolean retryDestination; // For size 1 packets that need to retry after collision reversal
    private Point2D sourcePosition;
    private Point2D destinationPosition;
    private String bulkPacketId; // For bit packets belonging to a bulk packet
    private int bulkPacketColor; // For distinguishing bit packets
    // Marks that this packet has just entered a system input port and a coin award is pending
    private boolean coinAwardPending;

    public Packet() {
        this.id = java.util.UUID.randomUUID().toString();
        this.isActive = true;
        this.currentPosition = new Point2D();
        this.movementVector = new Vec2D();
        this.travelTime = 0.0;
        this.maxTravelTime = 30.0; // Default 30 seconds
        this.isReversing = false;
        this.retryDestination = false;
        this.sourcePosition = new Point2D();
        this.destinationPosition = new Point2D();

        // Initialize path-based movement tracking
        this.pathProgress = 0.0;
        this.currentWire = null;
        this.baseSpeed = 50.0; // Default speed in pixels per second
        this.coinAwardPending = false;
    }

    public Packet(int size, double noiseLevel, Point2D currentPosition, Vec2D movementVector) {
        this();
        this.size = size;
        this.noiseLevel = noiseLevel;
        this.currentPosition = currentPosition;
        this.movementVector = movementVector;
    }

    public Packet(PacketType packetType, Point2D currentPosition, Vec2D movementVector) {
        this();
        this.packetType = packetType;
        this.size = packetType.getBaseSize();
        this.currentPosition = currentPosition;
        this.movementVector = movementVector;
        this.noiseLevel = 0.0;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public double getNoiseLevel() {
        return noiseLevel;
    }

    public void setNoiseLevel(double noiseLevel) {
        this.noiseLevel = noiseLevel;
    }

    public Point2D getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(Point2D currentPosition) {
        this.currentPosition = currentPosition;
    }

    public Vec2D getMovementVector() {
        return movementVector;
    }

    public void setMovementVector(Vec2D movementVector) {
        this.movementVector = movementVector;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    // Phase 2 properties
    public PacketType getPacketType() {
        return packetType;
    }

    public void setPacketType(PacketType packetType) {
        this.packetType = packetType;
    }

    public double getTravelTime() {
        return travelTime;
    }

    public void setTravelTime(double travelTime) {
        this.travelTime = travelTime;
    }

    public double getMaxTravelTime() {
        return maxTravelTime;
    }

    public void setMaxTravelTime(double maxTravelTime) {
        this.maxTravelTime = maxTravelTime;
    }

    public boolean isReversing() {
        return isReversing;
    }

    public void setReversing(boolean reversing) {
        isReversing = reversing;
    }

    public boolean isRetryDestination() {
        return retryDestination;
    }

    public void setRetryDestination(boolean retryDestination) {
        this.retryDestination = retryDestination;
    }

    public Point2D getSourcePosition() {
        return sourcePosition;
    }

    public void setSourcePosition(Point2D sourcePosition) {
        this.sourcePosition = sourcePosition;
    }

    public Point2D getDestinationPosition() {
        return destinationPosition;
    }

    public void setDestinationPosition(Point2D destinationPosition) {
        this.destinationPosition = destinationPosition;
    }

    public String getBulkPacketId() {
        return bulkPacketId;
    }

    public void setBulkPacketId(String bulkPacketId) {
        this.bulkPacketId = bulkPacketId;
    }

    public int getBulkPacketColor() {
        return bulkPacketColor;
    }

    public void setBulkPacketColor(int bulkPacketColor) {
        this.bulkPacketColor = bulkPacketColor;
    }

    /**
     * Returns whether a coin award is pending for this packet's most recent system entry.
     */
    @JsonIgnore
    public boolean isCoinAwardPending() {
        return coinAwardPending;
    }

    /**
     * Marks that this packet has entered a system and should award coins once.
     */
    public void setCoinAwardPending(boolean pending) {
        this.coinAwardPending = pending;
    }

    /**
     * Updates the packet's position based on its movement vector.
     * Enhanced for Phase 2 with travel time tracking.
     */
    public void updatePosition(double deltaTime) {
        if (!isActive) return;

        // Update travel time
        travelTime += deltaTime;

        // Check if packet has exceeded max travel time
        if (travelTime > maxTravelTime) {
            isActive = false;
            return;
        }

        Vec2D movement = movementVector.scale(deltaTime);
        currentPosition = currentPosition.add(movement);
    }

    /**
     * Marks this packet as lost due to a rule (e.g., off-wire).
     */
    public void setLost(boolean lost) {
        this.wasLost = lost;
    }

    /**
     * Returns whether this packet has been explicitly marked as lost.
     */
    @JsonIgnore
    public boolean isLost() {
        return wasLost;
    }

    /**
     * Checks if the packet should be lost due to noise level exceeding size.
     */
    public boolean shouldBeLost() {
        return noiseLevel >= size;
    }

    /**
     * Applies a shockwave effect to this packet.
     */
    public void applyShockwave(Vec2D effectVector) {
        if (!isActive) return;
        movementVector = movementVector.add(effectVector);
        noiseLevel += 0.5; // Increase noise when hit by shockwave
    }

    /**
     * Gets the coin value of this packet.
     */
    @JsonIgnore
    public abstract int getCoinValue();

    /**
     * Gets the coin value based on packet type.
     */
    @JsonIgnore
    public int getCoinValueByType() {
        if (packetType != null) {
            return packetType.getBaseCoinValue();
        }
        return getCoinValue();
    }

    /**
     * Checks if this packet should be destroyed due to travel time.
     */
    public boolean shouldBeDestroyedByTime() {
        return travelTime > maxTravelTime;
    }

    /**
     * Resets travel time for a new wire connection.
     */
    public void resetTravelTime() {
        travelTime = 0.0;
    }

    /**
     * Gets the progress along the current wire path (0.0 to 1.0).
     */
    public double getPathProgress() {
        return pathProgress;
    }

    /**
     * Sets the progress along the current wire path (0.0 to 1.0).
     */
    public void setPathProgress(double pathProgress) {
        this.pathProgress = Math.max(0.0, Math.min(1.0, pathProgress));
    }

    /**
     * Gets the current wire connection this packet is traveling on.
     */
    public WireConnection getCurrentWire() {
        return currentWire;
    }

    /**
     * Sets the current wire connection this packet is traveling on.
     */
    public void setCurrentWire(WireConnection currentWire) {
        this.currentWire = currentWire;
        if (currentWire != null) {
            // Reset path progress when switching wires
            this.pathProgress = 0.0;
        }
    }

    /**
     * Gets the base speed for uniform motion.
     */
    public double getBaseSpeed() {
        return baseSpeed;
    }

    /**
     * Sets the base speed for uniform motion.
     */
    public void setBaseSpeed(double baseSpeed) {
        this.baseSpeed = Math.max(0.0, baseSpeed);
    }

    /**
     * Updates position based on path progress and current wire.
     * This enables uniform motion along curved wire paths.
     */
    public void updatePositionOnWire() {
        if (currentWire != null) {
            Point2D newPosition = currentWire.getPositionAtProgress(pathProgress);
            if (newPosition != null) {
                setCurrentPosition(newPosition);
            }
        }
    }

    /**
     * Checks if the packet is currently on a wire.
     */
    @JsonIgnore
    public boolean isOnWire() {
        return currentWire != null;
    }

    /**
     * JSON serialization compatibility - maps isOnWire() to onWire property.
     */
    public boolean getOnWire() {
        return isOnWire();
    }

    /**
     * JSON deserialization compatibility - ignores onWire setter.
     */
    public void setOnWire(boolean onWire) {
        // This is computed from currentWire, so we ignore the setter
    }

    /**
     * Reverses the packet's direction to return to source.
     */
    public void reverseDirection() {
        isReversing = true;
        movementVector = movementVector.scale(-1.0);
    }

    /**
     * Initiates packet return to source system when destination system fails.
     * Phase 2 requirement: Packets return to source along the same wire when destination fails.
     */
    public void returnToSource() {
        if (currentWire != null) {
            // Reverse the packet's progress on the current wire
            pathProgress = 1.0 - pathProgress;
            isReversing = true;

            // Swap source and destination positions for this packet's journey
            Point2D temp = sourcePosition;
            sourcePosition = destinationPosition;
            destinationPosition = temp;

            java.lang.System.out.println("*** PACKET RETURNING *** " + getClass().getSimpleName() +
                    " returning to source due to system failure");
        } else {
            // If not on wire, just reverse direction
            reverseDirection();
        }
    }

    /**
     * Checks if this packet is currently returning to source.
     */
    public boolean isReturningToSource() {
        return isReversing;
    }

    /**
     * JSON serialization compatibility - maps isReturningToSource() to returningToSource property.
     */
    public boolean getReturningToSource() {
        return isReturningToSource();
    }

    /**
     * JSON deserialization compatibility - ignores returningToSource setter.
     */
    public void setReturningToSource(boolean returningToSource) {
        // This is computed from isReversing, so we ignore the setter
    }

    /**
     * Converts this packet to a protected packet.
     */
    public void convertToProtected() {
        if (packetType != null && packetType.isMessenger()) {
            packetType = PacketType.PROTECTED;
            size = size * 2; // Protected packets are twice the size
        } else if (packetType != null && packetType.isConfidential()) {
            packetType = PacketType.CONFIDENTIAL_PROTECTED;
            size = packetType.getBaseSize();
        }
    }

    /**
     * Converts this packet from protected back to original type.
     */
    public void convertFromProtected() {
        if (packetType != null && packetType.isProtected()) {
            // For protected packets, we need to restore the original size
            size = size / 2; // Restore original size
            packetType = PacketType.SQUARE_MESSENGER; // Default back to square messenger
        }
    }

    /**
     * Converts this packet to a trojan packet.
     */
    public void convertToTrojan() {
        packetType = PacketType.TROJAN;
        size = 2;
    }

    /**
     * Converts this packet from trojan to messenger.
     */
    public void convertFromTrojan() {
        packetType = PacketType.SQUARE_MESSENGER;
        size = 2;
    }

    /**
     * Realigns the packet's center of gravity to the wire center.
     * Used by Scroll of Eliphas ability.
     */
    public void realignCenter() {
        // This method will be overridden by specific packet types
        // to implement their own realignment logic
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Packet packet = (Packet) obj;
        return Objects.equals(id, packet.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id='" + id + '\'' +
                ", size=" + size +
                ", noiseLevel=" + noiseLevel +
                ", position=" + currentPosition +
                ", active=" + isActive +
                '}';
    }
}
