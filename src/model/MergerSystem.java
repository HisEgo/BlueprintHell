package model;

import java.util.List;
import java.util.ArrayList;

public class MergerSystem extends System {

    public MergerSystem() {
        super();
        setSystemType(SystemType.MERGER);
    }

    public MergerSystem(Point2D position) {
        super(position, SystemType.MERGER);
    }

    @Override
    public void processPacket(Packet packet) {
        processPacket(packet, null);
    }
    
    @Override
    public void processPacket(Packet packet, Port entryPort) {
        // Merger systems convert bit packets to messenger packets
        if (packet instanceof BitPacket) {
            BitPacket bitPacket = (BitPacket) packet;
            // Convert bit packet to appropriate messenger packet based on color
            PacketType targetType = getMessengerTypeFromColor(bitPacket.getColorIndex());
            if (targetType != null) {
                // Create new messenger packet of the target type
                Packet messengerPacket = createMessengerPacket(targetType, bitPacket);
                // Process the converted packet
                super.processPacket(messengerPacket, entryPort);
            } else {
                // If no target type, process normally
                super.processPacket(packet, entryPort);
            }
        } else {
            // Process normally for non-bit packets
            super.processPacket(packet, entryPort);
        }
    }

    private Packet createMessengerPacket(PacketType targetType, BitPacket bitPacket) {
        Packet messengerPacket;
        switch (targetType) {
            case SMALL_MESSENGER:
                messengerPacket = new MessengerPacket(PacketType.SMALL_MESSENGER, 
                    bitPacket.getCurrentPosition(), bitPacket.getMovementVector());
                break;
            case SQUARE_MESSENGER:
                messengerPacket = new MessengerPacket(PacketType.SQUARE_MESSENGER, 
                    bitPacket.getCurrentPosition(), bitPacket.getMovementVector());
                break;
            case TRIANGLE_MESSENGER:
                messengerPacket = new MessengerPacket(PacketType.TRIANGLE_MESSENGER, 
                    bitPacket.getCurrentPosition(), bitPacket.getMovementVector());
                break;
            default:
                // Default to small messenger
                messengerPacket = new MessengerPacket(PacketType.SMALL_MESSENGER, 
                    bitPacket.getCurrentPosition(), bitPacket.getMovementVector());
                break;
        }
        return messengerPacket;
    }

    private PacketType getMessengerTypeFromColor(int colorIndex) {
        // Map color indices to messenger packet types
        switch (colorIndex % 3) {
            case 0: return PacketType.SMALL_MESSENGER;
            case 1: return PacketType.SQUARE_MESSENGER;
            case 2: return PacketType.TRIANGLE_MESSENGER;
            default: return PacketType.SMALL_MESSENGER;
        }
    }

    @Override
    public int getCoinValue() {
        // Merger systems give standard coin value based on stored packets
        int totalValue = 0;
        for (Packet packet : getStorage()) {
            totalValue += packet.getCoinValue();
        }
        return totalValue;
    }

    public List<Packet> mergeBitPackets(List<BitPacket> bitPackets) {
        List<Packet> mergedPackets = new ArrayList<>();
        
        // Group bit packets by their target messenger type
        java.util.Map<PacketType, List<BitPacket>> groupedBits = new java.util.HashMap<>();
        
        for (BitPacket bitPacket : bitPackets) {
            PacketType targetType = getMessengerTypeFromColor(bitPacket.getColorIndex());
            if (targetType != null) {
                groupedBits.computeIfAbsent(targetType, k -> new ArrayList<>()).add(bitPacket);
            }
        }
        
        // Create messenger packets from grouped bit packets
        for (java.util.Map.Entry<PacketType, List<BitPacket>> entry : groupedBits.entrySet()) {
            PacketType targetType = entry.getKey();
            List<BitPacket> bits = entry.getValue();
            
            if (!bits.isEmpty()) {
                // Use the first bit packet's position and movement
                BitPacket firstBit = bits.get(0);
                Packet messengerPacket = createMessengerPacket(targetType, firstBit);
                
                // Set the size based on the number of bit packets
                messengerPacket.setSize(bits.size());
                
                mergedPackets.add(messengerPacket);
            }
        }
        
        return mergedPackets;
    }
}
