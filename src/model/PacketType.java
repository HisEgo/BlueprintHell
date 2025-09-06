package model;

public enum PacketType {
    // Messenger Packets (Phase 1) - Fixed coin values per Phase 2 spec
    SQUARE_MESSENGER("Square Messenger", 2, 2),
    TRIANGLE_MESSENGER("Triangle Messenger", 3, 3),

    // Small hexagon messenger packet (Phase 2)
    SMALL_MESSENGER("Small Messenger", 1, 1),

    // Protected Packets
    PROTECTED("Protected", 0, 5), // Size will be set dynamically

    // Confidential Packets
    CONFIDENTIAL("Confidential", 4, 3),
    CONFIDENTIAL_PROTECTED("Protected Confidential", 6, 4),

    // Bulk Packets
    BULK_SMALL("Small Bulk", 8, 8),
    BULK_LARGE("Large Bulk", 10, 10),

    // Special Packets
    TROJAN("Trojan", 2, 0),
    BIT_PACKET("Bit Packet", 1, 0);

    private final String displayName;
    private final int baseSize;
    private final int baseCoinValue;

    PacketType(String displayName, int baseSize, int baseCoinValue) {
        this.displayName = displayName;
        this.baseSize = baseSize;
        this.baseCoinValue = baseCoinValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getBaseSize() {
        return baseSize;
    }

    public int getBaseCoinValue() {
        return baseCoinValue;
    }

    /**
     * Checks if this packet type is a messenger packet.
     */
    public boolean isMessenger() {
        return this == SMALL_MESSENGER || this == SQUARE_MESSENGER ||
                this == TRIANGLE_MESSENGER;
    }

    /**
     * Checks if this packet type is a protected packet.
     */
    public boolean isProtected() {
        return this == PROTECTED || this == CONFIDENTIAL_PROTECTED;
    }

    /**
     * Checks if this packet type is a confidential packet.
     */
    public boolean isConfidential() {
        return this == CONFIDENTIAL || this == CONFIDENTIAL_PROTECTED;
    }

    /**
     * Checks if this packet type is a bulk packet.
     */
    public boolean isBulk() {
        return this == BULK_SMALL || this == BULK_LARGE;
    }

    /**
     * Checks if this packet type is a bit packet.
     */
    public boolean isBitPacket() {
        return this == BIT_PACKET;
    }

    /**
     * Checks if this packet type is a trojan packet.
     */
    public boolean isTrojan() {
        return this == TROJAN;
    }

    /**
     * Gets the original messenger type for protected packets.
     */
    public PacketType getOriginalType() {
        if (this == PROTECTED) {
            // Return a random messenger type
            PacketType[] messengerTypes = {SMALL_MESSENGER, SQUARE_MESSENGER, TRIANGLE_MESSENGER};
            return messengerTypes[(int)(Math.random() * messengerTypes.length)];
        } else if (this == CONFIDENTIAL_PROTECTED) {
            return CONFIDENTIAL;
        }
        return this;
    }

    /**
     * Converts a protected packet back to its original type.
     */
    public PacketType convertFromProtected() {
        if (this == PROTECTED) {
            return getOriginalType();
        } else if (this == CONFIDENTIAL_PROTECTED) {
            return CONFIDENTIAL;
        }
        return this;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
