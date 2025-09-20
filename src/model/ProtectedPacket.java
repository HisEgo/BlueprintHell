package model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Random;
import controller.MovementController.AccelerationType;

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
        
        // Protected packets are twice the size of original
        if (originalPacket.getPacketType() != null) {
            setSize(originalPacket.getPacketType().getBaseSize() * 2);
        }
    }

    private PacketType selectRandomMovementType() {
        PacketType[] messengerTypes = {
                PacketType.SMALL_MESSENGER,  // Size 1: acceleration/deceleration behavior
                PacketType.SQUARE_MESSENGER, // Size 2: speed variation behavior
                PacketType.TRIANGLE_MESSENGER   // Size 3: acceleration behavior
        };

        return messengerTypes[random.nextInt(messengerTypes.length)];
    }

    public void randomizeMovementTypeForNewWire() {
        this.currentMovementType = selectRandomMovementType();
    }

    public PacketType getCurrentMovementType() {
        return currentMovementType;
    }

    public PacketType getOriginalType() {
        return originalType;
    }

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

    public void updateMovementForPort(boolean isCompatiblePort) {
        double speed = calculateMovementSpeed(isCompatiblePort);
        Vec2D direction = getMovementVector().normalize();
        setMovementVector(direction.scale(speed));
    }

    public void applyExitSpeedMultiplier(boolean wasIncompatiblePort) {
        if (wasIncompatiblePort) {
            setMovementVector(getMovementVector().scale(2.0));
        }
    }

    @Override
    public void applyShockwave(Vec2D effectVector) {
        super.applyShockwave(effectVector);

        // If currently behaving like small messenger, reverse direction after collision
        if (currentMovementType == PacketType.SMALL_MESSENGER) {
            reverseDirection();
        }
    }

    public Packet revertToOriginal() {
        if (originalType.isMessenger()) {
            MessengerPacket reverted = new MessengerPacket(originalType, getCurrentPosition(), getMovementVector());
            reverted.setNoiseLevel(getNoiseLevel());
            reverted.setTravelTime(getTravelTime());
            reverted.setMaxTravelTime(getMaxTravelTime());
            reverted.setSize(originalType.getBaseSize()); // Restore original size
            return reverted;
        } else if (originalType.isConfidential()) {
            // For confidential packets, create a new confidential packet
            ConfidentialPacket reverted = new ConfidentialPacket(getCurrentPosition(), getMovementVector());
            reverted.setNoiseLevel(getNoiseLevel());
            reverted.setTravelTime(getTravelTime());
            reverted.setMaxTravelTime(getMaxTravelTime());
            reverted.setSize(originalType.getBaseSize()); // Restore original size
            return reverted;
        }
        
        // Fallback to messenger packet
        MessengerPacket reverted = new MessengerPacket(PacketType.SQUARE_MESSENGER, getCurrentPosition(), getMovementVector());
        reverted.setNoiseLevel(getNoiseLevel());
        reverted.setTravelTime(getTravelTime());
        reverted.setMaxTravelTime(getMaxTravelTime());
        reverted.setSize(2); // Default size
        return reverted;
    }

    @Override
    @JsonIgnore
    public int getCoinValue() {
        return 5; // Protected packets always give 5 coins
    }

    @Override
    public void setCurrentWire(WireConnection currentWire) {
        super.setCurrentWire(currentWire);
        if (currentWire != null) {
            // Randomize movement type for each new wire
            randomizeMovementTypeForNewWire();
        }
    }

}
