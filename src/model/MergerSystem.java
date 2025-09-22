package model;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class MergerSystem extends System {
    private Map<String, List<Packet>> bitPacketGroups;

    public MergerSystem() {
        super();
        setSystemType(SystemType.MERGER);
        this.bitPacketGroups = new HashMap<>();
    }

    public MergerSystem(Point2D position) {
        super(position, SystemType.MERGER);
        this.bitPacketGroups = new HashMap<>();
    }

    @Override
    public void processPacket(Packet packet) {
        // Check if it's a bit packet
        if (packet.getPacketType() != null && packet.getPacketType().isBitPacket()) {
            processBitPacket(packet);
        } else {
            // Process normally for non-bit packets
            super.processPacket(packet, null); // No entry port info for MergerSystem
        }
    }

    private void processBitPacket(Packet bitPacket) {
        // Cast to BitPacket to access specific methods
        BitPacket bp = (BitPacket) bitPacket;
        String bulkId = bp.getParentBulkPacketId();
        if (bulkId == null) {
            // Not a valid bit packet, process normally
            super.processPacket(bitPacket);
            return;
        }

        // Add to group
        bitPacketGroups.computeIfAbsent(bulkId, k -> new ArrayList<>()).add(bitPacket);

        // Check if group is complete (all bit packets collected)
        List<Packet> group = bitPacketGroups.get(bulkId);
        if (isGroupComplete(group, bulkId)) {
            reassembleBulkPacket(group, bulkId);
            bitPacketGroups.remove(bulkId);
        }
    }

    private boolean isGroupComplete(List<Packet> group, String bulkId) {
        if (group == null || group.isEmpty()) return false;

        // Count active bit packets
        int activeCount = 0;
        for (Packet packet : group) {
            if (packet.isActive()) {
                activeCount++;
            }
        }

        // We need to determine the original bulk packet size
        // For now, we'll assume a group is complete when we have at least 8 bit packets
        // This could be improved by storing the original size in the bit packets
        return activeCount >= 8; // Minimum bulk packet size (BULK_SMALL)
    }

    private void reassembleBulkPacket(List<Packet> group, String bulkId) {
        // Count active bit packets to determine bulk packet type
        int activeCount = 0;
        Vec2D movementVector = new Vec2D(1, 0); // Default
        Point2D position = getPosition();
        
        for (Packet packet : group) {
            if (packet.isActive()) {
                activeCount++;
                // Use the movement vector and position from the first active bit packet
                if (activeCount == 1) {
                    movementVector = packet.getMovementVector();
                    position = packet.getCurrentPosition();
                }
            }
        }

        // Determine bulk packet type based on bit packet count
        PacketType bulkType = (activeCount >= 10) ? PacketType.BULK_LARGE : PacketType.BULK_SMALL;

        // Create a new bulk packet
        BulkPacket bulkPacket = new BulkPacket(bulkType, position, movementVector);

        // Try to send the reassembled bulk packet using proper port selection
        Port availablePort = findAvailableOutputPort(bulkPacket);
        boolean sent = false;
        
        if (availablePort != null) {
            availablePort.acceptPacket(bulkPacket);
            sent = true;
            
            // Apply exit speed doubling if packet is exiting through incompatible port
            boolean isCompatible = availablePort.isCompatibleWithPacket(bulkPacket);
            if (!isCompatible) {
                // BulkPackets don't have exit speed multiplier methods
                // This is handled by their own movement logic
            }
        }

        if (!sent) {
            // Store in unlimited storage if no output port available
            getStorage().add(bulkPacket);
        }

        // Remove bit packets from the group
        for (Packet bitPacket : group) {
            bitPacket.setActive(false);
        }
    }

    public int getBitPacketGroupCount() {
        return bitPacketGroups.size();
    }
}

