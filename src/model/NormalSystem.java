package model;

public class NormalSystem extends System {

    public NormalSystem() {
        super();
        setSystemType(SystemType.NORMAL);
    }

    public NormalSystem(Point2D position) {
        super(position, SystemType.NORMAL);
    }

    public NormalSystem(Point2D position, SystemType systemType) {
        super(position, systemType);
    }

    @Override
    public void processPacket(Packet packet) {
        processPacket(packet, null);
    }
    
    @Override
    public void processPacket(Packet packet, Port entryPort) {
        // Normal systems process packets without special effects or type conversion
        // Packets maintain their original type when passing through normal systems
        super.processPacket(packet, entryPort);
    }

    @Override
    public int getCoinValue() {
        // Normal systems give standard coin value based on stored packets
        int totalValue = 0;
        for (Packet packet : getStorage()) {
            totalValue += packet.getCoinValue();
        }
        return totalValue;
    }
}

