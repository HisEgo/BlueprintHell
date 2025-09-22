package model;

import java.util.List;
import java.util.ArrayList;

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

    public boolean isSource() {
        return !injectedPackets.isEmpty();
    }

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

    public void update(double currentTime) {
        if (!isActive()) return;

        // ReferenceSystem should not inject packets during temporal preview
        // Packet injection is handled by PacketInjection schedule in GameController
        // This method is kept for compatibility but does nothing during temporal preview
    }

    private void injectNextPacket() {
        if (injectedPackets.isEmpty()) return;

        Packet packet = injectedPackets.remove(0);

        // Use the same port selection logic as normal systems
        Port availablePort = findAvailableOutputPort(packet);
        if (availablePort != null) {
            availablePort.acceptPacket(packet);
        } else {
            // If no port available, call processPacket to handle storage (though reference systems usually have available ports)
            processPacket(packet);
        }
    }

    public void schedulePacketInjection(Packet packet) {
        injectedPackets.add(packet);
    }

    @Override
    public void processPacket(Packet packet) {
        processPacket(packet, null);
    }
    
    @Override
    public void processPacket(Packet packet, Port entryPort) {
        // Bit packets should not reach reference systems - they are considered lost
        if (packet.getPacketType() != null && packet.getPacketType().isBitPacket()) {
            java.lang.System.out.println("*** BIT PACKET LOSS *** Bit packet reached reference system: " + packet.getPacketType());
            packet.setActive(false);
            packet.setLost(true); // Mark as lost for proper counting
            return;
        }
        
        // Reference systems don't forward packets, they just receive them
        // Packets reaching here are considered "delivered"
        packet.setActive(false);
        
        // Only count delivered packets once to prevent duplication in temporal navigation
        if (!packet.isProcessedByReferenceSystem()) {
            deliveredPacketCount++;
            packet.setProcessedByReferenceSystem(true);
        }
    }

    public boolean hasReceivedPackets() {
        for (Port inputPort : getInputPorts()) {
            if (inputPort.getCurrentPacket() != null) {
                return true;
            }
        }
        return !getStorage().isEmpty();
    }

    public int getReceivedPacketCount() {
        // Return only the count of delivered packets (processed by processPacket)
        // This gives the correct count for packet loss calculation
        return deliveredPacketCount;
    }

    public void resetStatistics() {
        deliveredPacketCount = 0;
        lastInjectionTime = 0.0;
    }
    
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
