package model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import controller.MovementController;

/**
 * Bit packets created when bulk packets are distributed.
 * These are messenger packets of size 1 that can be reassembled into bulk packets.
 */
public class BitPacket extends Packet {
    private String parentBulkPacketId;
    private int colorIndex;

    public BitPacket() {
        super();
        setPacketType(PacketType.BIT_PACKET);
        setSize(1);
        setNoiseLevel(0.0);
    }

    public BitPacket(String parentBulkPacketId, int colorIndex, Point2D currentPosition, Vec2D movementVector) {
        super(PacketType.BIT_PACKET, currentPosition, movementVector);
        setSize(1);
        setNoiseLevel(0.0);
        this.parentBulkPacketId = parentBulkPacketId;
        this.colorIndex = colorIndex;
    }

    @Override
    @JsonIgnore
    public int getCoinValue() {
        return 0; // Bit packets have no coin value until reassembled
    }

    public String getParentBulkPacketId() {
        return parentBulkPacketId;
    }

    public void setParentBulkPacketId(String parentBulkPacketId) {
        this.parentBulkPacketId = parentBulkPacketId;
    }

    public int getColorIndex() {
        return colorIndex;
    }

    public void setColorIndex(int colorIndex) {
        this.colorIndex = colorIndex;
    }

    /**
     * Bit packets behave like small messenger packets but with special properties.
     * They can be reassembled into bulk packets by Merger systems.
     */
    @Override
    public void applyShockwave(Vec2D effectVector) {
        super.applyShockwave(effectVector);

        // Bit packets have collision reversal behavior like small messenger packets
        if (shouldReverseOnCollision()) {
            initiateCollisionReversal();
        }
    }

    /**
     * Bit packets should reverse direction after collision (like size 1 messenger packets).
     */
    public boolean shouldReverseOnCollision() {
        return true; // All bit packets have this behavior
    }

    /**
     * Initiates collision reversal behavior for bit packets.
     */
    private void initiateCollisionReversal() {
        setReversing(true);
        reverseDirection();
        setRetryDestination(true);
    }

    /**
     * Checks if this bit packet can be reassembled with others from the same bulk packet.
     */
    public boolean canReassembleWith(BitPacket other) {
        return other != null &&
                other.getParentBulkPacketId() != null &&
                other.getParentBulkPacketId().equals(this.parentBulkPacketId);
    }

    /**
     * Gets the acceleration type for bit packets.
     * They behave like small messenger packets.
     */
    public MovementController.AccelerationType getAccelerationType(boolean isCompatiblePort) {
        // Size 1: constant acceleration from compatible, deceleration from incompatible
        return isCompatiblePort ?
                MovementController.AccelerationType.CONSTANT_ACCELERATION :
                MovementController.AccelerationType.DECELERATION;
    }
}

