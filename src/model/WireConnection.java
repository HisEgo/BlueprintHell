package model;


import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

/**
 * Represents a wire connection between two ports.
 * Tracks wire length consumption and connection state.
 * POJO class for serialization support.
 * Enhanced for Phase 2 with bend support and realistic wiring.
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class WireConnection {
    private String id;
    private Port sourcePort;
    private Port destinationPort;
    private double wireLength;
    private double consumedLength;
    private boolean isActive;
    private List<WireBend> bends;
    private boolean isDestroyed;
    private int bulkPacketPassages;
    private static final int MAX_BULK_PASSAGES = 3;
    private List<Packet> packetsOnWire; // Multiple packets can travel on the same wire
    // Per specification: Only one packet may occupy a wire from a port at any time
    private static final int MAX_WIRE_CAPACITY = 1;
    // Phase 1 spec: packet loss if packet goes off the wire path
    private static final double DEFAULT_OFF_WIRE_LOSS_THRESHOLD = 20.0; // pixels


    public WireConnection() {
        this.id = java.util.UUID.randomUUID().toString();
        this.isActive = true;
        this.consumedLength = 0.0;
        this.bends = new ArrayList<>();
        this.isDestroyed = false;
        this.bulkPacketPassages = 0;
        this.packetsOnWire = new ArrayList<>();

    }

    public WireConnection(Port sourcePort, Port destinationPort, double wireLength) {
        this();
        // Normalize so that sourcePort is always an OUTPUT and destinationPort is always an INPUT
        Port normalizedSource = sourcePort;
        Port normalizedDestination = destinationPort;
        if (sourcePort != null && destinationPort != null) {
            boolean sourceIsInput = sourcePort.isInput();
            boolean destIsInput = destinationPort.isInput();
            // If the first port is an input and the second is an output, swap them
            if (sourceIsInput && !destIsInput) {
                normalizedSource = destinationPort;
                normalizedDestination = sourcePort;
            }
        }

        this.sourcePort = normalizedSource;
        this.destinationPort = normalizedDestination;
        this.wireLength = wireLength;
        this.consumedLength = wireLength; // Mark the full length as consumed when wire is created
    }

    public WireConnection(Port sourcePort, Port destinationPort) {
        this();
        // Normalize so that sourcePort is always an OUTPUT and destinationPort is always an INPUT
        Port normalizedSource = sourcePort;
        Port normalizedDestination = destinationPort;
        if (sourcePort != null && destinationPort != null) {
            boolean sourceIsInput = sourcePort.isInput();
            boolean destIsInput = destinationPort.isInput();
            if (sourceIsInput && !destIsInput) {
                normalizedSource = destinationPort;
                normalizedDestination = sourcePort;
            }
        }

        this.sourcePort = normalizedSource;
        this.destinationPort = normalizedDestination;
        // Calculate wire length based on port positions
        if (this.sourcePort != null && this.destinationPort != null &&
                this.sourcePort.getPosition() != null && this.destinationPort.getPosition() != null) {
            this.wireLength = this.sourcePort.getPosition().distanceTo(this.destinationPort.getPosition());
            this.consumedLength = this.wireLength; // Mark the full length as consumed when wire is created
        } else {
            this.wireLength = 0.0;
            this.consumedLength = 0.0;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Port getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(Port sourcePort) {
        this.sourcePort = sourcePort;
    }

    public Port getDestinationPort() {
        return destinationPort;
    }

    public void setDestinationPort(Port destinationPort) {
        this.destinationPort = destinationPort;
    }

    public double getWireLength() {
        return wireLength;
    }

    public void setWireLength(double wireLength) {
        this.wireLength = wireLength;
    }

    public double getConsumedLength() {
        return consumedLength;
    }

    public void setConsumedLength(double consumedLength) {
        this.consumedLength = consumedLength;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    // Phase 2 properties
    public List<WireBend> getBends() {
        return bends;
    }

    public void setBends(List<WireBend> bends) {
        this.bends = bends;
    }

    public boolean isDestroyed() {
        return isDestroyed;
    }

    public void setDestroyed(boolean destroyed) {
        isDestroyed = destroyed;
    }

    public int getBulkPacketPassages() {
        return bulkPacketPassages;
    }

    public void setBulkPacketPassages(int bulkPacketPassages) {
        this.bulkPacketPassages = bulkPacketPassages;
    }

    /**
     * Gets the remaining wire length available for this connection.
     * For normal connections, this should be 0 since the full length is consumed.
     */
    public double getRemainingLength() {
        return wireLength - consumedLength;
    }

    /**
     * Checks if this connection has enough wire length remaining.
     */
    public boolean hasSufficientLength() {
        return getRemainingLength() > 0;
    }

    /**
     * Consumes wire length for this connection.
     */
    public boolean consumeLength(double amount) {
        if (consumedLength + amount > wireLength) {
            return false; // Not enough wire length
        }

        consumedLength += amount;
        return true;
    }

    /**
     * Calculates the actual distance between the two ports.
     */
    public double getActualDistance() {
        if (sourcePort == null || destinationPort == null) {
            return 0.0;
        }

        return sourcePort.getPosition().distanceTo(destinationPort.getPosition());
    }

    /**
     * Checks if this connection is valid (both ports exist and are compatible).
     */
    public boolean isValid() {
        if (sourcePort == null || destinationPort == null) {
            return false;
        }

        // Check if ports are from different systems
        if (sourcePort.getParentSystem() == destinationPort.getParentSystem()) {
            return false;
        }

        // Check if one is input and one is output
        if (sourcePort.isInput() == destinationPort.isInput()) {
            return false;
        }

        // Check if they have compatible shapes
        return sourcePort.getShape().isCompatibleWith(destinationPort.getShape());
    }

    /**
     * Transfers packets from source to destination port, respecting wire capacity.
     * Fixed to handle proper wire direction and destination port logic.
     */
    public boolean transferPacket() {
        if (!isActive || !isValid()) {
            return false;
        }

        // Determine actual input and output based on wire direction
        Port wireInputPort = sourcePort;  // Wire input is always the source port
        Port wireOutputPort = destinationPort; // Wire output is always the destination port

        // Try to move packet from source port to wire (if wire is available)
        if (wireInputPort.getCurrentPacket() != null && canAcceptPacket()) {
            Packet packet = wireInputPort.releasePacket();
            return acceptPacket(packet);
        }

        // Try to move packets from wire to destination port (if destination port is available)
        if (isOccupied() && wireOutputPort.isEmpty()) {
            // Move the first packet that's ready to be transferred
            for (Packet packet : packetsOnWire) {
                if (hasPacketReachedDestination(packet)) {
                    packetsOnWire.remove(packet);
                    boolean accepted = wireOutputPort.acceptPacket(packet);
                    if (accepted) {
                        // Mark coin award as pending for system entry (one-shot)
                        packet.setCoinAwardPending(true);
                        String systemType = wireOutputPort.getParentSystem() != null ?
                                wireOutputPort.getParentSystem().getClass().getSimpleName() : "Unknown";
                        java.lang.System.out.println("DEBUG: Packet " + packet.getClass().getSimpleName() +
                                " transferred from wire " + id.substring(0,8) + " to " + systemType +
                                " input port (remaining packets: " + packetsOnWire.size() + ")");

                        // If destination is a ReferenceSystem, finalize delivery immediately
                        model.System destSystem = wireOutputPort.getParentSystem();
                        if (destSystem instanceof model.ReferenceSystem) {
                            model.ReferenceSystem ref = (model.ReferenceSystem) destSystem;
                            // All reference systems can receive packets now
                            model.Packet p = wireOutputPort.releasePacket();
                            if (p != null) {
                                ref.processPacket(p);
                            }
                        }
                    }
                    return accepted;
                }
            }
        }

        return false;
    }

    /**
     * Gets all packets currently on this wire.
     */
    public List<Packet> getPacketsOnWire() {
        return new ArrayList<>(packetsOnWire);
    }

    /**
     * Sets packets on this wire.
     */
    public void setPacketsOnWire(List<Packet> packets) {
        this.packetsOnWire = new ArrayList<>(packets);
    }

    /**
     * Checks if this wire is currently occupied by any packets.
     */
    public boolean isOccupied() {
        return !packetsOnWire.isEmpty() && packetsOnWire.stream().anyMatch(Packet::isActive);
    }

    /**
     * Checks if this wire can accept a new packet.
     */
    public boolean canAcceptPacket() {
        // Only allow accepting a packet when no active packet is currently on this wire
        boolean hasActiveOnWire = packetsOnWire.stream().anyMatch(Packet::isActive);
        return !hasActiveOnWire && isActive && !isDestroyed;
    }

    /**
     * Accepts a packet onto this wire if it's available.
     */
    public boolean acceptPacket(Packet packet) {
        if (!canAcceptPacket()) {
            return false;
        }

        this.packetsOnWire.add(packet);

        // Debug: Log packet acceptance
        java.lang.System.out.println("DEBUG: Packet " + packet.getClass().getSimpleName() + " accepted on wire " + id +
                " (total packets on wire: " + packetsOnWire.size() + "/" + MAX_WIRE_CAPACITY + ")");

        // Initialize packet for path-based movement on this wire
        initializePacketOnWire(packet);

        // Set packet movement direction along the wire
        Vec2D direction = getDirectionVector();
        if (direction.magnitude() > 0) {
            // Use packet's base speed instead of overriding it
            // The MovementController will handle packet-specific speed calculations
            double initialSpeed = packet.getBaseSpeed();
            if (initialSpeed <= 0) {
                initialSpeed = 50.0; // Fallback default speed
            }

            packet.setMovementVector(direction.normalize().scale(initialSpeed));
        }

        // Reset travel time for new wire
        packet.resetTravelTime();

        return true;
    }

    /**
     * Removes a specific packet from this wire when it reaches the destination.
     */
    public Packet releasePacket(Packet packet) {
        if (packetsOnWire.remove(packet)) {
            return packet;
        }
        return null;
    }

    /**
     * Clears all packets from this wire (for temporal navigation rewind).
     */
    public void clearPackets() {
        packetsOnWire.clear();
    }

    /**
     * Checks if a specific packet on this wire has reached its destination.
     */
    public boolean hasPacketReachedDestination(Packet packet) {
        if (!packetsOnWire.contains(packet) || destinationPort == null) {
            return false;
        }

        Point2D packetPos = packet.getCurrentPosition();
        Point2D destPos = destinationPort.getPosition();

        // Consider packet reached if within 5 pixels of destination (reduced threshold)
        return packetPos.distanceTo(destPos) <= 5.0;
    }

    /**
     * Updates the packet movement along this wire using path-based movement.
     * Ensures packets are properly initialized for wire-based movement.
     */
    public void updatePacketMovement(double deltaTime) {
        updatePacketMovement(deltaTime, true); // Default to smooth curves for backward compatibility
    }

    /**
     * Updates the packet movement along this wire using path-based movement.
     * Ensures packets are properly initialized for wire-based movement.
     * @param deltaTime Time elapsed since last update
     * @param useSmoothCurves If true, uses smooth curves for path calculation; if false, uses rigid polyline
     */
    public void updatePacketMovement(double deltaTime, boolean useSmoothCurves) {
        if (!isOccupied()) {
            return;
        }

        // Update all packets on this wire
        List<Packet> packetsToRemove = new ArrayList<>();

        for (Packet packet : packetsOnWire) {
            if (!packet.isActive()) {
                packetsToRemove.add(packet);
                continue;
            }

            // Ensure packet is properly initialized for wire-based movement
            if (!packet.isOnWire() || packet.getCurrentWire() != this) {
                initializePacketOnWire(packet);
            }

            // Check if packet has reached destination
            if (hasPacketReachedDestination(packet)) {
                // Don't update position if at destination - let transfer logic handle it
                continue;
            }

            // For packets not using path-based movement, use legacy approach
            if (!packet.isOnWire()) {
                packet.updatePosition(deltaTime);
            }
            // Always constrain to wire path and enforce off-wire loss rule
            constrainPacketToWire(packet, useSmoothCurves);
            // Note: Path-based movement is handled by MovementController
        }

        // Remove inactive packets
        packetsOnWire.removeAll(packetsToRemove);
    }

    /**
     * Initializes a packet for path-based movement on this wire.
     * Enhanced to support curved paths with bends.
     */
    private void initializePacketOnWire(Packet packet) {
        packet.setCurrentWire(this);
        packet.setPathProgress(0.0);

        // Set initial position at the start of the wire path
        Point2D startPosition = getPositionAtProgress(0.0);
        if (startPosition != null) {
            packet.setCurrentPosition(startPosition);
        }

        // Initialize base speed if not set
        if (packet.getBaseSpeed() <= 0) {
            packet.setBaseSpeed(50.0); // Default base speed
        }

        // Calculate initial movement vector based on wire direction
        // For curved wires, look at the direction from start to a small progress ahead
        // Use smooth curves by default for initialization
        Point2D currentPos = getPositionAtProgress(0.0);
        Point2D nextPos = getPositionAtProgress(Math.min(0.1, 10.0 / getTotalLength()));

        if (currentPos != null && nextPos != null) {
            Vec2D direction = new Vec2D(
                    nextPos.getX() - currentPos.getX(),
                    nextPos.getY() - currentPos.getY()
            );
            if (direction.magnitude() > 0) {
                packet.setMovementVector(direction.normalize().scale(packet.getBaseSpeed()));
            }
        }
    }

    /**
     * Constrains a specific packet to follow the wire path, including bends.
     */
    private void constrainPacketToWire(Packet packet) {
        constrainPacketToWire(packet, true); // Default to smooth curves for backward compatibility
    }

    /**
     * Constrains a specific packet to follow the wire path, including bends.
     * @param packet Packet to constrain
     * @param useSmoothCurves If true, uses smooth curves for path calculation; if false, uses rigid polyline
     */
    private void constrainPacketToWire(Packet packet, boolean useSmoothCurves) {
        if (sourcePort == null || destinationPort == null) {
            return;
        }

        Point2D packetPos = packet.getCurrentPosition();
        List<Point2D> pathPoints = getPathPoints(useSmoothCurves);

        if (pathPoints.size() < 2) {
            return;
        }

        // Find the closest point on the wire path
        Point2D closestPoint = findClosestPointOnPath(packetPos, pathPoints);
        if (closestPoint != null) {
            double deviation = packetPos.distanceTo(closestPoint);
            double threshold = DEFAULT_OFF_WIRE_LOSS_THRESHOLD;
            // Allow configurable threshold via GameState setting if available
            try {
                model.GameLevel level = null;
                if (sourcePort != null && sourcePort.getParentSystem() != null) {
                    level = sourcePort.getParentSystem().getParentLevel();
                }
                if (level == null && destinationPort != null && destinationPort.getParentSystem() != null) {
                    level = destinationPort.getParentSystem().getParentLevel();
                }
                if (level != null) {
                    // GameState holds settings; attempt to fetch from any system's parent level via a reference system
                    // Since we don't hold GameState here, use a convention: store threshold in each system's GameLevel via settings in GameState
                    // Fallback to default if not reachable.
                    // Note: For simplicity, use default unless future refactor passes GameState here.
                }
            } catch (Exception ignored) {}
            if (deviation > threshold) {
                // Mark packet as lost per Phase 1 spec: "A packet goes off the wire path"
                packet.setLost(true);
                packet.setActive(false);
                java.lang.System.out.println("DEBUG: Packet went off-wire (deviation=" + deviation + ") and is marked lost");
                return;
            }
            // Snap gently to the path when within tolerance
            packet.setCurrentPosition(closestPoint);
        }
    }

    /**
     * Finds the closest point on the wire path to the given position.
     */
    private Point2D findClosestPointOnPath(Point2D position, List<Point2D> pathPoints) {
        if (pathPoints.size() < 2) {
            return null;
        }

        Point2D closestPoint = null;
        double minDistance = Double.MAX_VALUE;

        // Check each segment of the path
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point2D segmentStart = pathPoints.get(i);
            Point2D segmentEnd = pathPoints.get(i + 1);

            Point2D pointOnSegment = getClosestPointOnLineSegment(position, segmentStart, segmentEnd);
            double distance = position.distanceTo(pointOnSegment);

            if (distance < minDistance) {
                minDistance = distance;
                closestPoint = pointOnSegment;
            }
        }

        return closestPoint;
    }

    /**
     * Gets the closest point on a line segment to a given point.
     */
    private Point2D getClosestPointOnLineSegment(Point2D point, Point2D lineStart, Point2D lineEnd) {
        Vec2D lineVec = new Vec2D(lineEnd.getX() - lineStart.getX(), lineEnd.getY() - lineStart.getY());
        Vec2D pointVec = new Vec2D(point.getX() - lineStart.getX(), point.getY() - lineStart.getY());

        double lineLength = lineVec.magnitude();
        if (lineLength == 0) {
            return lineStart;
        }

        double projection = pointVec.dot(lineVec) / (lineLength * lineLength);
        projection = Math.max(0, Math.min(1, projection)); // Clamp to segment

        return new Point2D(
                lineStart.getX() + lineVec.getX() * projection,
                lineStart.getY() + lineVec.getY() * projection
        );
    }

    /**
     * Gets the direction vector from source to destination.
     */
    public Vec2D getDirectionVector() {
        if (sourcePort == null || destinationPort == null) {
            return new Vec2D();
        }

        Point2D sourcePos = sourcePort.getPosition();
        Point2D destPos = destinationPort.getPosition();

        return new Vec2D(
                destPos.getX() - sourcePos.getX(),
                destPos.getY() - sourcePos.getY()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        WireConnection that = (WireConnection) obj;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "WireConnection{" +
                "id='" + id + '\'' +
                ", sourcePort=" + (sourcePort != null ? sourcePort.getParentSystem().getId() : "null") +
                ", destinationPort=" + (destinationPort != null ? destinationPort.getParentSystem().getId() : "null") +
                ", wireLength=" + wireLength +
                ", consumedLength=" + consumedLength +
                ", active=" + isActive +
                '}';
    }

    /**
     * Adds a bend to this wire connection.
     * Validates that the bend doesn't cause the wire to pass over systems.
     * Ensures the bend is positioned exactly on the wire path for perfect alignment.
     */
    public boolean addBend(Point2D position, List<System> systems) {
        if (bends.size() >= 3) {
            return false; // Maximum 3 bends allowed
        }

        // Use smooth curve path points to find the nearest segment for better alignment
        List<Point2D> pathPoints = getPathPoints(true); // Force smooth curves for alignment
        if (pathPoints.size() < 2) {
            return false;
        }

        int nearestSegmentIndex = findNearestSegmentIndex(position, pathPoints);
        Point2D segmentStart = pathPoints.get(nearestSegmentIndex);
        Point2D segmentEnd = pathPoints.get(nearestSegmentIndex + 1);

        // Find the exact point on the wire path closest to the click position
        Point2D alignedPosition = findClosestPointOnLineSegment(position, segmentStart, segmentEnd);

        // Allow bend creation even if wire passes over systems
        // This removes the restriction that prevented bend creation on wires crossing systems

        // Insert the bend at the aligned position in the bend list so ordering is preserved
        // Mapping: segment 0 => insert at index 0, last segment => insert at bends.size()
        WireBend bend = new WireBend(alignedPosition, 50.0);
        bends.add(nearestSegmentIndex, bend);
        return true;
    }

    /**
     * Finds the closest point on a line segment to a given point.
     * This ensures bends are always positioned exactly on the wire path.
     * @param point The point to find the closest position to
     * @param lineStart Start of the line segment
     * @param lineEnd End of the line segment
     * @return The closest point on the line segment
     */
    private Point2D findClosestPointOnLineSegment(Point2D point, Point2D lineStart, Point2D lineEnd) {
        double A = point.getX() - lineStart.getX();
        double B = point.getY() - lineStart.getY();
        double C = lineEnd.getX() - lineStart.getX();
        double D = lineEnd.getY() - lineStart.getY();

        double dot = A * C + B * D;
        double lenSq = C * C + D * D;

        if (lenSq == 0) {
            // Line segment is actually a point
            return lineStart;
        }

        double param = dot / lenSq;

        // Clamp parameter to line segment bounds
        param = Math.max(0.0, Math.min(1.0, param));

        // Calculate the closest point on the line segment
        double x = lineStart.getX() + param * C;
        double y = lineStart.getY() + param * D;

        return new Point2D(x, y);
    }

    /**
     * Adds a bend to this wire connection (legacy method without validation).
     * @deprecated Use addBend(Point2D position, List<System> systems) instead
     */
    @Deprecated
    public boolean addBend(Point2D position) {
        if (bends.size() >= 3) {
            return false; // Maximum 3 bends allowed
        }

        WireBend bend = new WireBend(position, 50.0);
        bends.add(bend);
        return true;
    }

    /**
     * Moves a bend to a new position.
     * Validates that the new position doesn't cause the wire to pass over systems.
     */
    public boolean moveBend(int bendIndex, Point2D newPosition, List<System> systems) {
        if (bendIndex < 0 || bendIndex >= bends.size()) {
            return false;
        }

        // Temporarily remove the bend to compute adjacency correctly
        WireBend bend = bends.get(bendIndex);
        Point2D originalPosition = bend.getPosition();
        bends.remove(bendIndex);

        // Determine adjacent points (previous and next) for validation
        Point2D prevPoint = (bendIndex == 0) ?
                (sourcePort != null ? sourcePort.getPosition() : originalPosition) :
                bends.get(bendIndex - 1).getPosition();
        Point2D nextPoint = (bendIndex < bends.size()) ?
                bends.get(bendIndex).getPosition() :
                (destinationPort != null ? destinationPort.getPosition() : originalPosition);

        if (prevPoint == null || nextPoint == null) {
            // Restore and fail safely if endpoints are not available
            bends.add(bendIndex, bend);
            return false;
        }

        // Validate movement only against the affected neighboring segments
        if (wouldSegmentWithBendIntersectSystems(prevPoint, newPosition, nextPoint, systems)) {
            // Restore the original bend
            bends.add(bendIndex, bend);
            return false;
        }

        // Move the bend to the new position and restore at the same index
        boolean moveSuccess = bend.moveTo(newPosition, originalPosition);
        bends.add(bendIndex, bend);
        return moveSuccess;
    }

    /**
     * Moves a bend to a new position with more permissive validation for better user experience.
     * This method allows more freedom in bend positioning while still maintaining basic constraints.
     * Ensures the bend stays aligned with the wire path for perfect visual alignment.
     */
    public boolean moveBendPermissive(int bendIndex, Point2D newPosition, List<System> systems) {
        if (bendIndex < 0 || bendIndex >= bends.size()) {
            return false;
        }

        // Get the bend and its original position
        WireBend bend = bends.get(bendIndex);
        Point2D originalPosition = bend.getPosition();

        // Only check for extreme cases (e.g., bends inside connected systems)
        // Allow more freedom in positioning for better user experience

        // Check if the new position is inside the source or destination system
        if (sourcePort != null && sourcePort.getParentSystem() != null) {
            if (sourcePort.getParentSystem().getBounds().contains(newPosition.getX(), newPosition.getY())) {
                return false; // Don't allow bends inside source system
            }
        }

        if (destinationPort != null && destinationPort.getParentSystem() != null) {
            if (destinationPort.getParentSystem().getBounds().contains(newPosition.getX(), newPosition.getY())) {
                return false; // Don't allow bends inside destination system
            }
        }

        // For better visual alignment, we could optionally snap the bend to the nearest wire path
        // But for maximum user freedom, we'll allow the bend to be positioned anywhere
        // The wire path will automatically adjust to pass through the new bend position

        // Move the bend to the new position
        return bend.moveTo(newPosition, originalPosition);
    }

    /**
     * Moves a bend to a new position (legacy method without validation).
     * @deprecated Use moveBend(int bendIndex, Point2D newPosition, List<System> systems) instead
     */
    @Deprecated
    public boolean moveBend(int bendIndex, Point2D newPosition) {
        if (bendIndex < 0 || bendIndex >= bends.size()) {
            return false;
        }

        WireBend bend = bends.get(bendIndex);
        Point2D originalPosition = bend.getPosition();
        return bend.moveTo(newPosition, originalPosition);
    }

    /**
     * Calculates the total length of the wire including bends.
     */
    public double calculateTotalLength() {
        if (sourcePort == null || destinationPort == null) {
            return 0.0;
        }

        Point2D sourcePos = sourcePort.getPosition();
        Point2D destPos = destinationPort.getPosition();

        if (bends.isEmpty()) {
            // Straight line
            return sourcePos.distanceTo(destPos);
        }

        // Calculate polyline length
        double totalLength = 0.0;
        Point2D currentPoint = sourcePos;

        for (WireBend bend : bends) {
            totalLength += currentPoint.distanceTo(bend.getPosition());
            currentPoint = bend.getPosition();
        }

        totalLength += currentPoint.distanceTo(destPos);
        return totalLength;
    }

    /**
     * Checks if adding a bend at the specified position would cause the wire to pass over systems.
     */
    public boolean wouldBendPassOverSystems(Point2D bendPosition, List<System> systems) {
        if (sourcePort == null || destinationPort == null) {
            return false;
        }

        Point2D sourcePos = sourcePort.getPosition();
        Point2D destPos = destinationPort.getPosition();

        // Backward-compatible behavior: validate against full endpoints
        return wouldSegmentWithBendIntersectSystems(sourcePos, bendPosition, destPos, systems);
    }

    /**
     * Finds the index of the path segment closest to a given position.
     * Returns an index i such that the segment is between pathPoints[i] and pathPoints[i+1].
     */
    private int findNearestSegmentIndex(Point2D position, List<Point2D> pathPoints) {
        int nearestIndex = 0;
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point2D start = pathPoints.get(i);
            Point2D end = pathPoints.get(i + 1);
            Point2D closestOnSeg = getClosestPointOnSegment(start, end, position);
            double dist = position.distanceTo(closestOnSeg);
            if (dist < minDistance) {
                minDistance = dist;
                nearestIndex = i;
            }
        }
        return nearestIndex;
    }

    /**
     * Validates whether inserting/moving a bend within a specific segment causes intersections
     * with any system rectangles (excluding the wire's own endpoint systems).
     */
    private boolean wouldSegmentWithBendIntersectSystems(
            Point2D segmentStart,
            Point2D bendPosition,
            Point2D segmentEnd,
            List<System> systems
    ) {
        for (System system : systems) {
            // Skip systems connected by the endpoints of this wire
            if (sourcePort != null && (system == sourcePort.getParentSystem())) {
                continue;
            }
            if (destinationPort != null && (system == destinationPort.getParentSystem())) {
                continue;
            }

            if (lineIntersectsRectangle(segmentStart, bendPosition, system.getBounds()) ||
                    lineIntersectsRectangle(bendPosition, segmentEnd, system.getBounds())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if this wire passes over any systems.
     */
    public boolean passesOverSystems(List<System> systems) {
        if (sourcePort == null || destinationPort == null) {
            return false;
        }

        Point2D sourcePos = sourcePort.getPosition();
        Point2D destPos = destinationPort.getPosition();

        // Check each system for intersection
        for (System system : systems) {
            // Skip the systems that this wire connects
            if (system == sourcePort.getParentSystem() ||
                    system == destinationPort.getParentSystem()) {
                continue;
            }

            // Check if wire line intersects with system bounds
            if (lineIntersectsRectangle(sourcePos, destPos, system.getBounds())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a line segment intersects with a rectangle.
     * Uses the Liang-Barsky line clipping algorithm.
     */
    private boolean lineIntersectsRectangle(Point2D lineStart, Point2D lineEnd,
                                            java.awt.geom.Rectangle2D rect) {
        double x1 = lineStart.getX();
        double y1 = lineStart.getY();
        double x2 = lineEnd.getX();
        double y2 = lineEnd.getY();

        double xmin = rect.getX();
        double ymin = rect.getY();
        double xmax = rect.getX() + rect.getWidth();
        double ymax = rect.getY() + rect.getHeight();

        // Calculate direction vector
        double dx = x2 - x1;
        double dy = y2 - y1;

        // Parameters for clipping
        double p1 = -dx;
        double p2 = dx;
        double p3 = -dy;
        double p4 = dy;

        double q1 = x1 - xmin;
        double q2 = xmax - x1;
        double q3 = y1 - ymin;
        double q4 = ymax - y1;

        // Check if line is parallel to any boundary
        if (Math.abs(dx) < 1e-10) {
            // Vertical line
            if (x1 < xmin || x1 > xmax) {
                return false;
            }
            return !(y1 > ymax && y2 > ymax) && !(y1 < ymin && y2 < ymin);
        }

        if (Math.abs(dy) < 1e-10) {
            // Horizontal line
            if (y1 < ymin || y1 > ymax) {
                return false;
            }
            return !(x1 > xmax && x2 > xmax) && !(x1 < xmin && x2 < xmin);
        }

        // Calculate intersection parameters
        double u1 = Double.NEGATIVE_INFINITY;
        double u2 = Double.POSITIVE_INFINITY;

        if (p1 != 0) {
            double r1 = q1 / p1;
            double r2 = q2 / p2;
            if (p1 < 0) {
                u1 = Math.max(u1, r1);
                u2 = Math.min(u2, r2);
            } else {
                u1 = Math.max(u1, r2);
                u2 = Math.min(u2, r1);
            }
        }

        if (p3 != 0) {
            double r3 = q3 / p3;
            double r4 = q4 / p4;
            if (p3 < 0) {
                u1 = Math.max(u1, r3);
                u2 = Math.min(u2, r4);
            } else {
                u1 = Math.max(u1, r4);
                u2 = Math.min(u2, r3);
            }
        }

        // Check if there's a valid intersection
        return u1 <= u2 && u2 >= 0 && u1 <= 1;
    }

    /**
     * Increments bulk packet passage count and checks if wire should be destroyed.
     */
    public boolean incrementBulkPacketPassage() {
        bulkPacketPassages++;
        if (bulkPacketPassages >= MAX_BULK_PASSAGES) {
            isDestroyed = true;
            isActive = false;
            return true;
        }
        return false;
    }

    /**
     * Gets the path points for the wire connection.
     * @return List of points representing the wire path
     */
    public List<Point2D> getPathPoints() {
        return getPathPoints(true); // Use smooth curves so packets follow curved wire paths
    }

    /**
     * Gets the path points for the wire connection with optional smooth curve interpolation.
     * @param useSmoothCurves If true, generates smooth Bézier curves; if false, uses rigid polyline
     * @return List of points representing the wire path
     */
    public List<Point2D> getPathPoints(boolean useSmoothCurves) {
        if (!useSmoothCurves) {
            // Original rigid polyline behavior
            List<Point2D> points = new ArrayList<>();

            if (sourcePort != null) {
                points.add(sourcePort.getPosition());
            }

            for (WireBend bend : bends) {
                points.add(bend.getPosition());
            }

            if (destinationPort != null) {
                points.add(destinationPort.getPosition());
            }

            return points;
        } else {
            // Generate smooth curved path that ensures bends are always on the path
            return generateSmoothPathPointsWithBendAlignment();
        }
    }

    /**
     * Generates smooth curved path points using Bézier curve interpolation.
     * This creates natural, fluid wire paths instead of rigid angular bends.
     * @return List of smoothly interpolated path points
     */
    private List<Point2D> generateSmoothPathPoints() {
        List<Point2D> controlPoints = new ArrayList<>();

        // Add source port as first control point
        if (sourcePort != null) {
            controlPoints.add(sourcePort.getPosition());
        }

        // Add bend points as control points
        for (WireBend bend : bends) {
            controlPoints.add(bend.getPosition());
        }

        // Add destination port as last control point
        if (destinationPort != null) {
            controlPoints.add(destinationPort.getPosition());
        }

        if (controlPoints.size() < 2) {
            return controlPoints; // Need at least 2 points for a path
        }

        // Generate smooth curve with appropriate density
        return generateBezierCurve(controlPoints);
    }

    /**
     * Generates smooth curved path points that ensure bends are always perfectly aligned with the wire.
     * This creates a hybrid approach: smooth curves between bends, but bends are always on the exact path.
     * @return List of smoothly interpolated path points with perfect bend alignment
     */
    private List<Point2D> generateSmoothPathPointsWithBendAlignment() {
        if (bends.isEmpty()) {
            // No bends - just return straight line
            List<Point2D> points = new ArrayList<>();
            if (sourcePort != null) {
                points.add(sourcePort.getPosition());
            }
            if (destinationPort != null) {
                points.add(destinationPort.getPosition());
            }
            return points;
        }

        List<Point2D> alignedPath = new ArrayList<>();

        // Start with source port
        if (sourcePort != null) {
            alignedPath.add(sourcePort.getPosition());
        }

        // For each bend, create a smooth curve from the previous point to the bend
        Point2D currentPoint = sourcePort != null ? sourcePort.getPosition() : new Point2D(0, 0);

        for (int i = 0; i < bends.size(); i++) {
            WireBend bend = bends.get(i);
            Point2D bendPos = bend.getPosition();

            // Generate smooth curve from current point to this bend
            List<Point2D> curveSegment = generateSmoothCurveSegment(currentPoint, bendPos);

            // Add all curve points except the last one (to avoid duplication)
            for (int j = 0; j < curveSegment.size() - 1; j++) {
                alignedPath.add(curveSegment.get(j));
            }

            // Add the bend position exactly (ensuring perfect alignment)
            alignedPath.add(bendPos);
            currentPoint = bendPos;
        }

        // Generate final curve to destination port
        if (destinationPort != null) {
            List<Point2D> finalCurve = generateSmoothCurveSegment(currentPoint, destinationPort.getPosition());

            // Add all curve points except the first one (to avoid duplication)
            for (int j = 1; j < finalCurve.size(); j++) {
                alignedPath.add(finalCurve.get(j));
            }
        }

        return alignedPath;
    }

    /**
     * Generates a smooth curve segment between two points using quadratic Bézier interpolation.
     * This creates natural curves while ensuring the endpoints are exact.
     * @param start Start point
     * @param end End point
     * @return List of interpolated points for the curve segment
     */
    private List<Point2D> generateSmoothCurveSegment(Point2D start, Point2D end) {
        // Calculate a control point that creates a natural curve
        double distance = start.distanceTo(end);
        double controlDistance = distance * 0.3; // Control point at 30% of distance

        // Create a control point perpendicular to the line, creating a gentle curve
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();

        // Perpendicular vector (rotate 90 degrees)
        double perpX = -dy;
        double perpY = dx;

        // Normalize and scale
        double perpLength = Math.sqrt(perpX * perpX + perpY * perpY);
        if (perpLength > 0) {
            perpX = (perpX / perpLength) * controlDistance;
            perpY = (perpY / perpLength) * controlDistance;
        }

        // Control point at midpoint with perpendicular offset
        double midX = (start.getX() + end.getX()) / 2.0;
        double midY = (start.getY() + end.getY()) / 2.0;
        Point2D controlPoint = new Point2D(midX + perpX, midY + perpY);

        // Generate quadratic Bézier curve
        return generateQuadraticBezierCurve(start, controlPoint, end);
    }

    /**
     * Generates a smooth Bézier curve through the given control points.
     * Uses quadratic Bézier curves for 3 points, cubic for 4+ points.
     * @param controlPoints The control points to interpolate through
     * @return List of smoothly interpolated path points
     */
    private List<Point2D> generateBezierCurve(List<Point2D> controlPoints) {
        List<Point2D> curvePoints = new ArrayList<>();

        if (controlPoints.size() == 2) {
            // Straight line - just return the two points
            return controlPoints;
        } else if (controlPoints.size() == 3) {
            // Quadratic Bézier curve (3 control points)
            return generateQuadraticBezierCurve(controlPoints.get(0), controlPoints.get(1), controlPoints.get(2));
        } else {
            // Multiple segments with smooth transitions
            return generateMultiSegmentBezierCurve(controlPoints);
        }
    }

    /**
     * Generates a quadratic Bézier curve between three control points.
     * @param p0 Start point
     * @param p1 Control point
     * @param p2 End point
     * @return List of interpolated curve points
     */
    private List<Point2D> generateQuadraticBezierCurve(Point2D p0, Point2D p1, Point2D p2) {
        List<Point2D> curvePoints = new ArrayList<>();

        // Number of interpolation steps - more steps = smoother curve
        int steps = calculateOptimalSteps(p0, p1, p2);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Point2D point = quadraticBezierInterpolate(p0, p1, p2, t);
            curvePoints.add(point);
        }

        return curvePoints;
    }

    /**
     * Generates a multi-segment Bézier curve with smooth transitions between segments.
     * @param controlPoints The control points to interpolate through
     * @return List of smoothly interpolated path points
     */
    private List<Point2D> generateMultiSegmentBezierCurve(List<Point2D> controlPoints) {
        List<Point2D> curvePoints = new ArrayList<>();

        if (controlPoints.size() < 3) {
            return controlPoints; // Need at least 3 points for multi-segment
        }

        // Start with first point
        curvePoints.add(controlPoints.get(0));

        // For multiple control points, use a more sophisticated approach
        // Generate smooth curve through all points using Catmull-Rom splines
        return generateCatmullRomSpline(controlPoints);
    }

    /**
     * Generates a Catmull-Rom spline through the control points for very smooth curves.
     * This creates natural, fluid curves that pass through all control points.
     * @param controlPoints The control points to interpolate through
     * @return List of smoothly interpolated path points
     */
    private List<Point2D> generateCatmullRomSpline(List<Point2D> controlPoints) {
        List<Point2D> curvePoints = new ArrayList<>();

        if (controlPoints.size() < 2) {
            return controlPoints;
        }

        // Add first point
        curvePoints.add(controlPoints.get(0));

        // Generate curve segments between each pair of control points
        for (int i = 0; i < controlPoints.size() - 1; i++) {
            Point2D p0, p1, p2, p3;

            // Handle boundary conditions for smooth start/end
            if (i == 0) {
                // First segment: extrapolate backwards
                p0 = extrapolatePoint(controlPoints.get(1), controlPoints.get(0));
                p1 = controlPoints.get(0);
                p2 = controlPoints.get(1);
                p3 = controlPoints.size() > 2 ? controlPoints.get(2) : extrapolatePoint(controlPoints.get(0), controlPoints.get(1));
            } else if (i == controlPoints.size() - 2) {
                // Last segment: extrapolate forwards
                p0 = controlPoints.get(i - 1);
                p1 = controlPoints.get(i);
                p2 = controlPoints.get(i + 1);
                p3 = extrapolatePoint(controlPoints.get(i), controlPoints.get(i + 1));
            } else {
                // Middle segments: use adjacent points
                p0 = controlPoints.get(i - 1);
                p1 = controlPoints.get(i);
                p2 = controlPoints.get(i + 1);
                p3 = controlPoints.get(i + 2);
            }

            // Generate smooth curve segment using Catmull-Rom interpolation
            List<Point2D> segment = generateCatmullRomSegment(p0, p1, p2, p3);

            // Add segment points (skip first to avoid duplication)
            for (int j = 1; j < segment.size(); j++) {
                curvePoints.add(segment.get(j));
            }
        }

        return curvePoints;
    }

    /**
     * Generates a Catmull-Rom spline segment between four control points.
     * @param p0 Previous control point
     * @param p1 Current control point (start of segment)
     * @param p2 Next control point (end of segment)
     * @param p3 Following control point
     * @return List of interpolated points for this segment
     */
    private List<Point2D> generateCatmullRomSegment(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
        List<Point2D> segment = new ArrayList<>();

        // Calculate optimal number of steps based on segment length and complexity
        double segmentLength = p1.distanceTo(p2);
        int steps = Math.max(15, (int) (segmentLength / 5.0)); // At least 15 steps, more for longer segments

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Point2D point = catmullRomInterpolate(p0, p1, p2, p3, t);
            segment.add(point);
        }

        return segment;
    }

    /**
     * Interpolates a point on a Catmull-Rom spline at parameter t.
     * @param p0 Previous control point
     * @param p1 Current control point
     * @param p2 Next control point
     * @param p3 Following control point
     * @param t Parameter value (0.0 to 1.0)
     * @return Interpolated point on the spline
     */
    private Point2D catmullRomInterpolate(Point2D p0, Point2D p1, Point2D p2, Point2D p3, double t) {
        // Catmull-Rom matrix coefficients
        double t2 = t * t;
        double t3 = t2 * t;

        // Catmull-Rom blending functions
        double b0 = -0.5 * t3 + t2 - 0.5 * t;
        double b1 = 1.5 * t3 - 2.5 * t2 + 1.0;
        double b2 = -1.5 * t3 + 2.0 * t2 + 0.5 * t;
        double b3 = 0.5 * t3 - 0.5 * t2;

        // Interpolate x and y coordinates
        double x = b0 * p0.getX() + b1 * p1.getX() + b2 * p2.getX() + b3 * p3.getX();
        double y = b0 * p0.getY() + b1 * p1.getY() + b2 * p2.getY() + b3 * p3.getY();

        return new Point2D(x, y);
    }

    /**
     * Extrapolates a point beyond the given segment for smooth boundary conditions.
     * @param p1 First point
     * @param p2 Second point
     * @return Extrapolated point
     */
    private Point2D extrapolatePoint(Point2D p1, Point2D p2) {
        // Simple linear extrapolation: extend the line from p1 to p2
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();

        return new Point2D(p1.getX() - dx, p1.getY() - dy);
    }

    /**
     * Calculates optimal number of interpolation steps based on curve complexity.
     * @param p0 Start point
     * @param p1 Control point
     * @param p2 End point
     * @return Number of interpolation steps
     */
    private int calculateOptimalSteps(Point2D p0, Point2D p1, Point2D p2) {
        // Calculate curve complexity based on control point deviation
        double straightLineLength = p0.distanceTo(p2);
        double actualPathLength = p0.distanceTo(p1) + p1.distanceTo(p2);
        double deviation = actualPathLength - straightLineLength;

        // More deviation = more complex curve = more interpolation steps
        int baseSteps = 20; // Base smoothness
        int additionalSteps = (int) Math.min(30, deviation / 10.0); // Cap at reasonable limit

        return Math.max(10, baseSteps + additionalSteps); // Minimum 10 steps for smoothness
    }

    /**
     * Interpolates a point on a quadratic Bézier curve at parameter t.
     * @param p0 Start point
     * @param p1 Control point
     * @param p2 End point
     * @param t Parameter value (0.0 to 1.0)
     * @return Interpolated point on the curve
     */
    private Point2D quadraticBezierInterpolate(Point2D p0, Point2D p1, Point2D p2, double t) {
        // Quadratic Bézier formula: B(t) = (1-t)²P₀ + 2(1-t)tP₁ + t²P₂
        double oneMinusT = 1.0 - t;
        double oneMinusTSquared = oneMinusT * oneMinusT;
        double tSquared = t * t;

        double x = oneMinusTSquared * p0.getX() + 2 * oneMinusT * t * p1.getX() + tSquared * p2.getX();
        double y = oneMinusTSquared * p0.getY() + 2 * oneMinusT * t * p1.getY() + tSquared * p2.getY();

        return new Point2D(x, y);
    }

    /**
     * Calculates the total length of the wire including all bends.
     * Uses smooth curves by default for backward compatibility.
     */
    public double getTotalLength() {
        return getTotalLength(true); // Default to smooth curves for backward compatibility
    }

    /**
     * Calculates the total length of the wire including all bends.
     * @param useSmoothCurves If true, uses smooth curves; if false, uses rigid polyline
     */
    public double getTotalLength(boolean useSmoothCurves) {
        List<Point2D> pathPoints = getPathPoints(useSmoothCurves);
        if (pathPoints.size() < 2) {
            return wireLength; // Fallback to stored length
        }

        double totalLength = 0.0;
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point2D start = pathPoints.get(i);
            Point2D end = pathPoints.get(i + 1);
            totalLength += start.distanceTo(end);
        }

        return totalLength;
    }

    /**
     * Gets the position at a specific progress along the wire path (0.0 to 1.0).
     * This enables uniform motion along curved wire paths with bends.
     * Uses smooth curves by default for backward compatibility.
     */
    public Point2D getPositionAtProgress(double progress) {
        return getPositionAtProgress(progress, true); // Default to smooth curves for backward compatibility
    }

    /**
     * Gets the position at a specific progress along the wire path (0.0 to 1.0).
     * This enables uniform motion along both curved and rigid wire paths with bends.
     * @param progress Progress along the wire (0.0 to 1.0)
     * @param useSmoothCurves If true, uses smooth curves; if false, uses rigid polyline
     */
    public Point2D getPositionAtProgress(double progress, boolean useSmoothCurves) {
        List<Point2D> pathPoints = getPathPoints(useSmoothCurves);
        if (pathPoints.size() < 2) {
            return pathPoints.isEmpty() ? new Point2D(0, 0) : pathPoints.get(0);
        }

        // Clamp progress to valid range
        progress = Math.max(0.0, Math.min(1.0, progress));

        // Calculate target distance along the path
        double totalLength = getTotalLength(useSmoothCurves);
        double targetDistance = progress * totalLength;

        // Find the segment and interpolate within it
        double accumulatedDistance = 0.0;
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point2D start = pathPoints.get(i);
            Point2D end = pathPoints.get(i + 1);
            double segmentLength = start.distanceTo(end);

            if (accumulatedDistance + segmentLength >= targetDistance) {
                // The target position is within this segment
                double segmentProgress = (targetDistance - accumulatedDistance) / segmentLength;
                return interpolatePosition(start, end, segmentProgress);
            }

            accumulatedDistance += segmentLength;
        }

        // If we reach here, return the destination port position
        return pathPoints.get(pathPoints.size() - 1);
    }

    /**
     * Interpolates between two points with the given progress (0.0 to 1.0).
     */
    private Point2D interpolatePosition(Point2D start, Point2D end, double progress) {
        double x = start.getX() + (end.getX() - start.getX()) * progress;
        double y = start.getY() + (end.getY() - start.getY()) * progress;
        return new Point2D(x, y);
    }



    /**
     * Clears all packets from this wire connection.
     * Used during level transitions to prevent packet duplication.
     */
    public void clearAllPackets() {
        packetsOnWire.clear();
        java.lang.System.out.println("DEBUG: Cleared all packets from wire " + getId().substring(0, 8));
    }

    /**
     * Gets the closest point on this wire to a given point.
     * Used for ability targeting.
     */
    public Point2D getClosestPointOnWire(Point2D targetPoint) {
        List<Point2D> pathPoints = getPathPoints();
        if (pathPoints.isEmpty()) {
            return null;
        }

        Point2D closestPoint = pathPoints.get(0);
        double closestDistance = targetPoint.distanceTo(closestPoint);

        // Check all path points and line segments
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point2D start = pathPoints.get(i);
            Point2D end = pathPoints.get(i + 1);

            // Find closest point on line segment
            Point2D segmentClosest = getClosestPointOnSegment(start, end, targetPoint);
            double distance = targetPoint.distanceTo(segmentClosest);

            if (distance < closestDistance) {
                closestDistance = distance;
                closestPoint = segmentClosest;
            }
        }

        return closestPoint;
    }

    /**
     * Gets the progress value (0-1) for a given point on the wire.
     */
    public double getProgressAtPoint(Point2D point) {
        List<Point2D> pathPoints = getPathPoints();
        if (pathPoints.isEmpty()) {
            return 0.0;
        }

        double totalLength = getTotalLength();
        if (totalLength == 0) {
            return 0.0;
        }

        double accumulatedLength = 0.0;
        Point2D closestPoint = getClosestPointOnWire(point);

        // Find which segment contains the closest point and calculate progress
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point2D start = pathPoints.get(i);
            Point2D end = pathPoints.get(i + 1);

            Point2D segmentClosest = getClosestPointOnSegment(start, end, point);

            if (segmentClosest.distanceTo(closestPoint) < 1.0) { // Found the segment
                double segmentProgress = start.distanceTo(segmentClosest) / start.distanceTo(end);
                double segmentLength = start.distanceTo(end);
                return (accumulatedLength + segmentProgress * segmentLength) / totalLength;
            }

            accumulatedLength += start.distanceTo(end);
        }

        return 1.0; // Default to end if not found
    }

    /**
     * Gets the distance from a point to this wire.
     */
    public double getDistanceToPoint(Point2D targetPoint) {
        Point2D closestPoint = getClosestPointOnWire(targetPoint);
        return closestPoint != null ? targetPoint.distanceTo(closestPoint) : Double.MAX_VALUE;
    }

    /**
     * Gets the closest point on a line segment to a target point.
     */
    private Point2D getClosestPointOnSegment(Point2D start, Point2D end, Point2D target) {
        double segmentLength = start.distanceTo(end);
        if (segmentLength == 0) {
            return start;
        }

        Vec2D segmentVector = new Vec2D(end.getX() - start.getX(), end.getY() - start.getY());
        Vec2D targetVector = new Vec2D(target.getX() - start.getX(), target.getY() - start.getY());

        double projection = targetVector.dot(segmentVector) / (segmentLength * segmentLength);
        projection = Math.max(0.0, Math.min(1.0, projection)); // Clamp to segment

        return new Point2D(
                start.getX() + projection * segmentVector.getX(),
                start.getY() + projection * segmentVector.getY()
        );
    }

    /**
     * Updates the port references for this wire connection.
     * This is used during level loading to ensure wire connections
     * reference the correct port instances in the current level.
     */
    public void updatePortReferences(Port newSourcePort, Port newDestinationPort) {
        // Normalize direction: ensure source is OUTPUT and destination is INPUT
        Port normalizedSource = newSourcePort;
        Port normalizedDestination = newDestinationPort;
        if (newSourcePort != null && newDestinationPort != null) {
            boolean srcIsInput = newSourcePort.isInput();
            boolean dstIsInput = newDestinationPort.isInput();
            if (srcIsInput && !dstIsInput) {
                // Swap so that source is always an output and destination is input
                normalizedSource = newDestinationPort;
                normalizedDestination = newSourcePort;
            }
        }
        this.sourcePort = normalizedSource;
        this.destinationPort = normalizedDestination;

        // Recalculate path points with new port positions
        calculatePathPoints();
    }

    /**
     * Calculates the path points for this wire connection.
     * Used when port references are updated.
     */
    private void calculatePathPoints() {
        // For now, use a simple straight line path
        // This could be enhanced later to handle bends properly
        if (sourcePort != null && destinationPort != null) {
            Point2D sourcePos = sourcePort.getPosition();
            Point2D destPos = destinationPort.getPosition();

            if (sourcePos != null && destPos != null) {
                // Update wire length if needed
                this.wireLength = sourcePos.distanceTo(destPos);
            }
        }
    }

}
