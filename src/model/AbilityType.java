package model;

public enum AbilityType {
    // Phase 1 Abilities
    O_ATAR("O' Atar", 3, "Disable shockwaves for 10s"),
    O_AIRYAMAN("O' Airyaman", 4, "Disable collisions for 5s"),
    O_ANAHITA("O' Anahita", 5, "Set all packet noise to 0"),

    // Phase 2 Abilities
    SCROLL_OF_AERGIA("Scroll of Aergia", 10, "Sets packet acceleration to zero"),
    SCROLL_OF_SISYPHUS("Scroll of Sisyphus", 15, "Move non-reference systems"),
    SCROLL_OF_ELIPHAS("Scroll of Eliphas", 20, "Realign packet centers");

    private final String displayName;
    private final int cost;
    private final String description;

    AbilityType(String displayName, int cost, String description) {
        this.displayName = displayName;
        this.cost = cost;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getCost() {
        return cost;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPhase1Ability() {
        return this == O_ATAR || this == O_AIRYAMAN || this == O_ANAHITA;
    }

    public boolean isPhase2Ability() {
        return this == SCROLL_OF_AERGIA || this == SCROLL_OF_SISYPHUS || this == SCROLL_OF_ELIPHAS;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

