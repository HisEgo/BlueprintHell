package model;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class DistributorSystem extends System {
    private Random random;

    public DistributorSystem() {
        super();
        setSystemType(SystemType.DISTRIBUTOR);
        this.random = new Random();
    }

    public DistributorSystem(Point2D position) {
        super(position, SystemType.DISTRIBUTOR);
        this.random = new Random();
    }


    @Override
    public void processPacket(Packet packet) {
        processPacket(packet, null);
    }
    
    @Override
    public void processPacket(Packet packet, Port entryPort) {
        // Distributor systems handle bulk packets specially
        if (packet.getPacketType() != null && packet.getPacketType().isBulk()) {
            handleBulkPacketEffects(packet);
        } else {
            // Process normally for non-bulk packets (messenger, confidential, etc.)
            // This behaves exactly like a normal system for non-bulk packets
            super.processPacket(packet, null); // No entry port info for DistributorSystem
        }
    }

    private void handleBulkPacketEffects(Packet packet) {
        // Distributor systems split bulk packets into bit packets
        if (packet instanceof BulkPacket) {
            BulkPacket bulkPacket = (BulkPacket) packet;
            
            // Destroy other packets in storage (same as normal systems)
            List<Packet> toRemove = new ArrayList<>();
            for (Packet stored : getStorage()) {
                if (stored != packet && stored.isActive()) {
                    stored.setActive(false);
                    stored.setLost(true); // Mark as lost for statistics
                    toRemove.add(stored);
                }
            }
            if (!toRemove.isEmpty()) {
                getStorage().removeAll(toRemove);
            }
            
            // Also destroy packets in input ports
            for (Port inputPort : getInputPorts()) {
                Packet portPacket = inputPort.getCurrentPacket();
                if (portPacket != null && portPacket != packet && portPacket.isActive()) {
                    portPacket.setActive(false);
                    portPacket.setLost(true); // Mark as lost for statistics
                    inputPort.setCurrentPacket(null);
                }
            }
            
            // Also destroy packets in output ports
            for (Port outputPort : getOutputPorts()) {
                Packet portPacket = outputPort.getCurrentPacket();
                if (portPacket != null && portPacket != packet && portPacket.isActive()) {
                    portPacket.setActive(false);
                    portPacket.setLost(true); // Mark as lost for statistics
                    outputPort.setCurrentPacket(null);
                }
            }
            
            // Split bulk packet into bit packets
            List<Packet> bitPackets = bulkPacket.splitIntoBitPackets();
            
            // Deactivate the original bulk packet
            packet.setActive(false);
            
            // Add bit packets to storage for processing
            for (Packet bitPacket : bitPackets) {
                getStorage().add(bitPacket);
            }
            
            // For DistributorSystem, we don't have entry port info, so use random selection
            randomlyChangePortTypes();
        } else {
            // Process normally for non-bulk packets
            super.processPacket(packet, null);
        }
    }

    private void randomlyChangePortTypes() {
        // Randomly change one input port type
        if (!getInputPorts().isEmpty()) {
            Port randomInputPort = getInputPorts().get(random.nextInt(getInputPorts().size()));
            PortShape[] shapes = {PortShape.SQUARE, PortShape.TRIANGLE, PortShape.HEXAGON};
            PortShape currentShape = randomInputPort.getShape();
            PortShape newShape;
            do {
                newShape = shapes[random.nextInt(shapes.length)];
            } while (newShape == currentShape);
            randomInputPort.setShape(newShape);
        }
        
        // Randomly change one output port type
        if (!getOutputPorts().isEmpty()) {
            Port randomOutputPort = getOutputPorts().get(random.nextInt(getOutputPorts().size()));
            PortShape[] shapes = {PortShape.SQUARE, PortShape.TRIANGLE, PortShape.HEXAGON};
            PortShape currentShape = randomOutputPort.getShape();
            PortShape newShape;
            do {
                newShape = shapes[random.nextInt(shapes.length)];
            } while (newShape == currentShape);
            randomOutputPort.setShape(newShape);
        }
    }

    @Override
    public int getCoinValue() {
        // Distributor systems give standard coin value based on stored packets
        int totalValue = 0;
        for (Packet packet : getStorage()) {
            totalValue += packet.getCoinValue();
        }
        return totalValue;
    }

    public List<Packet> distributePacket(Packet packet) {
        List<Packet> bitPackets = new ArrayList<>();
        
        if (packet instanceof BulkPacket) {
            BulkPacket bulkPacket = (BulkPacket) packet;
            bitPackets = bulkPacket.splitIntoBitPackets();
        } else {
            // For non-bulk packets, create a single bit packet
            BitPacket bitPacket = new BitPacket(
                packet.getId(),
                0, // Default color
                packet.getCurrentPosition(),
                packet.getMovementVector()
            );
            bitPackets.add(bitPacket);
        }
        
        return bitPackets;
    }
}
