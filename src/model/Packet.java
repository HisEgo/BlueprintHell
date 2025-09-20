package model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
    private PacketType originalPacketType; // Store original type before protection
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
    // Marks that this packet has been processed by a reference system to prevent duplication
    private boolean processedByReferenceSystem;

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
        this.processedByReferenceSystem = false;

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

    public PacketType getOriginalPacketType() {
        return originalPacketType;
    }

    public void setOriginalPacketType(PacketType originalPacketType) {
        this.originalPacketType = originalPacketType;
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

    @JsonIgnore
    public boolean isCoinAwardPending() {
        return coinAwardPending;
    }

    public void setCoinAwardPending(boolean pending) {
        this.coinAwardPending = pending;
    }

    @JsonIgnore
    public boolean isProcessedByReferenceSystem() {
        return processedByReferenceSystem;
    }

    public void setProcessedByReferenceSystem(boolean processed) {
        this.processedByReferenceSystem = processed;
    }

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

    public void setLost(boolean lost) {
        this.wasLost = lost;
    }

    @JsonIgnore
    public boolean isLost() {
        return wasLost;
    }

    public boolean shouldBeLost() {
        return noiseLevel >= size;
    }

    public void applyShockwave(Vec2D effectVector) {
        if (!isActive) return;
        movementVector = movementVector.add(effectVector);
        noiseLevel += 0.5; // Increase noise when hit by shockwave
    }

    @JsonIgnore
    public abstract int getCoinValue();

    @JsonIgnore
    public int getCoinValueByType() {
        if (packetType != null) {
            return packetType.getBaseCoinValue();
        }
        return getCoinValue();
    }

    public boolean shouldBeDestroyedByTime() {
        return travelTime > maxTravelTime;
    }

    public void resetTravelTime() {
        travelTime = 0.0;
    }

    public double getPathProgress() {
        return pathProgress;
    }

    public void setPathProgress(double pathProgress) {
        this.pathProgress = Math.max(0.0, Math.min(1.0, pathProgress));
    }

    public WireConnection getCurrentWire() {
        return currentWire;
    }

    public void setCurrentWire(WireConnection currentWire) {
        this.currentWire = currentWire;
        if (currentWire != null) {
            // Reset path progress when switching wires
            this.pathProgress = 0.0;
        }
    }

    public double getBaseSpeed() {
        return baseSpeed;
    }

    public void setBaseSpeed(double baseSpeed) {
        this.baseSpeed = Math.max(0.0, baseSpeed);
    }

    public void updatePositionOnWire() {
        updatePositionOnWire(true); // Default to smooth curves for backward compatibility
    }

    public void updatePositionOnWire(boolean useSmoothCurves) {
        if (currentWire != null) {
            Point2D newPosition = currentWire.getPositionAtProgress(pathProgress, useSmoothCurves);
            if (newPosition != null) {
                setCurrentPosition(newPosition);
            }
        }
    }

    @JsonIgnore
    public boolean isOnWire() {
        return currentWire != null;
    }

    public boolean getOnWire() {
        return isOnWire();
    }

    public void setOnWire(boolean onWire) {
        // This is computed from currentWire, so we ignore the setter
    }

    public void reverseDirection() {
        isReversing = true;
        movementVector = movementVector.scale(-1.0);
    }

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

    public boolean isReturningToSource() {
        return isReversing;
    }

    public boolean getReturningToSource() {
        return isReturningToSource();
    }

    public void setReturningToSource(boolean returningToSource) {
        // This is computed from isReversing, so we ignore the setter
    }

    public void convertToProtected() {
        if (packetType != null && packetType.isMessenger()) {
            originalPacketType = packetType; // Store original type
            packetType = PacketType.PROTECTED;
            size = size * 2; // Protected packets are twice the size
        } else if (packetType != null && packetType.isConfidential()) {
            originalPacketType = packetType; // Store original type
            packetType = PacketType.CONFIDENTIAL_PROTECTED;
            size = packetType.getBaseSize();
        }
    }

    public void convertFromProtected() {
        if (packetType != null && packetType.isProtected()) {
            if (originalPacketType != null) {
                // Restore to the original type that was stored
                packetType = originalPacketType;
                size = originalPacketType.getBaseSize(); // Restore original size
                originalPacketType = null; // Clear the stored original type
            } else {
                // Fallback if original type wasn't stored
                size = size / 2; // Restore original size
                packetType = PacketType.SQUARE_MESSENGER; // Default back to square messenger
            }
        }
    }

    public void convertToTrojan() {
        packetType = PacketType.TROJAN;
        size = 2;
    }

    public void convertFromTrojan() {
        packetType = PacketType.SQUARE_MESSENGER;
        size = 2;
    }

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
