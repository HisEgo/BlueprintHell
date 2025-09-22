package model;

import java.util.ArrayList;
import java.util.List;

public class DistributorSystem extends System {

    public DistributorSystem() {
        super();
        setSystemType(SystemType.DISTRIBUTOR);
    }

    public DistributorSystem(Point2D position) {
        super(position, SystemType.DISTRIBUTOR);
    }

    @Override
    public boolean hasStorageSpace() {
        // DistributorSystem has unlimited storage capacity
        return true;
    }

    @Override
    protected Port findAvailableOutputPort(Packet packet) {
        // Use the same compatibility-based selection as the parent System class
        // This ensures packets choose compatible ports first
        return super.findAvailableOutputPort(packet);
    }

    @Override
    public void processPacket(Packet packet) {
        // Check if it's a bulk packet
        if (packet.getPacketType() != null && packet.getPacketType().isBulk()) {
            // First handle the common bulk packet logic from parent class
            handleBulkPacketEffects(packet);
            // Then process the distribution-specific logic
            processBulkPacket((BulkPacket) packet);
        } else {
            // Process normally for non-bulk packets (messenger, confidential, etc.)
            // This behaves exactly like a normal system for non-bulk packets
            super.processPacket(packet);
        }
    }

    private void handleBulkPacketEffects(Packet packet) {
        // Handle bulk packet effects like destroying stored packets and changing port types
        // (copied from System.processPacket logic)
        
        // Destroy other packets in storage
        List<Packet> toRemove = new ArrayList<>();
        for (Packet stored : getStorage()) {
            if (stored != packet && stored.isActive()) {
                stored.setActive(false);
                toRemove.add(stored);
            }
        }
        if (!toRemove.isEmpty()) {
            getStorage().removeAll(toRemove);
        }
        
        // Bulk packets randomly change port types when entering systems
        if (packet instanceof BulkPacket) {
            randomlyChangePortTypes();
        }
    }

    private void randomlyChangePortTypes() {
        // Simple implementation of port type changing
        List<Port> allPorts = new ArrayList<>();
        allPorts.addAll(getInputPorts());
        allPorts.addAll(getOutputPorts());
        
        if (!allPorts.isEmpty()) {
            java.util.Random random = new java.util.Random();
            Port portToChange = allPorts.get(random.nextInt(allPorts.size()));
            
            PortShape currentShape = portToChange.getShape();
            PortShape[] availableShapes = {PortShape.SQUARE, PortShape.TRIANGLE, PortShape.HEXAGON};
            PortShape newShape;
            
            do {
                newShape = availableShapes[random.nextInt(availableShapes.length)];
            } while (newShape == currentShape);
            
            portToChange.setShape(newShape);
        }
    }

    private void processBulkPacket(BulkPacket bulkPacket) {
        // Deactivate the original bulk packet as it will be split
        bulkPacket.setActive(false);
        
        // Split bulk packet into bit packets
        List<Packet> bitPackets = bulkPacket.splitIntoBitPackets();
        
        // Store all bit packets in storage and let normal system processing handle them
        // This prevents the double-processing issue
        for (Packet bitPacket : bitPackets) {
            getStorage().add(bitPacket);
        }
    }
}

