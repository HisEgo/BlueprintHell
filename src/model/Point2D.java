package model;

import java.util.Objects;

/**
 * Represents a 2D point with x and y coordinates.
 * POJO class for serialization support.
 */
public class Point2D {
    private double x;
    private double y;

    public Point2D() {
        this(0.0, 0.0);
    }

    public Point2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double distanceTo(Point2D other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public Point2D add(Vec2D vector) {
        return new Point2D(this.x + vector.getX(), this.y + vector.getY());
    }

    public Point2D subtract(Point2D other) {
        return new Point2D(this.x - other.x, this.y - other.y);
    }

    /**
     * Converts this point to a normalized vector.
     */
    public Vec2D normalize() {
        double magnitude = Math.sqrt(x * x + y * y);
        if (magnitude == 0) {
            return new Vec2D(0, 0);
        }
        return new Vec2D(x / magnitude, y / magnitude);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Point2D point2D = (Point2D) obj;
        return Double.compare(point2D.x, x) == 0 && Double.compare(point2D.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Point2D{" + "x=" + x + ", y=" + y + '}';
    }
}
