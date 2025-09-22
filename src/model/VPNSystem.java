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
        processPacket(packet, null);
    }
    
    @Override
    public void processPacket(Packet packet, Port entryPort) {
        // VPN systems ONLY affect messenger packets (پیام‌رسان)
        // They act like normal systems for confidential and bulk packets
        if (packet.getPacketType() != null && packet.getPacketType().isMessenger()) {
            // Convert messenger packets to protected packets
            packet.convertToProtected();
        }

        // Process normally after conversion (or for non-messenger packets)
        super.processPacket(packet, entryPort);
    }


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
                Packet revertedPacket = protectedPacket.revertToOriginal();
                port.setCurrentPacket(revertedPacket);
            } else if (packet != null && packet.getPacketType() != null && packet.getPacketType().isProtected()) {
                packet.convertFromProtected();
            }
        }

        // Revert packets in output ports
        for (Port port : getOutputPorts()) {
            Packet packet = port.getCurrentPacket();
            if (packet instanceof ProtectedPacket) {
                ProtectedPacket protectedPacket = (ProtectedPacket) packet;
                Packet revertedPacket = protectedPacket.revertToOriginal();
                port.setCurrentPacket(revertedPacket);
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
