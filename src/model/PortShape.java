package model;

/**
 * Enum representing the shape of a port.
 * Used for packet compatibility checking.
 */
public enum PortShape {
    SQUARE,
    TRIANGLE,
    HEXAGON;

    /**
     * Checks if this shape is compatible with another shape for wire connections.
     * Updated: All shapes can now connect to each other (SQUARE ↔ TRIANGLE allowed)
     */
    public boolean isCompatibleWith(PortShape other) {
        return true; // All shapes are now compatible
    }

    /**
     * Gets the size value for this shape.
     * Used for packet size calculations.
     */
    public int getSize() {
        return switch (this) {
            case SQUARE -> 2;
            case TRIANGLE -> 3;
            case HEXAGON -> 1;
        };
    }

    /**
     * Gets the coin value for this shape when a packet enters a system.
     */
    public int getCoinValue() {
        return switch (this) {
            case SQUARE -> 2;      // Square packets give 2 coins
            case TRIANGLE -> 3;    // Triangle packets give 3 coins  
            case HEXAGON -> 1;     // Small hexagon packets give 1 coin
        };
    }
}
