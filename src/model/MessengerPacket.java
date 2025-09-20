package model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import controller.MovementController.AccelerationType;

public class MessengerPacket extends Packet {

    public MessengerPacket() {
        super();
        setPacketType(PacketType.SQUARE_MESSENGER);
    }

    public MessengerPacket(PacketType packetType, Point2D currentPosition, Vec2D movementVector) {
        super(packetType, currentPosition, movementVector);
    }

    public MessengerPacket(int size, double noiseLevel, Point2D currentPosition, Vec2D movementVector) {
        super(size, noiseLevel, currentPosition, movementVector);
        setPacketType(getPacketTypeBySize(size));
    }

    private PacketType getPacketTypeBySize(int size) {
        switch (size) {
            case 1: return PacketType.SMALL_MESSENGER;
            case 2: return PacketType.SQUARE_MESSENGER;
            case 3: return PacketType.TRIANGLE_MESSENGER;
            default: return PacketType.SQUARE_MESSENGER;
        }
    }

    @Override
    @JsonIgnore
    public int getCoinValue() {
        return getCoinValueByType();
    }

    public double calculateMovementSpeed(boolean isCompatiblePort) {
        PacketType type = getPacketType();
        if (type == null) return 100.0; // Default speed

        double baseSpeed = 100.0;

        switch (type) {
            case SMALL_MESSENGER:
                // Size 1: Acceleration effects translate to speed multipliers for testing compatibility
                // Compatible port: 1.5x speed (represents acceleration effect)
                // Incompatible port: 0.5x speed (represents deceleration effect)
                return isCompatiblePort ? baseSpeed * 1.5 : baseSpeed * 0.5;

            case SQUARE_MESSENGER:
                // Size 2: Full speed from compatible, half speed from incompatible
                // Phase 2 spec: "speed when starting from compatible port is half its speed from incompatible"
                // This means: compatible = 0.5x, incompatible = 1.0x, so incompatible is 2x compatible
                return isCompatiblePort ? baseSpeed : baseSpeed * 0.5;

            case TRIANGLE_MESSENGER:
                // Size 3: Same base speed from compatible, 2x speed from incompatible (acceleration effect)
                // Phase 2 spec: "constant velocity from compatible, accelerated from incompatible"
                return isCompatiblePort ? baseSpeed : baseSpeed * 2.0;

            default:
                return baseSpeed;
        }
    }

    public void updateMovementForPort(boolean isCompatiblePort) {
        double speed = calculateMovementSpeed(isCompatiblePort);
        Vec2D direction = getMovementVector().normalize();
        setMovementVector(direction.scale(speed));

        // Apply exit speed doubling for incompatible ports (Phase 2 spec)
        if (!isCompatiblePort) {
            applyExitSpeedMultiplier(true);
        }
    }

    public void applyExitSpeedMultiplier(boolean wasIncompatiblePort) {
        if (wasIncompatiblePort) {
            setMovementVector(getMovementVector().scale(2.0));
        }
    }

    public AccelerationType getAccelerationType(boolean isCompatiblePort) {
        PacketType type = getPacketType();
        if (type == null) return AccelerationType.CONSTANT_VELOCITY;

        switch (type) {
            case SMALL_MESSENGER:
                // Size 1: constant acceleration from compatible, deceleration from incompatible
                return isCompatiblePort ? AccelerationType.CONSTANT_ACCELERATION : AccelerationType.DECELERATION;

            case SQUARE_MESSENGER:
                // Size 2: constant velocity from both port types
                return AccelerationType.CONSTANT_VELOCITY;

            case TRIANGLE_MESSENGER:
                // Size 3: constant velocity from compatible, acceleration from incompatible
                return isCompatiblePort ? AccelerationType.CONSTANT_VELOCITY : AccelerationType.ACCELERATION;

            default:
                return AccelerationType.CONSTANT_VELOCITY;
        }
    }

    @Override
    public void applyShockwave(Vec2D effectVector) {
        super.applyShockwave(effectVector);

        // Small messenger packets reverse direction after collision
        if (getPacketType() == PacketType.SMALL_MESSENGER) {
            initiateCollisionReversal();
        }
    }

    private void initiateCollisionReversal() {
        // Mark packet as reversing
        setReversing(true);

        // Reverse movement direction immediately
        reverseDirection();

        // Set flag to attempt reaching destination again after returning to source
        setRetryDestination(true);
    }

    public boolean shouldReverseOnCollision() {
        return getPacketType() == PacketType.SMALL_MESSENGER;
    }
}
