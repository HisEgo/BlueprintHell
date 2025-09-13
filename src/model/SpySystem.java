package model;

import java.util.List;
import java.util.ArrayList;

/**
 * Spy system that allows packets to exit from any other spy system.
 * Destroys confidential packets and cannot affect protected packets.
 */
public class SpySystem extends System {

    public SpySystem() {
        super();
        setSystemType(SystemType.SPY);
    }

    public SpySystem(Point2D position) {
        super(position, SystemType.SPY);
    }

    @Override
    public void processPacket(Packet packet) {
        // Destroy confidential packets immediately per spec
        if (packet.getPacketType() != null && packet.getPacketType().isConfidential()) {
            packet.setActive(false);
            packet.setLost(true); // Mark as lost for proper counting
            return;
        }

        // Protected packets revert to original type when passing through spy (cannot be destroyed)
        if ((packet.getPacketType() != null && packet.getPacketType().isProtected()) || packet instanceof ProtectedPacket) {
            packet.convertFromProtected();
            // After reverting, continue normal processing in this system
            super.processPacket(packet);
            return;
        }

        // For other packets, attempt teleport to any spy system (including this one)
        List<SpySystem> allSpySystems = findAllSpySystems();
        if (!allSpySystems.isEmpty()) {
            SpySystem targetSpy = allSpySystems.get((int) (Math.random() * allSpySystems.size()));
            if (targetSpy == this) {
                // If selected this spy system, process normally
                super.processPacket(packet);
            } else {
                // Teleport to another spy system
                teleportPacketToSpySystem(packet, targetSpy);
            }
            return;
        }

        // If no other spy systems exist, process as a normal system
        super.processPacket(packet);
    }

    /**
     * Finds all spy systems in the network (including this one).
     */
    private List<SpySystem> findAllSpySystems() {
        List<SpySystem> allSpies = new ArrayList<>();
        GameLevel level = getParentLevel();
        if (level == null) {
            return allSpies;
        }
        for (System system : level.getSystems()) {
            if (system instanceof SpySystem) {
                allSpies.add((SpySystem) system);
            }
        }
        return allSpies;
    }

    /**
     * Finds other spy systems in the network (excluding this one).
     */
    private List<SpySystem> findOtherSpySystems() {
        List<SpySystem> others = new ArrayList<>();
        GameLevel level = getParentLevel();
        if (level == null) {
            return others;
        }
        for (System system : level.getSystems()) {
            if (system instanceof SpySystem && system != this) {
                others.add((SpySystem) system);
            }
        }
        return others;
    }

    /**
     * Teleports a packet to another spy system.
     */
    private void teleportPacketToSpySystem(Packet packet, SpySystem targetSpy) {
        if (packet == null || targetSpy == null) {
            return;
        }

        // Prefer an empty, compatible output port
        for (Port port : targetSpy.getOutputPorts()) {
            if (port.isEmpty() && port.isCompatibleWithPacket(packet)) {
                port.acceptPacket(packet);
                return;
            }
        }

        // Fallback: place on any empty output port
        for (Port port : targetSpy.getOutputPorts()) {
            if (port.isEmpty()) {
                port.acceptPacket(packet);
                return;
            }
        }

        // If no output ports are available, try system storage; otherwise mark lost
        if (targetSpy.hasStorageSpace()) {
            targetSpy.getStorage().add(packet);
        } else {
            packet.setActive(false);
        }
    }
}
