package model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TrojanPacket extends Packet {

    public TrojanPacket() {
        super();
        setPacketType(PacketType.TROJAN);
        setSize(2); // Default trojan size
        setNoiseLevel(1.0); // Trojans start with noise
    }

    public TrojanPacket(Point2D currentPosition, Vec2D movementVector) {
        super(PacketType.TROJAN, currentPosition, movementVector);
        setSize(2);
        setNoiseLevel(1.0);
    }

    @Override
    @JsonIgnore
    public int getCoinValue() {
        return 0; // Trojans have no coin value
    }

    public Packet convertToMessenger() {
        // Convert to a medium messenger packet (size 2)
        MessengerPacket messenger = new MessengerPacket(PacketType.SQUARE_MESSENGER,
                getCurrentPosition(),
                getMovementVector());
        messenger.setSize(2);
        messenger.setNoiseLevel(0.0); // Reset noise level
        return messenger;
    }

    @Override
    public void applyShockwave(Vec2D effectVector) {
        super.applyShockwave(effectVector);
        // Trojan packets are more susceptible to shockwave effects
        setNoiseLevel(getNoiseLevel() + 0.5);
    }

    public boolean shouldBeDestroyed() {
        return getNoiseLevel() > getSize();
    }
}


