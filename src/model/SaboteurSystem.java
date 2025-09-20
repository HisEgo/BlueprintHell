package model;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;

public class SaboteurSystem extends System {
    private static final double TROJAN_CONVERSION_PROBABILITY = 0.3; // 30% chance
    private Random random;

    public SaboteurSystem() {
        super();
        setSystemType(SystemType.SABOTEUR);
        this.random = new Random();
    }

    public SaboteurSystem(Point2D position) {
        super(position, SystemType.SABOTEUR);
        this.random = new Random();
    }

    @Override
    public void processPacket(Packet packet) {
        // Protected packets are NOT affected by saboteur systems - they pass through unchanged
        if (packet instanceof ProtectedPacket || 
            (packet.getPacketType() != null && packet.getPacketType().isProtected())) {
            // Send protected packets to compatible ports like normal systems
            super.processPacket(packet);
            return;
        }

        // For non-protected packets, apply saboteur effects:
        
        // 1. Add noise if packet has no noise
        if (packet.getNoiseLevel() == 0.0) {
            packet.setNoiseLevel(1.0);
        }

        // 2. Convert to trojan with probability
        if (random.nextDouble() < TROJAN_CONVERSION_PROBABILITY) {
            packet.convertToTrojan();
        }

        // 3. Send to incompatible port (opposite of normal systems)
        sendToIncompatiblePort(packet);
    }

    private void sendToIncompatiblePort(Packet packet) {
        List<Port> incompatiblePorts = new ArrayList<>();
        List<Port> availablePorts = new ArrayList<>();

        // Find all available output ports and categorize them
        for (Port port : getOutputPorts()) {
            if (port.isEmpty()) {
                availablePorts.add(port);
                if (!port.isCompatibleWithPacket(packet)) {
                    incompatiblePorts.add(port);
                }
            }
        }

        // Priority 1: Send to incompatible ports (saboteur behavior)
        if (!incompatiblePorts.isEmpty()) {
            Port targetPort = incompatiblePorts.get(random.nextInt(incompatiblePorts.size()));
            targetPort.acceptPacket(packet);
            return;
        }

        // Priority 2: If no incompatible ports available, use any available port
        if (!availablePorts.isEmpty()) {
            Port targetPort = availablePorts.get(random.nextInt(availablePorts.size()));
            targetPort.acceptPacket(packet);
            return;
        }

        // Priority 3: Store if no output ports available
        if (hasStorageSpace()) {
            getStorage().add(packet);
        } else {
            // Packet is lost
            packet.setActive(false);
        }
    }

}