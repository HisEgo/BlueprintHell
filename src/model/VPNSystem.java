package model;

public class VPNSystem extends System {

    public VPNSystem() {
        super();
        setSystemType(SystemType.VPN);
    }

    public VPNSystem(Point2D position) {
        super(position, SystemType.VPN);
    }

    @Override
    public void processPacket(Packet packet) {
        // Convert messenger packets to protected packets
        if (packet.getPacketType() != null && packet.getPacketType().isMessenger()) {
            ProtectedPacket protectedPacket = new ProtectedPacket(packet);

            // Replace the original packet with protected packet
            replacePacketInSystem(packet, protectedPacket);
            packet = protectedPacket;
        } else if (packet.getPacketType() != null && packet.getPacketType().isConfidential()) {
            // For confidential packets, use the existing conversion method
            packet.convertToProtected();
        }

        // Process normally after conversion
        super.processPacket(packet);
    }

    /**
     * Replaces a packet in the system's storage and ports.
     */
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

    /**
     * Reverts all protected packets to their original type when VPN fails.
     */
    public void revertProtectedPackets() {
        // Revert packets in storage
        for (int i = 0; i < getStorage().size(); i++) {
            Packet packet = getStorage().get(i);
            if (packet instanceof ProtectedPacket) {
                ProtectedPacket protectedPacket = (ProtectedPacket) packet;
                Packet revertedPacket = protectedPacket.revertToOriginal();
                getStorage().set(i, revertedPacket);
            } else if (packet.getPacketType() != null && packet.getPacketType().isProtected()) {
                packet.convertFromProtected();
            }
        }

        // Revert packets in input ports
        for (Port port : getInputPorts()) {
            Packet packet = port.getCurrentPacket();
            if (packet instanceof ProtectedPacket) {
                ProtectedPacket protectedPacket = (ProtectedPacket) packet;
                port.setCurrentPacket(protectedPacket.revertToOriginal());
            } else if (packet != null && packet.getPacketType() != null && packet.getPacketType().isProtected()) {
                packet.convertFromProtected();
            }
        }

        // Revert packets in output ports
        for (Port port : getOutputPorts()) {
            Packet packet = port.getCurrentPacket();
            if (packet instanceof ProtectedPacket) {
                ProtectedPacket protectedPacket = (ProtectedPacket) packet;
                port.setCurrentPacket(protectedPacket.revertToOriginal());
            } else if (packet != null && packet.getPacketType() != null && packet.getPacketType().isProtected()) {
                packet.convertFromProtected();
            }
        }
    }

    @Override
    public void fail() {
        super.fail();
        revertProtectedPackets();
    }
}
