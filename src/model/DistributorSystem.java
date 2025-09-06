package model;

import java.util.List;

/**
 * Distributor system that splits bulk packets into bit packets.
 * Behaves like a normal system for non-bulk packets.
 */
public class DistributorSystem extends System {

    public DistributorSystem() {
        super();
        setSystemType(SystemType.DISTRIBUTOR);
    }

    public DistributorSystem(Point2D position) {
        super(position, SystemType.DISTRIBUTOR);
    }

    @Override
    public void processPacket(Packet packet) {
        // Check if it's a bulk packet
        if (packet.getPacketType() != null && packet.getPacketType().isBulk()) {
            processBulkPacket((BulkPacket) packet);
        } else {
            // Process normally for non-bulk packets
            super.processPacket(packet);
        }
    }

    /**
     * Processes a bulk packet by splitting it into bit packets.
     */
    private void processBulkPacket(BulkPacket bulkPacket) {
        // Split bulk packet into bit packets
        List<Packet> bitPackets = bulkPacket.splitIntoBitPackets();

        // Try to send bit packets to output ports
        for (Packet bitPacket : bitPackets) {
            boolean sent = false;

            for (Port port : getOutputPorts()) {
                if (port.isEmpty()) {
                    port.acceptPacket(bitPacket);
                    sent = true;
                    break;
                }
            }

            // If no output ports available, store in system
            if (!sent && hasStorageSpace()) {
                getStorage().add(bitPacket);
            } else if (!sent) {
                // Packet is lost if no storage available
                bitPacket.setActive(false);
            }
        }
    }
}
