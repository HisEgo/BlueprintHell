package model;

public enum PortShape {
    SQUARE,
    TRIANGLE,
    HEXAGON;

    public boolean isCompatibleWith(PortShape other) {
        return true; // All shapes are now compatible
    }

    public int getSize() {
        return switch (this) {
            case SQUARE -> 2;
            case TRIANGLE -> 3;
            case HEXAGON -> 1;
        };
    }

    public int getCoinValue() {
        return switch (this) {
            case SQUARE -> 2;      // Square packets give 2 coins
            case TRIANGLE -> 3;    // Triangle packets give 3 coins  
            case HEXAGON -> 1;     // Small hexagon packets give 1 coin
        };
    }
}

