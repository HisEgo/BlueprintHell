package controller;

import model.*;
import java.util.List;

public class MovementController {

    // Movement constants for smooth motion
    private static final double DEFAULT_ACCELERATION = 80.0; // pixels/second^2
    private static final double DEFAULT_DECELERATION = 60.0; // pixels/second^2
    // Lower the cap speed to reduce likelihood of system damage and make motion smoother
    private static final double MAX_SPEED = 100.0; // pixels/second
    private static final double MIN_SPEED = 20.0; // pixels/second

    // Phase 2: Long wire acceleration constants
    private static final double LONG_WIRE_THRESHOLD = 200.0; // pixels (raise threshold so fewer wires accelerate)
    private static final double LONG_WIRE_ACCELERATION_MULTIPLIER = 1.2; // gentler acceleration
    private static final double SPEED_THRESHOLD_FOR_SYSTEM_DAMAGE = 100.0; // pixels/second

    public MovementController() {
    }

    public void updatePackets(List<Packet> packets, double deltaTime) {
        updatePackets(packets, deltaTime, true); // Default to smooth curves for backward compatibility
    }

    public void updatePackets(List<Packet> packets, double deltaTime, boolean useSmoothCurves) {
        updatePackets(packets, deltaTime, useSmoothCurves, 1.0); // Default acceleration factor
    }

    public void updatePackets(List<Packet> packets, double deltaTime, boolean useSmoothCurves, double accelerationFactor) {
        double acceleratedDeltaTime = deltaTime * accelerationFactor;
        for (Packet packet : packets) {
            if (packet.isActive()) {
                updatePacketMovement(packet, acceleratedDeltaTime, useSmoothCurves);
            }
        }
    }

    private void updatePacketMovement(Packet packet, double deltaTime) {
        updatePacketMovement(packet, deltaTime, true); // Default to smooth curves for backward compatibility
    }

    private void updatePacketMovement(Packet packet, double deltaTime, boolean useSmoothCurves) {
        if (packet.isOnWire()) {
            // Use path-based movement for packets on wires
            updateWireBasedMovement(packet, deltaTime, useSmoothCurves);
        } else {
            // Use traditional vector-based movement for packets not on wires
            updateFreeMovement(packet, deltaTime);
        }
    }

    private void updateWireBasedMovement(Packet packet, double deltaTime) {
        updateWireBasedMovement(packet, deltaTime, true); // Default to smooth curves for backward compatibility
    }

    private void updateWireBasedMovement(Packet packet, double deltaTime, boolean useSmoothCurves) {
        WireConnection wire = packet.getCurrentWire();
        if (wire == null) {
            return;
        }

        // Get acceleration profile for this packet type
        AccelerationProfile profile = getAccelerationProfile(packet, wire);

        // Calculate speed with packet-specific acceleration/deceleration
        double currentSpeed = packet.getBaseSpeed();
        double targetSpeed = calculateTargetSpeed(packet);

        // Apply acceleration based on packet type and port compatibility
        currentSpeed = applyAccelerationProfile(packet, currentSpeed, targetSpeed, profile, deltaTime);
        packet.setBaseSpeed(currentSpeed);

        // Calculate progress increment based on wire length and speed
        double wireLength = wire.getTotalLength(useSmoothCurves);
        if (wireLength > 0) {
            double progressIncrement = (currentSpeed * deltaTime) / wireLength;
            double newProgress = packet.getPathProgress() + progressIncrement;

            // Check if packet has reached the end of the wire
            if (newProgress >= 1.0) {
                newProgress = 1.0;
                packet.setPathProgress(newProgress);
                packet.updatePositionOnWire(useSmoothCurves);

                // Packet has reached destination - will be handled by wire transfer logic
                return;
            }

            // Update progress and position
            packet.setPathProgress(newProgress);
            packet.updatePositionOnWire(useSmoothCurves);

            // Update movement vector for visual effects and collision detection
            updateMovementVectorFromPath(packet, wire, deltaTime, useSmoothCurves);
        }
    }

    private void updateMovementVectorFromPath(Packet packet, WireConnection wire, double deltaTime) {
        updateMovementVectorFromPath(packet, wire, deltaTime, true); // Default to smooth curves for backward compatibility
    }

    private void updateMovementVectorFromPath(Packet packet, WireConnection wire, double deltaTime, boolean useSmoothCurves) {
        double currentProgress = packet.getPathProgress();
        double speed = packet.getBaseSpeed();

        // Calculate direction by looking slightly ahead on the path
        double lookAheadDistance = Math.min(0.01, speed * deltaTime / wire.getTotalLength(useSmoothCurves));
        double futureProgress = Math.min(1.0, currentProgress + lookAheadDistance);

        Point2D currentPos = wire.getPositionAtProgress(currentProgress, useSmoothCurves);
        Point2D futurePos = wire.getPositionAtProgress(futureProgress, useSmoothCurves);

        if (currentPos != null && futurePos != null) {
            Vec2D direction = new Vec2D(
                    futurePos.getX() - currentPos.getX(),
                    futurePos.getY() - currentPos.getY()
            );

            if (direction.magnitude() > 0) {
                Vec2D velocity = direction.normalize().scale(speed);
                packet.setMovementVector(velocity);
            }
        }
    }

    private void updateFreeMovement(Packet packet, double deltaTime) {
        // Apply acceleration/deceleration for smooth movement
        Vec2D currentVelocity = packet.getMovementVector();
        Vec2D acceleration = calculateAcceleration(packet);

        // Update velocity
        Vec2D newVelocity = currentVelocity.add(acceleration.scale(deltaTime));
        packet.setMovementVector(newVelocity);

        // Update position using traditional method
        packet.updatePosition(deltaTime);
    }

    private double calculateTargetSpeed(Packet packet) {
        // Start with packet-specific base speed from Phase 2 specifications
        double baseSpeed = getPacketBaseSpeed(packet);

        // Apply port compatibility speed adjustments for messenger packets
        WireConnection currentWire = packet.getCurrentWire();
        if (currentWire != null && packet instanceof MessengerPacket) {
            MessengerPacket messengerPacket = (MessengerPacket) packet;
            boolean isCompatible = isPortCompatible(packet, currentWire);
            double compatibilitySpeed = messengerPacket.calculateMovementSpeed(isCompatible);
            // Use the compatibility-adjusted speed as the base
            baseSpeed = compatibilitySpeed;
        }

        // Apply environmental factors
        baseSpeed = applyEnvironmentalFactors(packet, baseSpeed);

        // Phase 2: Apply long wire acceleration
        if (currentWire != null && shouldAccelerateOnLongWire(packet, currentWire)) {
            baseSpeed *= LONG_WIRE_ACCELERATION_MULTIPLIER;
        }

        // Ensure reasonable speed range
        return Math.max(MIN_SPEED, Math.min(MAX_SPEED * 1.5, baseSpeed));
    }

    private double getPacketBaseSpeed(Packet packet) {
        if (packet.getPacketType() == null) {
            return 60.0; // Default speed
        }

        switch (packet.getPacketType()) {
            case SMALL_MESSENGER:
                return 50.0; // Size 1 packets are nimble
            case SQUARE_MESSENGER:
                return 60.0; // Size 2 packets are standard
            case TRIANGLE_MESSENGER:
                return 70.0; // Size 3 packets are powerful
            case CONFIDENTIAL:
                return 55.0; // Confidential packets move cautiously
            case CONFIDENTIAL_PROTECTED:
                return 45.0; // Protected confidential packets are heavier
            case BULK_SMALL:
                return 40.0; // Bulk packets are slower
            case BULK_LARGE:
                return 35.0; // Large bulk packets are very slow
            case PROTECTED:
                return 50.0; // Protected packets vary but average
            default:
                return 60.0;
        }
    }

    private double applyEnvironmentalFactors(Packet packet, double baseSpeed) {
        // Adjust speed based on noise level (higher noise reduces speed)
        double noiseMultiplier = 1.0 - Math.min(0.5, packet.getNoiseLevel() / 10.0);
        return baseSpeed * noiseMultiplier;
    }

    private boolean shouldAccelerateOnLongWire(Packet packet, WireConnection wire) {
        if (wire == null || wire.getTotalLength() <= LONG_WIRE_THRESHOLD) {
            return false;
        }

        // Different packet types have different acceleration behaviors on long wires
        PacketType packetType = packet.getPacketType();
        if (packetType == null) return false;

        switch (packetType) {
            case SMALL_MESSENGER:
                // Size 1: Accelerates on long wires when from compatible ports
                return true;
            case SQUARE_MESSENGER:
                // Size 2: Accelerates moderately on long wires
                return true;
            case TRIANGLE_MESSENGER:
                // Size 3: Strong acceleration on long wires
                return true;
            case BULK_SMALL:
            case BULK_LARGE:
                // Bulk packets: Always accelerate on long wires
                return true;
            case CONFIDENTIAL:
            case CONFIDENTIAL_PROTECTED:
                // Confidential packets: Moderate acceleration
                return true;
            default:
                return false;
        }
    }

    public void applyAbilityEffects(Packet packet, List<model.AbilityType> activeAbilities) {
        if (activeAbilities == null) return;

        for (model.AbilityType ability : activeAbilities) {
            switch (ability) {
                case SCROLL_OF_AERGIA:
                    // Set packet acceleration to zero at selected points
                    // This could be implemented as reducing base speed to near zero
                    if (isPacketAtSelectedPoint(packet)) {
                        packet.setBaseSpeed(MIN_SPEED * 0.1); // Very slow movement
                    }
                    break;
                default:
                    // Other abilities handled elsewhere
                    break;
            }
        }
    }

    private boolean isPacketAtSelectedPoint(Packet packet) {
        // For now, apply to packets at certain progress points
        double progress = packet.getPathProgress();
        return progress > 0.25 && progress < 0.75; // Middle section of wire
    }

    public boolean hasPacketReachedDestination(Packet packet) {
        if (packet.isOnWire()) {
            return packet.getPathProgress() >= 1.0;
        }

        // For packets not on wires, use traditional distance check
        return false;
    }

    private Vec2D calculateAcceleration(Packet packet) {
        Vec2D currentVelocity = packet.getMovementVector();
        double currentSpeed = currentVelocity.magnitude();
        double targetSpeed = calculateTargetSpeed(packet);

        if (currentSpeed < targetSpeed) {
            // Accelerate towards target speed
            if (currentSpeed > 0) {
                return currentVelocity.normalize().scale(DEFAULT_ACCELERATION);
            } else {
                // If no current velocity, accelerate in a default direction
                return new Vec2D(DEFAULT_ACCELERATION, 0);
            }
        } else if (currentSpeed > targetSpeed) {
            // Decelerate to target speed
            return currentVelocity.normalize().scale(-DEFAULT_DECELERATION);
        }

        // Maintain current speed
        return new Vec2D(0, 0);
    }

    public void initializePacketOnWire(Packet packet, WireConnection wire) {
        packet.setCurrentWire(wire);
        packet.setPathProgress(0.0);
        packet.setBaseSpeed(calculateTargetSpeed(packet));
        packet.updatePositionOnWire();
    }

    public boolean isPacketSpeedDamaging(Packet packet) {
        if (packet == null || !packet.isActive()) {
            return false;
        }

        double packetSpeed = packet.getMovementVector().magnitude();
        return packetSpeed > SPEED_THRESHOLD_FOR_SYSTEM_DAMAGE;
    }

    public double getSpeedThresholdForSystemDamage() {
        return SPEED_THRESHOLD_FOR_SYSTEM_DAMAGE;
    }

    public void removePacketFromWire(Packet packet) {
        packet.setCurrentWire(null);
        packet.setPathProgress(0.0);
    }

    private AccelerationProfile getAccelerationProfile(Packet packet, WireConnection wire) {
        PacketType packetType = packet.getPacketType();
        boolean isCompatiblePort = isPortCompatible(packet, wire);

        // Handle protected packets with random movement types
        if (packet instanceof ProtectedPacket) {
            ProtectedPacket protectedPacket = (ProtectedPacket) packet;
            return getAccelerationProfileForType(protectedPacket.getCurrentMovementType(), isCompatiblePort);
        }

        return getAccelerationProfileForType(packetType, isCompatiblePort);
    }

    private AccelerationProfile getAccelerationProfileForType(PacketType packetType, boolean isCompatiblePort) {
        if (packetType == null) {
            return new AccelerationProfile(AccelerationType.CONSTANT_VELOCITY, DEFAULT_ACCELERATION);
        }

        switch (packetType) {
            case SMALL_MESSENGER:
                // Size 1: Constant acceleration from compatible, deceleration from incompatible
                if (isCompatiblePort) {
                    return new AccelerationProfile(AccelerationType.CONSTANT_ACCELERATION, DEFAULT_ACCELERATION);
                } else {
                    return new AccelerationProfile(AccelerationType.DECELERATION, DEFAULT_DECELERATION);
                }

            case SQUARE_MESSENGER:
                // Size 2: Constant velocity from both port types (Phase 2 spec)
                return new AccelerationProfile(AccelerationType.CONSTANT_VELOCITY, 0.0);

            case TRIANGLE_MESSENGER:
                // Size 3: Constant velocity from compatible, acceleration from incompatible
                if (isCompatiblePort) {
                    return new AccelerationProfile(AccelerationType.CONSTANT_VELOCITY, 0.0);
                } else {
                    return new AccelerationProfile(AccelerationType.ACCELERATION, DEFAULT_ACCELERATION * 1.5);
                }

            case CONFIDENTIAL:
            case CONFIDENTIAL_PROTECTED:
                // Confidential packets: Constant velocity
                return new AccelerationProfile(AccelerationType.CONSTANT_VELOCITY, 0.0);

            case PROTECTED:
                // Protected packets: Use random movement mechanics from messenger types
                // This is handled by the ProtectedPacket class itself
                return new AccelerationProfile(AccelerationType.CONSTANT_VELOCITY, 0.0);

            default:
                return new AccelerationProfile(AccelerationType.CONSTANT_VELOCITY, DEFAULT_ACCELERATION);
        }
    }

    private double applyAccelerationProfile(Packet packet, double currentSpeed, double targetSpeed,
                                            AccelerationProfile profile, double deltaTime) {
        switch (profile.getType()) {
            case CONSTANT_VELOCITY:
                // Maintain target speed with minimal adjustment
                if (Math.abs(currentSpeed - targetSpeed) > 5.0) {
                    return currentSpeed < targetSpeed ?
                            Math.min(targetSpeed, currentSpeed + DEFAULT_ACCELERATION * deltaTime) :
                            Math.max(targetSpeed, currentSpeed - DEFAULT_DECELERATION * deltaTime);
                }
                return currentSpeed;

            case CONSTANT_ACCELERATION:
                // Continuously accelerate at constant rate - ignore target speed
                return Math.min(MAX_SPEED, currentSpeed + profile.getAcceleration() * deltaTime);

            case ACCELERATION:
                // Accelerate towards and potentially beyond target speed
                return Math.min(MAX_SPEED, currentSpeed + profile.getAcceleration() * deltaTime);

            case DECELERATION:
                // Continuously decelerate
                return Math.max(MIN_SPEED, currentSpeed - profile.getAcceleration() * deltaTime);

            default:
                // Default behavior
                return currentSpeed < targetSpeed ?
                        Math.min(targetSpeed, currentSpeed + DEFAULT_ACCELERATION * deltaTime) :
                        Math.max(targetSpeed, currentSpeed - DEFAULT_DECELERATION * deltaTime);
        }
    }

    private boolean isPortCompatible(Packet packet, WireConnection wire) {
        if (wire == null || wire.getSourcePort() == null) {
            return true; // Default to compatible if no port info
        }

        return wire.getSourcePort().isCompatibleWithPacket(packet);
    }

    public enum AccelerationType {
        CONSTANT_VELOCITY,      // Maintains constant speed
        CONSTANT_ACCELERATION,  // Continuously accelerates at constant rate
        ACCELERATION,          // Accelerates towards target
        DECELERATION          // Continuously decelerates
    }

    public static class AccelerationProfile {
        private final AccelerationType type;
        private final double acceleration;

        public AccelerationProfile(AccelerationType type, double acceleration) {
            this.type = type;
            this.acceleration = acceleration;
        }

        public AccelerationType getType() {
            return type;
        }

        public double getAcceleration() {
            return acceleration;
        }
    }
}

