package model;

/**
 * A concrete implementation of System for normal network systems.
 * Behaves as a standard system that processes packets normally.
 */
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
        // Normal systems process packets without special effects or type conversion
        // Packets maintain their original type when passing through normal systems
        super.processPacket(packet);
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
