package model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import java.util.Objects;

@JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class)
public class Port {
    private PortShape shape;
    private boolean isConnected;
    private System parentSystem;
    private Point2D position;
    private boolean isInput;
    private Packet currentPacket;

    public Port() {
        this.isConnected = false;
        this.currentPacket = null;
    }

    public Port(PortShape shape, System parentSystem, Point2D position, boolean isInput) {
        this();
        this.shape = shape;
        this.parentSystem = parentSystem;
        this.position = position;
        this.isInput = isInput;
    }

    public PortShape getShape() {
        return shape;
    }

    public void setShape(PortShape shape) {
        this.shape = shape;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public System getParentSystem() {
        return parentSystem;
    }

    public void setParentSystem(System parentSystem) {
        this.parentSystem = parentSystem;
    }

    public Point2D getPosition() {
        return position;
    }

    public void setPosition(Point2D position) {
        this.position = position;
    }

    public boolean isInput() {
        return isInput;
    }

    public void setInput(boolean input) {
        isInput = input;
    }

    public Packet getCurrentPacket() {
        return currentPacket;
    }

    public void setCurrentPacket(Packet currentPacket) {
        this.currentPacket = currentPacket;
    }

    public boolean canAcceptPacket(Packet packet) {
        if (currentPacket != null) return false; // Port is occupied

        // All ports can accept any packet type (different from wire connection compatibility)
        return packet != null && packet.isActive();
    }

    public boolean isCompatibleWithPacket(Packet packet) {
        if (packet.getPacketType() != null) {
            switch (packet.getPacketType()) {
                case SQUARE_MESSENGER:  // Size 2 = Square equivalent
                    return shape == PortShape.SQUARE;
                case TRIANGLE_MESSENGER:   // Size 3 = Triangle equivalent
                    return shape == PortShape.TRIANGLE;
                case SMALL_MESSENGER:   // Size 1 = Hexagon equivalent
                    return shape == PortShape.HEXAGON;

                // Phase 2 spec: "The concept of port compatibility is not meaningful for confidential packets"
                case CONFIDENTIAL:
                case CONFIDENTIAL_PROTECTED:
                    return true; // Always compatible for movement purposes

                // Phase 2 spec: "The concept of port compatibility is not meaningful for bulk packets"
                case BULK_SMALL:
                case BULK_LARGE:
                case BIT_PACKET:
                    return true; // Always compatible for movement purposes

                case PROTECTED:
                case TROJAN:
                    return true; // These can use any port

                default:
                    return false;
            }
        }

        // Fallback for legacy packet instances
        if (packet instanceof SquarePacket) {
            return shape == PortShape.SQUARE;
        } else if (packet instanceof TrianglePacket) {
            return shape == PortShape.TRIANGLE;
        }
        return false;
    }

    public boolean acceptPacket(Packet packet) {
        if (!canAcceptPacket(packet)) return false;

        currentPacket = packet;
        return true;
    }

    public Packet releasePacket() {
        Packet packet = currentPacket;
        currentPacket = null;
        return packet;
    }

    public boolean isEmpty() {
        return currentPacket == null;
    }

    public System getConnectedSystem() {
        if (!isConnected || parentSystem == null || parentSystem.getParentLevel() == null) {
            return null;
        }

        // Find the wire connection that involves this port
        GameLevel level = parentSystem.getParentLevel();
        for (WireConnection connection : level.getWireConnections()) {
            if (connection.isActive()) {
                Port sourcePort = connection.getSourcePort();
                Port destPort = connection.getDestinationPort();

                if (sourcePort == this) {
                    // This port is the source, return the destination's parent system
                    return destPort != null ? destPort.getParentSystem() : null;
                } else if (destPort == this) {
                    // This port is the destination, return the source's parent system
                    return sourcePort != null ? sourcePort.getParentSystem() : null;
                }
            }
        }

        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Port port = (Port) obj;
        return Objects.equals(parentSystem, port.parentSystem) &&
                Objects.equals(position, port.position) &&
                isInput == port.isInput;
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentSystem, position, isInput);
    }

    public void updatePositionRelativeToSystem() {
        if (parentSystem != null) {
            Point2D systemPos = parentSystem.getPosition();

            // Calculate new position based on system position and port offset
            // For now, use a simple offset - this could be enhanced with proper relative positioning
            Point2D newPosition = new Point2D(
                    systemPos.getX() + relativeOffset.getX(),
                    systemPos.getY() + relativeOffset.getY()
            );

            setPosition(newPosition);
        }
    }

    private Vec2D relativeOffset = new Vec2D(0, 0);

    public void setRelativeOffset(Vec2D offset) {
        this.relativeOffset = offset;
    }

    public Vec2D getRelativeOffset() {
        return relativeOffset;
    }

    @Override
    public String toString() {
        return "Port{" +
                "shape=" + shape +
                ", connected=" + isConnected +
                ", position=" + position +
                ", isInput=" + isInput +
                ", hasPacket=" + (currentPacket != null) +
                '}';
    }
}

