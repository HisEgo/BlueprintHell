package model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ConfidentialPacket extends Packet {

    public ConfidentialPacket() {
        super();
        setPacketType(PacketType.CONFIDENTIAL);
    }

    public ConfidentialPacket(Point2D currentPosition, Vec2D movementVector) {
        super(PacketType.CONFIDENTIAL, currentPosition, movementVector);
    }

    public ConfidentialPacket(PacketType packetType, Point2D currentPosition, Vec2D movementVector) {
        super(packetType, currentPosition, movementVector);
    }

    @Override
    @JsonIgnore
    public int getCoinValue() {
        return getCoinValueByType();
    }

    public void adjustSpeedForSystemOccupancy(boolean systemHasOtherPackets) {
        if (systemHasOtherPackets && getPacketType() == PacketType.CONFIDENTIAL) {
            // Reduce speed by 50% to avoid simultaneous presence
            Vec2D currentMovement = getMovementVector();
            setMovementVector(currentMovement.scale(0.5));
        }
    }

    public void maintainDistanceFromOtherPackets(java.util.List<Packet> otherPackets, double targetDistance) {
        if (getPacketType() != PacketType.CONFIDENTIAL_PROTECTED) {
            return;
        }

        Vec2D adjustment = new Vec2D(0, 0);
        int adjustmentCount = 0;
        double minDistance = Double.MAX_VALUE;

        for (Packet other : otherPackets) {
            if (other == this || !other.isActive()) continue;

            double distance = getCurrentPosition().distanceTo(other.getCurrentPosition());
            minDistance = Math.min(minDistance, distance);

            if (distance < targetDistance) {
                // Calculate adjustment vector
                Vec2D direction = getCurrentPosition().subtract(other.getCurrentPosition()).normalize();
                double adjustmentMagnitude = (targetDistance - distance) * 0.15; // Stronger adjustment
                adjustment = adjustment.add(direction.scale(adjustmentMagnitude));
                adjustmentCount++;
            }
        }

        if (adjustmentCount > 0) {
            // Apply average adjustment
            adjustment = adjustment.scale(1.0 / adjustmentCount);

            // For wire-based movement, adjust along the wire direction
            if (isOnWire() && getCurrentWire() != null) {
                // Move forward or backward along the wire to maintain distance
                Vec2D wireDirection = getCurrentWire().getDirectionVector().normalize();
                double adjustmentProjection = adjustment.dot(wireDirection);
                setMovementVector(getMovementVector().add(wireDirection.scale(adjustmentProjection)));
            } else {
                setMovementVector(getMovementVector().add(adjustment));
            }
        }
    }

    public boolean shouldBeDestroyedBySpy() {
        return getPacketType() == PacketType.CONFIDENTIAL;
    }
}
