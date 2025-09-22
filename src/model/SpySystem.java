package model;

import java.util.List;
import java.util.ArrayList;

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
        processPacket(packet, null);
    }
    
    @Override
    public void processPacket(Packet packet, Port entryPort) {
        java.lang.System.out.println("SPY SYSTEM: Processing packet " + packet.getPacketType() + " in Spy" + java.lang.System.identityHashCode(this));
        
        // Destroy confidential packets immediately per spec
        if (packet.getPacketType() != null && packet.getPacketType().isConfidential()) {
            java.lang.System.out.println("SPY SYSTEM: Destroying confidential packet " + packet.getPacketType());
            packet.setActive(false);
            packet.setLost(true); // Mark as lost for proper counting
            return;
        }

        // Protected packets revert to original type when passing through spy (cannot be destroyed)
        if (packet instanceof ProtectedPacket) {
            ProtectedPacket protectedPacket = (ProtectedPacket) packet;
            Packet revertedPacket = protectedPacket.revertToOriginal();
            // Replace the protected packet with reverted packet
            replacePacketInSystem(packet, revertedPacket);
            packet = revertedPacket;
            // After reverting, continue normal processing in this system
            super.processPacket(packet, entryPort);
            return;
        } else if (packet.getPacketType() != null && packet.getPacketType().isProtected()) {
            packet.convertFromProtected();
            // After reverting, continue normal processing in this system
            super.processPacket(packet, entryPort);
            return;
        }

        // For other packets, teleport to any spy system (including this one)
        List<SpySystem> allSpySystems = findAllSpySystems();
        
        if (!allSpySystems.isEmpty()) {
            int randomIndex = (int) (Math.random() * allSpySystems.size());
            SpySystem targetSpy = allSpySystems.get(randomIndex);
            
            // Debug: Show which spy system was selected for teleportation
            java.lang.System.out.println("SPY TELEPORT: Packet " + packet.getPacketType() + " teleporting from Spy" + 
                    java.lang.System.identityHashCode(this) + " to Spy" + java.lang.System.identityHashCode(targetSpy) + 
                    " (total spies: " + allSpySystems.size() + ", same system: " + (targetSpy == this) + ")");
            
            // Always teleport, even if it's the same system
            teleportPacketToSpySystem(packet, targetSpy);
            return;
        }

        // If no spy systems exist (shouldn't happen), process as a normal system
        super.processPacket(packet);
    }

    private List<SpySystem> findAllSpySystems() {
        List<SpySystem> allSpySystems = new ArrayList<>();
        GameLevel level = getParentLevel();
        java.lang.System.out.println("SPY DEBUG: getParentLevel() returned: " + level);
        if (level == null) {
            java.lang.System.out.println("SPY DEBUG: parentLevel is null!");
            return allSpySystems;
        }
        java.lang.System.out.println("SPY DEBUG: Level has " + level.getSystems().size() + " systems");
        for (System system : level.getSystems()) {
            if (system instanceof SpySystem) {
                allSpySystems.add((SpySystem) system);
                java.lang.System.out.println("SPY DEBUG: Found spy system: " + java.lang.System.identityHashCode(system));
            }
        }
        java.lang.System.out.println("SPY DEBUG: Total spy systems found: " + allSpySystems.size());
        return allSpySystems;
    }

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

    private void teleportPacketToSpySystem(Packet packet, SpySystem targetSpy) {
        if (packet == null || targetSpy == null) {
            return;
        }

        // Find the best output port on the target spy system
        Port bestPort = targetSpy.findBestOutputPortForPacket(packet);
        
        if (bestPort != null) {
            java.lang.System.out.println("SPY TELEPORT: Packet placed on output port of Spy" + java.lang.System.identityHashCode(targetSpy));
            bestPort.acceptPacket(packet);
        } else {
            // If no output ports available, store in target system
            if (targetSpy.hasStorageSpace()) {
                java.lang.System.out.println("SPY TELEPORT: Packet stored in Spy" + java.lang.System.identityHashCode(targetSpy) + " storage");
                targetSpy.getStorage().add(packet);
            } else {
                // If no storage space, packet is lost
                java.lang.System.out.println("SPY TELEPORT: Packet lost - no space in Spy" + java.lang.System.identityHashCode(targetSpy));
                packet.setActive(false);
            }
        }
    }

    private Port findBestOutputPortForPacket(Packet packet) {
        // Priority 1: Compatible empty port
        for (Port port : getOutputPorts()) {
            if (port.isEmpty() && port.isCompatibleWithPacket(packet)) {
                return port;
            }
        }

        // Priority 2: Any empty port
        for (Port port : getOutputPorts()) {
            if (port.isEmpty()) {
                return port;
            }
        }

        // No available ports
        return null;
    }

    private void replacePacketInSystem(Packet oldPacket, Packet newPacket) {
        // Replace in storage
        if (getStorage().contains(oldPacket)) {
            getStorage().remove(oldPacket);
            getStorage().add(newPacket);
        }

        // Replace in input ports
        for (Port port : getInputPorts()) {
            if (port.getCurrentPacket() == oldPacket) {
                port.setCurrentPacket(newPacket);
            }
        }

        // Replace in output ports
        for (Port port : getOutputPorts()) {
            if (port.getCurrentPacket() == oldPacket) {
                port.setCurrentPacket(newPacket);
            }
        }
    }
}
