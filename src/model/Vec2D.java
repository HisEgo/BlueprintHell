package model;

import java.util.Objects;

/**
 * Represents a 2D vector with x and y components.
 * POJO class for serialization support.
 */
public class Vec2D {
    private double x;
    private double y;

    public Vec2D() {
        this(0.0, 0.0);
    }

    public Vec2D(double x, double y) {
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

    public double magnitude() {
        return Math.sqrt(x * x + y * y);
    }

    public Vec2D normalize() {
        double mag = magnitude();
        if (mag == 0) return new Vec2D(0, 0);
        return new Vec2D(x / mag, y / mag);
    }

    public Vec2D scale(double factor) {
        return new Vec2D(x * factor, y * factor);
    }

    public Vec2D add(Vec2D other) {
        return new Vec2D(this.x + other.x, this.y + other.y);
    }

    public Vec2D subtract(Vec2D other) {
        return new Vec2D(this.x - other.x, this.y - other.y);
    }

    public double dot(Vec2D other) {
        return this.x * other.x + this.y * other.y;
    }

    public Vec2D limit(double maxMagnitude) {
        double mag = magnitude();
        if (mag <= maxMagnitude) return this;
        return normalize().scale(maxMagnitude);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Vec2D vec2D = (Vec2D) obj;
        return Double.compare(vec2D.x, x) == 0 && Double.compare(vec2D.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Vec2D{" + "x=" + x + ", y=" + y + '}';
    }
}
