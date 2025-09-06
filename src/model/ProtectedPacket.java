package model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Random;
import controller.MovementController.AccelerationType;

/**
 * Protected packets that randomly choose movement mechanics from messenger packet types.
 * Created when messenger packets pass through VPN systems.
 * Their specific type is unknown to the network and they are visually hidden.
 */
public class ProtectedPacket extends Packet {
    private PacketType originalType;
    private PacketType currentMovementType; // Randomly chosen movement behavior
    private Random random;

    public ProtectedPacket() {
        super();
        this.random = new Random();
        this.originalType = PacketType.SQUARE_MESSENGER; // Default
        this.currentMovementType = selectRandomMovementType();
        setPacketType(PacketType.PROTECTED);
    }

    public ProtectedPacket(PacketType originalType, Point2D currentPosition, Vec2D movementVector) {
        super(PacketType.PROTECTED, currentPosition, movementVector);
        this.random = new Random();
        this.originalType = originalType;
        this.currentMovementType = selectRandomMovementType();

        // Protected packets are twice the size of original
        if (originalType != null) {
            setSize(originalType.getBaseSize() * 2);
        }
    }

    public ProtectedPacket(Packet originalPacket) {
        this(originalPacket.getPacketType(), originalPacket.getCurrentPosition(), originalPacket.getMovementVector());
        this.setNoiseLevel(originalPacket.getNoiseLevel());
        this.setTravelTime(originalPacket.getTravelTime());
        this.setMaxTravelTime(originalPacket.getMaxTravelTime());
    }

    /**
     * Randomly selects a movement type from available messenger packet types.
     * This determines how the protected packet moves on each wire.
     */
    private PacketType selectRandomMovementType() {
        PacketType[] messengerTypes = {
                PacketType.SMALL_MESSENGER,  // Size 1: acceleration/deceleration behavior
                PacketType.SQUARE_MESSENGER, // Size 2: speed variation behavior
                PacketType.TRIANGLE_MESSENGER   // Size 3: acceleration behavior
        };

        return messengerTypes[random.nextInt(messengerTypes.length)];
    }

    /**
     * Re-randomizes movement type for each new wire connection.
     * Phase 2 spec: "randomly chosen from one of the messenger packet types" on each wire.
     */
    public void randomizeMovementTypeForNewWire() {
        this.currentMovementType = selectRandomMovementType();
    }

    /**
     * Gets the current movement type that determines movement mechanics.
     */
    public PacketType getCurrentMovementType() {
        return currentMovementType;
    }

    /**
     * Gets the original packet type before protection.
     */
    public PacketType getOriginalType() {
        return originalType;
    }

    /**
     * Calculates movement speed based on the randomly chosen movement type and port compatibility.
     * Follows Phase 2 specifications for messenger packet movement.
     */
    public double calculateMovementSpeed(boolean isCompatiblePort) {
        double baseSpeed = 100.0;

        // Use the randomly selected movement type to determine speed behavior
        switch (currentMovementType) {
            case SMALL_MESSENGER:
                // Size 1 behavior: Same base speed, acceleration/deceleration handled by MovementController
                return baseSpeed;

            case SQUARE_MESSENGER:
                // Size 2 behavior: Full speed from compatible, half speed from incompatible
                return isCompatiblePort ? baseSpeed : baseSpeed * 0.5;

            case TRIANGLE_MESSENGER:
                // Size 3 behavior: Same base speed, acceleration handled by MovementController
                return baseSpeed;

            default:
                return baseSpeed;
        }
    }

    /**
     * Gets the acceleration type based on current movement type.
     */
    public AccelerationType getAccelerationType(boolean isCompatiblePort) {
        switch (currentMovementType) {
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

    /**
     * Updates movement vector based on port compatibility using random movement type.
     * Phase 2 spec: messenger packets exit at 2x speed from incompatible ports.
     */
    public void updateMovementForPort(boolean isCompatiblePort) {
        double speed = calculateMovementSpeed(isCompatiblePort);
        Vec2D direction = getMovementVector().normalize();
        setMovementVector(direction.scale(speed));
    }

    /**
     * Applies the exit speed multiplier when leaving a system through an incompatible port.
     * Phase 2 spec: messenger packets exit at 2x speed from incompatible ports.
     */
    public void applyExitSpeedMultiplier(boolean wasIncompatiblePort) {
        if (wasIncompatiblePort) {
            setMovementVector(getMovementVector().scale(2.0));
        }
    }

    /**
     * Handles collision behavior based on current movement type.
     */
    @Override
    public void applyShockwave(Vec2D effectVector) {
        super.applyShockwave(effectVector);

        // If currently behaving like small messenger, reverse direction after collision
        if (currentMovementType == PacketType.SMALL_MESSENGER) {
            reverseDirection();
        }
    }

    /**
     * Converts back to original type (used when VPN system fails).
     */
    public Packet revertToOriginal() {
        MessengerPacket reverted = new MessengerPacket(originalType, getCurrentPosition(), getMovementVector());
        reverted.setNoiseLevel(getNoiseLevel());
        reverted.setTravelTime(getTravelTime());
        reverted.setMaxTravelTime(getMaxTravelTime());
        reverted.setSize(originalType.getBaseSize()); // Restore original size
        return reverted;
    }

    @Override
    @JsonIgnore
    public int getCoinValue() {
        return 5; // Protected packets always give 5 coins
    }

    /**
     * Override wire connection to randomize movement type.
     */
    @Override
    public void setCurrentWire(WireConnection currentWire) {
        super.setCurrentWire(currentWire);
        if (currentWire != null) {
            // Randomize movement type for each new wire
            randomizeMovementTypeForNewWire();
        }
    }

}
