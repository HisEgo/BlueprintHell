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

    public boolean isReference() {
        return this == REFERENCE;
    }

    public boolean isSpy() {
        return this == SPY;
    }

    public boolean isSaboteur() {
        return this == SABOTEUR;
    }

    public boolean isVPN() {
        return this == VPN;
    }

    public boolean isAntiTrojan() {
        return this == ANTI_TROJAN;
    }

    public boolean isDistributor() {
        return this == DISTRIBUTOR;
    }

    public boolean isMerger() {
        return this == MERGER;
    }

    public boolean isMovable() {
        return this != REFERENCE; // Only reference systems cannot be moved
    }

    @Override
    public String toString() {
        return displayName;
    }
}

