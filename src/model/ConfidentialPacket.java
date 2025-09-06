package model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Confidential packets for secure communication between systems.
 * Implements special movement mechanics to avoid simultaneous presence in systems.
 */
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

    /**
     * Adjusts speed to avoid simultaneous presence in a system with another packet.
     * Phase 2 spec: "if such a packet is on its way to a system where another packet is already stored,
     * it reduces its speed by a specific amount to avoid being present in the system at the same time"
     */
    public void adjustSpeedForSystemOccupancy(boolean systemHasOtherPackets) {
        if (systemHasOtherPackets && getPacketType() == PacketType.CONFIDENTIAL) {
            // Reduce speed by 50% to avoid simultaneous presence
            Vec2D currentMovement = getMovementVector();
            setMovementVector(currentMovement.scale(0.5));
            java.lang.System.out.println("DEBUG: Confidential packet reducing speed to avoid system occupancy");
        }
    }

    /**
     * Maintains distance from other packets on the network.
     * Phase 2 spec: "this packet will attempt to maintain a specific distance from all other packets
     * on the network wires by moving forward or backward along the connections."
     * Used for protected confidential packets (size 6).
     */
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

            java.lang.System.out.println("DEBUG: Protected confidential packet adjusting distance (min distance: " +
                    String.format("%.1f", minDistance) + ", target: " + targetDistance + ")");
        }
    }

    /**
     * Checks if this packet should be destroyed by spy systems.
     */
    public boolean shouldBeDestroyedBySpy() {
        return getPacketType() == PacketType.CONFIDENTIAL;
    }
}
