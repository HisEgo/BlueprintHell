package model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Represents a scheduled packet injection in a game level.
 * Defines when, what type, and from which system a packet should be injected.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PacketInjection {
    private double time;
    private PacketType packetType;
    @JsonIgnore // Prevent circular reference during JSON deserialization
    private System sourceSystem;
    private String sourceId;
    private boolean isExecuted;

    public PacketInjection() {
        this.isExecuted = false;
    }

    public PacketInjection(double time, PacketType packetType, System sourceSystem) {
        this.time = time;
        this.packetType = packetType;
        this.sourceSystem = sourceSystem;
        this.isExecuted = false;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public PacketType getPacketType() {
        return packetType;
    }

    public void setPacketType(PacketType packetType) {
        this.packetType = packetType;
    }

    public System getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(System sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    /**
     * Optional JSON-friendly identifier of the source system.
     * When loading from JSON, this will be resolved to {@link #sourceSystem}
     * using the systems defined in the {@link GameLevel}.
     */
    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public boolean isExecuted() {
        return isExecuted;
    }

    public void setExecuted(boolean executed) {
        isExecuted = executed;
    }

    /**
     * Creates a packet based on the injection configuration.
     */
    public Packet createPacket() {
        // Get position from first available output port of source system
        Point2D position = sourceSystem.getPosition(); // Default to system position
        if (sourceSystem != null && !sourceSystem.getOutputPorts().isEmpty()) {
            Port firstOutputPort = sourceSystem.getOutputPorts().get(0);
            if (firstOutputPort.getPosition() != null) {
                position = firstOutputPort.getPosition();
            }
        }
        Vec2D movementVector = new Vec2D(1, 0); // Default movement


        switch (packetType) {
            case SQUARE_MESSENGER:
            case TRIANGLE_MESSENGER:
            case SMALL_MESSENGER:
                return new MessengerPacket(packetType, position, movementVector);

            case CONFIDENTIAL:
            case CONFIDENTIAL_PROTECTED:
                return new ConfidentialPacket(packetType, position, movementVector);

            case BULK_SMALL:
            case BULK_LARGE:
                return new BulkPacket(packetType, position, movementVector);

            case PROTECTED:
                return new ProtectedPacket(packetType, position, movementVector);

            case TROJAN:
                MessengerPacket trojan = new MessengerPacket(PacketType.SQUARE_MESSENGER, position, movementVector);
                trojan.convertToTrojan();
                return trojan;

            case BIT_PACKET:
                return new MessengerPacket(PacketType.BIT_PACKET, position, movementVector);

            default:
                return new MessengerPacket(PacketType.SQUARE_MESSENGER, position, movementVector);
        }
    }

    /**
     * Binds the sourceId to the actual sourceSystem after JSON deserialization.
     * This method should be called after the level is loaded to resolve references.
     */
    public void bindSourceSystem(List<System> systems) {
        if (sourceId != null && sourceSystem == null) {
            for (System system : systems) {
                if (sourceId.equals(system.getId())) {
                    this.sourceSystem = system;
                    break;
                }
            }
        }
    }

    @Override
    public String toString() {
        return "PacketInjection{" +
                "time=" + time +
                ", packetType=" + packetType +
                ", sourceSystem=" + (sourceSystem != null ? sourceSystem.getId() : sourceId) +
                ", executed=" + isExecuted +
                '}';
    }
}
