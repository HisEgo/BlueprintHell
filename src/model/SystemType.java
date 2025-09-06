package model;

public enum SystemType {
    // Phase 1 Systems
    REFERENCE("Reference System"),
    NORMAL("Normal System"),

    // Phase 2 Systems
    SPY("Spy System"),
    SABOTEUR("Saboteur System"),
    VPN("VPN System"),
    ANTI_TROJAN("Anti-Trojan System"),
    DISTRIBUTOR("Distributor System"),
    MERGER("Merger System");

    private final String displayName;

    SystemType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this system type is a reference system.
     */
    public boolean isReference() {
        return this == REFERENCE;
    }

    /**
     * Checks if this system type is a spy system.
     */
    public boolean isSpy() {
        return this == SPY;
    }

    /**
     * Checks if this system type is a saboteur system.
     */
    public boolean isSaboteur() {
        return this == SABOTEUR;
    }

    /**
     * Checks if this system type is a VPN system.
     */
    public boolean isVPN() {
        return this == VPN;
    }

    /**
     * Checks if this system type is an anti-trojan system.
     */
    public boolean isAntiTrojan() {
        return this == ANTI_TROJAN;
    }

    /**
     * Checks if this system type is a distributor system.
     */
    public boolean isDistributor() {
        return this == DISTRIBUTOR;
    }

    /**
     * Checks if this system type is a merger system.
     */
    public boolean isMerger() {
        return this == MERGER;
    }

    /**
     * Checks if this system type can be moved by the player.
     */
    public boolean isMovable() {
        return this != REFERENCE; // Only reference systems cannot be moved
    }

    @Override
    public String toString() {
        return displayName;
    }
}
