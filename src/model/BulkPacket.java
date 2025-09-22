package model;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class BulkPacket extends Packet {
    private int wirePassageCount;
    private static final int MAX_WIRE_PASSAGES = 3;

    public BulkPacket() {
        super();
        setPacketType(PacketType.BULK_SMALL);
        this.wirePassageCount = 0;
    }

    public BulkPacket(PacketType packetType, Point2D currentPosition, Vec2D movementVector) {
        super(packetType, currentPosition, movementVector);
        this.wirePassageCount = 0;
    }

    @Override
    @JsonIgnore
    public int getCoinValue() {
        return getCoinValueByType();
    }

    public boolean incrementWirePassage() {
        wirePassageCount++;
        return wirePassageCount >= MAX_WIRE_PASSAGES;
    }

    public int getWirePassageCount() {
        return wirePassageCount;
    }

    public void setWirePassageCount(int count) {
        this.wirePassageCount = count;
    }

    public boolean shouldDestroyWire() {
        return wirePassageCount >= MAX_WIRE_PASSAGES;
    }

    public void destroyStoredPackets(List<Packet> systemPackets) {
        // Bulk packets destroy all other packets stored in the system
        for (Packet packet : systemPackets) {
            if (packet != this && packet.isActive()) {
                packet.setActive(false);
            }
        }
    }

    public List<Packet> splitIntoBitPackets() {
        List<Packet> bitPackets = new ArrayList<>();
        String bulkId = UUID.randomUUID().toString();
        int colorIndex = (int)(Math.random() * 8); // Random color index (0-7)

        for (int i = 0; i < getSize(); i++) {
            BitPacket bitPacket = new BitPacket(
                    bulkId,
                    colorIndex,
                    new Point2D(getCurrentPosition().getX(), getCurrentPosition().getY()),
                    new Vec2D(getMovementVector().getX(), getMovementVector().getY())
            );
            bitPackets.add(bitPacket);
        }

        return bitPackets;
    }

    public double calculateMovementSpeed(boolean isOnBend) {
        if (getPacketType() == PacketType.BULK_SMALL) {
            // Size 8: Constant velocity on straight, acceleration on bends
            return isOnBend ? 150.0 : 100.0;
        } else if (getPacketType() == PacketType.BULK_LARGE) {
            // Size 10: Constant velocity with deflection
            return 80.0;
        }
        return 100.0;
    }

    public void applyDeflection(double distanceTraveled) {
        if (getPacketType() == PacketType.BULK_LARGE) {
            // Apply deflection every 50 units of distance
            if (distanceTraveled % 50.0 < 1.0) {
                double deflectionAngle = Math.random() * Math.PI / 4; // Random angle up to 45 degrees
                Vec2D currentMovement = getMovementVector();
                double magnitude = currentMovement.magnitude();

                // Apply deflection perpendicular to current direction
                double perpendicularX = -currentMovement.getY();
                double perpendicularY = currentMovement.getX();
                Vec2D deflection = new Vec2D(perpendicularX, perpendicularY).normalize().scale(magnitude * 0.1);

                setMovementVector(currentMovement.add(deflection));
            }
        }
    }

    public PortShape changePortType(PortShape currentPortType) {
        // Bulk packets randomly change port types when entering systems
        PortShape[] availableTypes = {PortShape.SQUARE, PortShape.TRIANGLE, PortShape.HEXAGON};
        PortShape newType;
        do {
            newType = availableTypes[(int)(Math.random() * availableTypes.length)];
        } while (newType == currentPortType); // Ensure it's different from current type
        return newType;
    }
}
