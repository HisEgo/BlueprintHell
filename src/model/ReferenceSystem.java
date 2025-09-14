package model;

import java.util.List;
import java.util.ArrayList;

/**
 * Reference system that can act as both source and destination for packets.
 * Can inject packets if it has a packet injection schedule.
 * Can receive packets as an endpoint (packets are delivered and not forwarded).
 */
public class ReferenceSystem extends System {
    private List<Packet> injectedPackets;
    private double injectionInterval;
    private double lastInjectionTime;
    private int deliveredPacketCount = 0; // Track successful deliveries

    public ReferenceSystem() {
        super();
        setSystemType(SystemType.REFERENCE);
        this.injectedPackets = new ArrayList<>();
        this.injectionInterval = 2.0; // Default 2 seconds
        this.lastInjectionTime = 0.0;
    }

    public ReferenceSystem(Point2D position) {
        super(position);
        setSystemType(SystemType.REFERENCE);
        this.injectedPackets = new ArrayList<>();
        this.injectionInterval = 2.0;
        this.lastInjectionTime = 0.0;
    }

    // Backward compatibility constructor
    public ReferenceSystem(Point2D position, boolean isSource) {
        this(position);
        // isSource parameter is ignored for backward compatibility
        // All reference systems can now act as both source and destination
    }

    /**
     * Checks if this reference system can inject packets (has packets to inject).
     * Replaces the old isSource() logic - now any reference system can be a source if it has packets.
     */
    public boolean isSource() {
        return !injectedPackets.isEmpty();
    }

    /**
     * Backward compatibility method - no longer used since all reference systems can be sources.
     */
    @Deprecated
    public void setSource(boolean source) {
        // This method is deprecated - use schedulePacketInjection() to make a system act as a source
    }

    public List<Packet> getInjectedPackets() {
        return injectedPackets;
    }

    public void setInjectedPackets(List<Packet> injectedPackets) {
        this.injectedPackets = injectedPackets;
    }

    public double getInjectionInterval() {
        return injectionInterval;
    }

    public void setInjectionInterval(double injectionInterval) {
        this.injectionInterval = injectionInterval;
    }

    public double getLastInjectionTime() {
        return lastInjectionTime;
    }

    public void setLastInjectionTime(double lastInjectionTime) {
        this.lastInjectionTime = lastInjectionTime;
    }

    /**
     * Updates the reference system based on current time.
     * If it has packets to inject, injects them at scheduled intervals.
     */
    public void update(double currentTime) {
        if (!isActive()) return;

        // ReferenceSystem should not inject packets during temporal preview
        // Packet injection is handled by PacketInjection schedule in GameController
        // This method is kept for compatibility but does nothing during temporal preview
    }

    /**
     * Injects the next packet from the injection schedule.
     * Uses the same port selection priority as normal systems:
     * 1. Compatible empty port (highest priority)
     * 2. Any empty port (random selection)
     */
    private void injectNextPacket() {
        if (injectedPackets.isEmpty()) return;

        Packet packet = injectedPackets.remove(0);

        // Use the same port selection logic as normal systems
        Port availablePort = findAvailableOutputPort(packet);
        if (availablePort != null) {
            availablePort.acceptPacket(packet);
            java.lang.System.out.println("DEBUG: ReferenceSystem injected " + packet.getPacketType() + 
                    " packet to " + availablePort.getShape() + " port");
        } else {
            // If no port available, call processPacket to handle storage (though reference systems usually have available ports)
            processPacket(packet);
            java.lang.System.out.println("DEBUG: ReferenceSystem processed " + packet.getPacketType() + 
                    " packet through normal system logic (no available output ports)");
        }
    }

    /**
     * Adds a packet to the injection schedule.
     */
    public void schedulePacketInjection(Packet packet) {
        injectedPackets.add(packet);
    }

    /**
     * Override to prevent forwarding packets - reference systems are endpoints.
     * All reference systems can receive packets, regardless of whether they also inject packets.
     */
    @Override
    public void processPacket(Packet packet) {
        // Reference systems don't forward packets, they just receive them
        // Packets reaching here are considered "delivered"
        packet.setActive(false);
        
        // Only count delivered packets once to prevent duplication in temporal navigation
        if (!packet.isProcessedByReferenceSystem()) {
            deliveredPacketCount++;
            packet.setProcessedByReferenceSystem(true);
            java.lang.System.out.println("*** PACKET DELIVERED *** " + packet.getPacketType() +
                    " (" + packet.getClass().getSimpleName() + ") to ReferenceSystem (total delivered: " + deliveredPacketCount + ")");
        }
    }

    /**
     * Checks if this reference system has received any packets.
     */
    public boolean hasReceivedPackets() {
        for (Port inputPort : getInputPorts()) {
            if (inputPort.getCurrentPacket() != null) {
                return true;
            }
        }
        return !getStorage().isEmpty();
    }

    /**
     * Gets the total number of packets received by this reference system.
     */
    public int getReceivedPacketCount() {
        // Return only the count of delivered packets (processed by processPacket)
        // This gives the correct count for packet loss calculation
        return deliveredPacketCount;
    }

    /**
     * Resets packet statistics for temporal navigation.
     */
    public void resetStatistics() {
        deliveredPacketCount = 0;
        lastInjectionTime = 0.0;
    }
    
    /**
     * Resets all packets to prevent duplication in temporal navigation.
     */
    public void resetPacketFlags() {
        // Reset all packets in storage
        for (Packet packet : getStorage()) {
            packet.setProcessedByReferenceSystem(false);
        }
        
        // Reset all packets in input ports
        for (Port inputPort : getInputPorts()) {
            Packet packet = inputPort.getCurrentPacket();
            if (packet != null) {
                packet.setProcessedByReferenceSystem(false);
            }
        }
    }
    
    /**
     * Gets the number of delivered packets (for coin calculation).
     */
    public int getDeliveredPacketCount() {
        return deliveredPacketCount;
    }

    @Override
    public String toString() {
        return "ReferenceSystem{" +
                "id='" + getId() + '\'' +
                ", position=" + getPosition() +
                ", canInject=" + isSource() +
                ", injectedPackets=" + injectedPackets.size() +
                ", delivered=" + deliveredPacketCount +
                ", active=" + isActive() +
                '}';
    }
}
