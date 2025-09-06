package model;

/**
 * Represents a bend point on a wire that can be moved to navigate around obstacles.
 * Each bend costs 1 coin to create and can be moved within a specific radius.
 */
public class WireBend {
    private Point2D position;
    private double maxMoveRadius;
    private boolean isMovable;

    public WireBend() {
        this(new Point2D(0, 0), 50.0);
    }

    public WireBend(Point2D position, double maxMoveRadius) {
        this.position = position;
        this.maxMoveRadius = maxMoveRadius;
        this.isMovable = true;
    }

    public Point2D getPosition() {
        return position;
    }

    public void setPosition(Point2D position) {
        this.position = position;
    }

    public double getMaxMoveRadius() {
        return maxMoveRadius;
    }

    public void setMaxMoveRadius(double maxMoveRadius) {
        this.maxMoveRadius = maxMoveRadius;
    }

    public boolean isMovable() {
        return isMovable;
    }

    public void setMovable(boolean movable) {
        isMovable = movable;
    }

    /**
     * Moves the bend to a new position within the allowed radius.
     */
    public boolean moveTo(Point2D newPosition, Point2D originalPosition) {
        if (!isMovable) return false;

        double distance = originalPosition.distanceTo(newPosition);
        if (distance <= maxMoveRadius) {
            position = newPosition;
            return true;
        }
        return false;
    }

    /**
     * Checks if a position is within the move radius of the original position.
     */
    public boolean isWithinMoveRadius(Point2D newPosition, Point2D originalPosition) {
        double distance = originalPosition.distanceTo(newPosition);
        return distance <= maxMoveRadius;
    }

    @Override
    public String toString() {
        return "WireBend{" +
                "position=" + position +
                ", maxMoveRadius=" + maxMoveRadius +
                ", movable=" + isMovable +
                '}';
    }
}
