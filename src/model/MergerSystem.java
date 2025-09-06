package model;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Merger system that reassembles bit packets into bulk packets.
 * Behaves like a normal system for non-bit packets.
 */
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
            super.processPacket(packet);
        }
    }

    /**
     * Processes a bit packet by adding it to a group and checking for completion.
     */
    private void processBitPacket(Packet bitPacket) {
        String bulkId = bitPacket.getBulkPacketId();
        if (bulkId == null) {
            // Not a valid bit packet, process normally
            super.processPacket(bitPacket);
            return;
        }

        // Add to group
        bitPacketGroups.computeIfAbsent(bulkId, k -> new ArrayList<>()).add(bitPacket);

        // Check if group is complete (all bit packets collected)
        List<Packet> group = bitPacketGroups.get(bulkId);
        if (isGroupComplete(group)) {
            reassembleBulkPacket(group, bulkId);
            bitPacketGroups.remove(bulkId);
        }
    }

    /**
     * Checks if a bit packet group is complete.
     */
    private boolean isGroupComplete(List<Packet> group) {
        if (group == null || group.isEmpty()) return false;

        // Count active bit packets
        int activeCount = 0;
        for (Packet packet : group) {
            if (packet.isActive()) {
                activeCount++;
            }
        }

        // Group is complete if we have enough active bit packets
        // (assuming the original bulk packet size)
        return activeCount >= 8; // Minimum bulk packet size
    }

    /**
     * Reassembles bit packets into a bulk packet.
     */
    private void reassembleBulkPacket(List<Packet> group, String bulkId) {
        // Create a new bulk packet
        BulkPacket bulkPacket = new BulkPacket(
                PacketType.BULK_SMALL,
                getPosition(),
                new Vec2D(1, 0) // Default movement vector
        );

        // Try to send the reassembled bulk packet
        boolean sent = false;
        for (Port port : getOutputPorts()) {
            if (port.isEmpty()) {
                port.acceptPacket(bulkPacket);
                sent = true;
                break;
            }
        }

        if (!sent && hasStorageSpace()) {
            getStorage().add(bulkPacket);
        } else if (!sent) {
            bulkPacket.setActive(false);
        }

        // Remove bit packets from the group
        for (Packet bitPacket : group) {
            bitPacket.setActive(false);
        }
    }

    /**
     * Gets the number of bit packet groups being tracked.
     */
    public int getBitPacketGroupCount() {
        return bitPacketGroups.size();
    }
}
