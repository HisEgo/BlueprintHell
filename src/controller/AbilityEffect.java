package controller;

import model.*;
import java.util.List;

/**
 * Base class for all ability effects.
 * Handles duration tracking and provides interface for applying effects.
 */
public abstract class AbilityEffect {
    protected double duration;
    protected double remainingTime;
    protected Point2D effectPoint;
    protected boolean isActive;

    public AbilityEffect(Point2D effectPoint, double duration) {
        this.effectPoint = effectPoint;
        this.duration = duration;
        this.remainingTime = duration;
        this.isActive = true;
    }

    /**
     * Updates the effect, decreasing remaining time.
     */
    public void update(double deltaTime) {
        if (isActive) {
            remainingTime -= deltaTime;
            if (remainingTime <= 0) {
                isActive = false;
            }
        }
    }

    /**
     * Applies the effect to a list of packets.
     */
    public abstract void applyToPackets(List<Packet> packets);

    /**
     * Checks if the effect has expired.
     */
    public boolean isExpired() {
        return !isActive || remainingTime <= 0;
    }

    /**
     * Gets the remaining time for this effect.
     */
    public double getRemainingTime() {
        return remainingTime;
    }

    /**
     * Gets the effect point.
     */
    public Point2D getEffectPoint() {
        return effectPoint;
    }

    /**
     * Gets the original duration.
     */
    public double getDuration() {
        return duration;
    }
}

/**
 * Aergia effect - sets packet acceleration to zero at a specific point.
 */
class AergiaEffect extends AbilityEffect {
    private WireConnection targetWire;
    private double wireProgress;
    private static final double EFFECT_RADIUS = 25.0; // 25 pixel radius around the point

    public AergiaEffect(WireConnection wire, double progress, Point2D effectPoint, double duration) {
        super(effectPoint, duration);
        this.targetWire = wire;
        this.wireProgress = progress;
    }

    @Override
    public void applyToPackets(List<Packet> packets) {
        if (!isActive || targetWire == null) return;

        for (Packet packet : packets) {
            if (packet.getCurrentWire() == targetWire && packet.isActive()) {
                double packetProgress = packet.getPathProgress();

                // Check if packet is near the effect point
                if (Math.abs(packetProgress - wireProgress) < 0.1) { // 10% of wire length tolerance
                    // Set acceleration to zero by maintaining constant velocity
                    packet.setBaseSpeed(Math.max(20.0, packet.getBaseSpeed() * 0.1)); // Very slow but not stopped

                    // Apply visual effect
                    java.lang.System.out.println("Aergia effect applied to packet " + packet.getId() +
                            " - acceleration set to zero");
                }
            }
        }
    }

    public WireConnection getTargetWire() {
        return targetWire;
    }

    public double getWireProgress() {
        return wireProgress;
    }
}

/**
 * Sisyphus effect - allows moving a specific system.
 */
class SisyphusEffect extends AbilityEffect {
    private model.System targetSystem;
    private Point2D originalPosition;
    private static final double MAX_MOVE_RADIUS = 100.0; // 100 pixel max movement

    public SisyphusEffect(model.System system, Point2D activationPoint, double duration) {
        super(activationPoint, duration);
        this.targetSystem = system;
        this.originalPosition = new Point2D(system.getPosition().getX(), system.getPosition().getY());
    }

    @Override
    public void applyToPackets(List<Packet> packets) {
        // Sisyphus doesn't directly affect packets, but enables system movement
        // The actual movement is handled by AbilityManager.moveSystem()
    }

    public model.System getTargetSystem() {
        return targetSystem;
    }

    public Point2D getOriginalPosition() {
        return originalPosition;
    }

    public double getMaxMoveRadius() {
        return MAX_MOVE_RADIUS;
    }
}

/**
 * Eliphas effect - continuously realigns packet centers to wire center.
 */
class EliphasEffect extends AbilityEffect {
    private WireConnection targetWire;
    private double wireProgress;
    private static final double REALIGNMENT_STRENGTH = 0.5; // How strongly to pull packets to center
    private static final double EFFECT_RADIUS = 30.0; // 30 pixel radius around the point

    public EliphasEffect(WireConnection wire, double progress, Point2D effectPoint, double duration) {
        super(effectPoint, duration);
        this.targetWire = wire;
        this.wireProgress = progress;
    }

    @Override
    public void applyToPackets(List<Packet> packets) {
        if (!isActive || targetWire == null) return;

        for (Packet packet : packets) {
            if (packet.getCurrentWire() == targetWire && packet.isActive()) {
                double packetProgress = packet.getPathProgress();

                // Check if packet is near the effect point
                if (Math.abs(packetProgress - wireProgress) < 0.15) { // 15% of wire length tolerance
                    realignPacketCenter(packet);
                }
            }
        }
    }

    /**
     * Realigns a packet's center to the wire center.
     */
    private void realignPacketCenter(Packet packet) {
        // Get the current position and the ideal wire center position
        Point2D currentPos = packet.getCurrentPosition();
        Point2D wireCenterPos = targetWire.getPositionAtProgress(packet.getPathProgress());

        if (wireCenterPos != null) {
            // Calculate the offset from wire center
            Vec2D offset = new Vec2D(
                    currentPos.getX() - wireCenterPos.getX(),
                    currentPos.getY() - wireCenterPos.getY()
            );

            // Gradually move packet towards wire center
            Vec2D correction = offset.scale(-REALIGNMENT_STRENGTH * 0.1); // Smooth correction
            Vec2D currentMovement = packet.getMovementVector();
            Vec2D correctedMovement = currentMovement.add(correction);

            packet.setMovementVector(correctedMovement);

            // Also slightly adjust position for immediate effect
            Point2D correctedPos = new Point2D(
                    currentPos.getX() + correction.getX(),
                    currentPos.getY() + correction.getY()
            );
            packet.setCurrentPosition(correctedPos);

            java.lang.System.out.println("Eliphas effect: realigning packet " + packet.getId() +
                    " center (offset: " + offset.magnitude() + ")");
        }
    }

    public WireConnection getTargetWire() {
        return targetWire;
    }

    public double getWireProgress() {
        return wireProgress;
    }
}
