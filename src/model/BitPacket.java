package model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import controller.MovementController;

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

    @Override
    public void applyShockwave(Vec2D effectVector) {
        super.applyShockwave(effectVector);

        // Bit packets have collision reversal behavior like small messenger packets
        if (shouldReverseOnCollision()) {
            initiateCollisionReversal();
        }
    }

    public boolean shouldReverseOnCollision() {
        return true; // All bit packets have this behavior
    }

    private void initiateCollisionReversal() {
        setReversing(true);
        reverseDirection();
        setRetryDestination(true);
    }

    public boolean canReassembleWith(BitPacket other) {
        return other != null &&
                other.getParentBulkPacketId() != null &&
                other.getParentBulkPacketId().equals(this.parentBulkPacketId);
    }

    public MovementController.AccelerationType getAccelerationType(boolean isCompatiblePort) {
        // Size 1: constant acceleration from compatible, deceleration from incompatible
        return isCompatiblePort ?
                MovementController.AccelerationType.CONSTANT_ACCELERATION :
                MovementController.AccelerationType.DECELERATION;
    }
}


