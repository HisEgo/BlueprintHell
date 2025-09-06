package model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Triangle-shaped packet with size 3 and coin value 2.
 */
public class TrianglePacket extends Packet {

    public TrianglePacket() {
        super(PacketType.TRIANGLE_MESSENGER, new Point2D(), new Vec2D());
    }

    public TrianglePacket(double noiseLevel, Point2D currentPosition, Vec2D movementVector) {
        super(PacketType.TRIANGLE_MESSENGER, currentPosition, movementVector);
        this.setNoiseLevel(noiseLevel);
    }

    @Override
    @JsonIgnore
    public int getCoinValue() {
        if (getPacketType() != null) {
            return getPacketType().getBaseCoinValue();
        }
        return 2;
    }
}
