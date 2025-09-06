package model;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;

/**
 * Saboteur system that sends packets to incompatible ports and can convert packets to trojans.
 * Cannot affect protected packets.
 */
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
        // If a protected packet passes through saboteur, it reverts to original type (Phase 2 rule)
        if ((packet.getPacketType() != null && packet.getPacketType().isProtected()) || packet instanceof ProtectedPacket) {
            packet.convertFromProtected();
        }

        // Add noise if packet has no noise
        if (packet.getNoiseLevel() == 0.0) {
            packet.setNoiseLevel(1.0);
        }

        // Convert to trojan with probability (no effect on protected after reversion step above)
        if (packet.getPacketType() == null || !packet.getPacketType().isProtected()) {
            if (random.nextDouble() < TROJAN_CONVERSION_PROBABILITY) {
                packet.convertToTrojan();
            }
        }

        // Send to incompatible port
        sendToIncompatiblePort(packet);
    }

    /**
     * Sends a packet to an incompatible port.
     */
    private void sendToIncompatiblePort(Packet packet) {
        List<Port> incompatiblePorts = new ArrayList<>();

        for (Port port : getOutputPorts()) {
            if (!port.canAcceptPacket(packet)) {
                incompatiblePorts.add(port);
            }
        }

        if (!incompatiblePorts.isEmpty()) {
            // Choose a random incompatible port
            Port targetPort = incompatiblePorts.get(random.nextInt(incompatiblePorts.size()));
            targetPort.acceptPacket(packet);
        } else if (hasStorageSpace()) {
            // Store if no incompatible ports available
            getStorage().add(packet);
        } else {
            // Packet is lost
            packet.setActive(false);
        }
    }
}