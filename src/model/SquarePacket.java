package model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Square-shaped packet with size 2 and coin value 1.
 */
public class SquarePacket extends Packet {

    public SquarePacket() {
        super(PacketType.SQUARE_MESSENGER, new Point2D(), new Vec2D());
    }

    public SquarePacket(double noiseLevel, Point2D currentPosition, Vec2D movementVector) {
        super(PacketType.SQUARE_MESSENGER, currentPosition, movementVector);
        this.setNoiseLevel(noiseLevel);
    }

    @Override
    @JsonIgnore
    public int getCoinValue() {
        if (getPacketType() != null) {
            return getPacketType().getBaseCoinValue();
        }
        return 1;
    }
}