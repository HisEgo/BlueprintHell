package model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Objects;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@com.fasterxml.jackson.annotation.JsonTypeInfo(use = com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME,
        include = com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY, property = "type")
@com.fasterxml.jackson.annotation.JsonSubTypes({
        @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = NormalSystem.class, name = "NormalSystem"),
        @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = ReferenceSystem.class, name = "ReferenceSystem"),
        @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = SpySystem.class, name = "SpySystem"),
        @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = SaboteurSystem.class, name = "SaboteurSystem"),
        @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = VPNSystem.class, name = "VPNSystem"),
        @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = AntiTrojanSystem.class, name = "AntiTrojanSystem"),
        @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = DistributorSystem.class, name = "DistributorSystem"),
        @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = MergerSystem.class, name = "MergerSystem")
})
public abstract class System {
    private static final int MAX_STORAGE = 5;

    // Phase 2: Speed damage constants (raised to avoid destroying packets on normal wires)
    private static final double SPEED_DAMAGE_THRESHOLD = 150.0; // pixels/second
    private static final double SPEED_DAMAGE_DEACTIVATION_TIME = 10.0; // seconds

    private String id;
    private Point2D position;
    private List<Port> inputPorts;
    private List<Port> outputPorts;
    private List<Packet> storage;
    private boolean isActive;
    private SystemType systemType;
    private double deactivationTimer;
    private double maxDeactivationTime;
    private boolean isFailed;
    private boolean indicatorVisible;
    private GameLevel parentLevel; // Reference to access wire connections

    public System() {
        this.id = java.util.UUID.randomUUID().toString();
        this.position = new Point2D();
        this.inputPorts = new ArrayList<>();
        this.outputPorts = new ArrayList<>();
        this.storage = new ArrayList<>();
        this.isActive = true;
        this.deactivationTimer = 0.0;
        this.maxDeactivationTime = 10.0; // Default 10 seconds
        this.isFailed = false;
        this.indicatorVisible = false; // Initially turned off
    }

    public System(Point2D position) {
        this();
        this.position = position;
    }

    public System(Point2D position, SystemType systemType) {
        this(position);
        this.systemType = systemType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Point2D getPosition() {
        return position;
    }

    public void setPosition(Point2D position) {
        this.position = position;
    }

    public List<Port> getInputPorts() {
        return inputPorts;
    }

    public void setInputPorts(List<Port> inputPorts) {
        this.inputPorts = inputPorts;
    }

    public List<Port> getOutputPorts() {
        return outputPorts;
    }

    public void setOutputPorts(List<Port> outputPorts) {
        this.outputPorts = outputPorts;
    }

    public List<Packet> getStorage() {
        return storage;
    }

    public void setStorage(List<Packet> storage) {
        this.storage = storage;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    // Phase 2 properties
    public SystemType getSystemType() {
        return systemType;
    }

    public void setSystemType(SystemType systemType) {
        this.systemType = systemType;
    }

    public double getDeactivationTimer() {
        return deactivationTimer;
    }

    public void setDeactivationTimer(double deactivationTimer) {
        this.deactivationTimer = deactivationTimer;
    }

    public double getMaxDeactivationTime() {
        return maxDeactivationTime;
    }

    public void setMaxDeactivationTime(double maxDeactivationTime) {
        this.maxDeactivationTime = maxDeactivationTime;
    }

    public boolean isFailed() {
        return isFailed;
    }

    public void setFailed(boolean failed) {
        isFailed = failed;
    }

    public boolean isIndicatorVisible() {
        return indicatorVisible;
    }

    public void setIndicatorVisible(boolean indicatorVisible) {
        this.indicatorVisible = indicatorVisible;
    }

    public GameLevel getParentLevel() {
        return parentLevel;
    }

    public void setParentLevel(GameLevel parentLevel) {
        this.parentLevel = parentLevel;
    }

    public void addInputPort(Port port) {
        port.setParentSystem(this);
        port.setInput(true);
        inputPorts.add(port);
    }

    public void addOutputPort(Port port) {
        port.setParentSystem(this);
        port.setInput(false);
        outputPorts.add(port);
    }

    public List<Port> getAllPorts() {
        List<Port> allPorts = new ArrayList<>();
        allPorts.addAll(inputPorts);
        allPorts.addAll(outputPorts);
        return allPorts;
    }

    public void processInputs() {
        if (!isActive) return;

        for (Port inputPort : inputPorts) {
            if (inputPort.getCurrentPacket() != null) {
                Packet packet = inputPort.releasePacket();
                processPacket(packet, inputPort); // Pass the input port
            }
        }
        
        // Also process stored packets if output ports become available
        processStoredPackets();
    }

    private void processStoredPackets() {
        if (storage.isEmpty()) return;

        // Try to move stored packets to available output ports
        Iterator<Packet> iterator = storage.iterator();
        while (iterator.hasNext()) {
            Packet packet = iterator.next();
            Port availablePort = findAvailableOutputPort(packet);
            
            if (availablePort != null) {
                // Move packet from storage to output port
                iterator.remove();
                availablePort.acceptPacket(packet);
                
                // Apply exit speed doubling if packet is exiting through incompatible port
                boolean isCompatible = availablePort.isCompatibleWithPacket(packet);
                if (!isCompatible && packet instanceof MessengerPacket) {
                    ((MessengerPacket) packet).applyExitSpeedMultiplier(true);
                } else if (!isCompatible && packet instanceof ProtectedPacket) {
                    ((ProtectedPacket) packet).applyExitSpeedMultiplier(true);
                }
                // Process only one packet per update cycle to avoid overwhelming the system
                break;
            }
        }
    }

    public void processPacket(Packet packet) {
        processPacket(packet, null);
    }
    
    public void processPacket(Packet packet, Port entryPort) {
        // Phase 2: Check if packet speed exceeds damage threshold
        // Disable the damage rule for Level 1 as requested
        boolean disableDamageForLevel1 = parentLevel != null &&
                "level1".equals(parentLevel.getLevelId());
        if (!disableDamageForLevel1) {
            double packetSpeed = packet.getMovementVector().magnitude();
            if (packetSpeed > SPEED_DAMAGE_THRESHOLD) {
                // High-speed packet damages the system
                deactivate(SPEED_DAMAGE_DEACTIVATION_TIME);
                java.lang.System.out.println("*** SYSTEM DAMAGED *** " + getClass().getSimpleName() +
                        " deactivated for " + SPEED_DAMAGE_DEACTIVATION_TIME + "s due to high-speed packet (" +
                        String.format("%.1f", packetSpeed) + " > " + SPEED_DAMAGE_THRESHOLD + ")");

                // Packet bounces back or is destroyed
                packet.setActive(false);
                return;
            }
        }

        // Phase 2 advanced scenarios:
        // Bulk packets destroy all other packets stored in a system upon arrival
        if (packet.getPacketType() != null && packet.getPacketType().isBulk()) {
            // Destroy other packets in storage
            List<Packet> toRemove = new ArrayList<>();
            for (Packet stored : storage) {
                if (stored != packet && stored.isActive()) {
                    stored.setActive(false);
                    stored.setLost(true); // Mark as lost for statistics
                    toRemove.add(stored);
                }
            }
            if (!toRemove.isEmpty()) {
                storage.removeAll(toRemove);
            }
            
            // Also destroy packets in input ports
            for (Port inputPort : inputPorts) {
                Packet portPacket = inputPort.getCurrentPacket();
                if (portPacket != null && portPacket != packet && portPacket.isActive()) {
                    portPacket.setActive(false);
                    portPacket.setLost(true); // Mark as lost for statistics
                    inputPort.setCurrentPacket(null);
                }
            }
            
            // Also destroy packets in output ports
            for (Port outputPort : outputPorts) {
                Packet portPacket = outputPort.getCurrentPacket();
                if (portPacket != null && portPacket != packet && portPacket.isActive()) {
                    portPacket.setActive(false);
                    portPacket.setLost(true); // Mark as lost for statistics
                    outputPort.setCurrentPacket(null);
                }
            }
            
            // Bulk packets change the entry port type when entering systems
            if (packet instanceof BulkPacket && entryPort != null) {
                changeEntryPortType(entryPort);
            }
        }

        // Confidential packets reduce speed if system already holds other packets
        if (packet instanceof ConfidentialPacket) {
            boolean systemHasOtherPackets = getTotalPacketCount() > 0; // includes ports and storage
            ((ConfidentialPacket) packet).adjustSpeedForSystemOccupancy(systemHasOtherPackets);
        }

        // Try to find an available compatible output port
        Port availablePort = findAvailableOutputPort(packet);

        if (availablePort != null) {
            // Transfer packet to output port
            availablePort.acceptPacket(packet);
            
            // Apply exit speed doubling if packet is exiting through incompatible port
            boolean isCompatible = availablePort.isCompatibleWithPacket(packet);
            if (!isCompatible && packet instanceof MessengerPacket) {
                ((MessengerPacket) packet).applyExitSpeedMultiplier(true);
            } else if (!isCompatible && packet instanceof ProtectedPacket) {
                ((ProtectedPacket) packet).applyExitSpeedMultiplier(true);
            }
            
        } else if (storage.size() < MAX_STORAGE) {
            // Store packet if storage is available
            storage.add(packet);
        } else {
            // Packet is lost if no storage available
            packet.setActive(false);
        }
    }

    protected Port findAvailableOutputPort(Packet packet) {
        List<Port> compatibleEmptyPorts = new ArrayList<>();
        List<Port> anyEmptyPorts = new ArrayList<>();

        for (Port port : outputPorts) {
            // Check if port is empty and destination system is active
            boolean canAccept = port.canAcceptPacket(packet);
            boolean destActive = isDestinationSystemActive(port);
            boolean isCompatible = port.isCompatibleWithPacket(packet);
            
            if (canAccept && destActive) {
                if (isCompatible) {
                    // Priority 1: Compatible and empty
                    compatibleEmptyPorts.add(port);
                } else {
                    // Priority 2: Any empty port (even if not compatible)
                    anyEmptyPorts.add(port);
                }
            }
        }

        // Priority 1: Compatible empty ports (highest priority)
        if (!compatibleEmptyPorts.isEmpty()) {
            Random random = new Random();
            Port selectedPort = compatibleEmptyPorts.get(random.nextInt(compatibleEmptyPorts.size()));
            return selectedPort;
        }

        // Priority 2: Any empty port (random selection)
        if (!anyEmptyPorts.isEmpty()) {
            Random random = new Random();
            Port selectedPort = anyEmptyPorts.get(random.nextInt(anyEmptyPorts.size()));
            return selectedPort;
        }

        // Priority 3: No empty ports - packet will be stored in system
        return null;
    }

    private boolean isPortWireAvailable(Port port) {
        // Only treat a port as having wire capacity if there is an ACTIVE
        // outgoing connection starting from this port that can accept a packet.
        if (parentLevel == null) {
            return false;
        }

        for (WireConnection connection : parentLevel.getWireConnections()) {
            if (!connection.isActive()) {
                continue;
            }
            if (connection.getSourcePort() == port) {
                return connection.canAcceptPacket();
            }
        }
        // No outgoing wire found for this port
        return false;
    }

    private WireConnection findWireConnectionForPort(Port port) {
        if (parentLevel == null || port == null) {
            return null;
        }

        for (WireConnection connection : parentLevel.getWireConnections()) {
            Port source = connection.getSourcePort();
            Port destination = connection.getDestinationPort();

            // Fast path: identity match
            if (source == port || destination == port) {
                return connection;
            }

            // Robust matching: structural equality or near-identical position/shape/direction
            if (portsEquivalent(source, port) || portsEquivalent(destination, port)) {
                return connection;
            }
        }

        return null;
    }

    private boolean portsEquivalent(Port a, Port b) {
        if (a == null || b == null) return false;
        if (a.equals(b)) return true;
        if (a.getShape() != b.getShape()) return false;
        if (a.isInput() != b.isInput()) return false;
        Point2D pa = a.getPosition();
        Point2D pb = b.getPosition();
        if (pa == null || pb == null) return false;
        return pa.distanceTo(pb) < 1.0;
    }

    private void changeEntryPortType(Port entryPort) {
        // Change the entry port type randomly
        java.util.Random random = new java.util.Random();
        PortShape currentShape = entryPort.getShape();
        PortShape[] availableShapes = {PortShape.SQUARE, PortShape.TRIANGLE, PortShape.HEXAGON};
        PortShape newShape;
        
        do {
            newShape = availableShapes[random.nextInt(availableShapes.length)];
        } while (newShape == currentShape);
        
        entryPort.setShape(newShape);
        java.lang.System.out.println("*** BULK PACKET PORT CHANGE *** Entry port " + 
                entryPort.getPosition() + " changed from " + currentShape + " to " + newShape);
    }

    private boolean isDestinationSystemActive(Port port) {
        WireConnection connection = findWireConnectionForPort(port);
        if (connection == null) return false;

        // Find the destination system for this port
        System destinationSystem = null;
        if (connection.getSourcePort() == port) {
            destinationSystem = connection.getDestinationPort().getParentSystem();
        } else if (connection.getDestinationPort() == port) {
            destinationSystem = connection.getSourcePort().getParentSystem();
        }

        // Check if destination system is active and not failed
        return destinationSystem != null && destinationSystem.isActive() && !destinationSystem.hasFailed();
    }

    public void processStorage() {
        // Storage processing is now handled by GameController for better coordination
        // This prevents conflicts between system-level and controller-level packet management
    }

    public int getTotalPacketCount() {
        int count = storage.size();

        for (Port port : inputPorts) {
            if (port.getCurrentPacket() != null) count++;
        }

        for (Port port : outputPorts) {
            if (port.getCurrentPacket() != null) count++;
        }

        return count;
    }

    public boolean hasStorageSpace() {
        return storage.size() < MAX_STORAGE;
    }

    public void clearStorage() {
        storage.clear();
    }

    public int getCoinValue() {
        int totalCoins = 0;

        for (Port port : inputPorts) {
            if (port.getCurrentPacket() != null) {
                totalCoins += port.getCurrentPacket().getCoinValue();
            }
        }

        for (Packet packet : storage) {
            totalCoins += packet.getCoinValue();
        }

        return totalCoins;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        System system = (System) obj;
        return Objects.equals(id, system.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id='" + id + '\'' +
                ", position=" + position +
                ", inputPorts=" + inputPorts.size() +
                ", outputPorts=" + outputPorts.size() +
                ", storage=" + storage.size() +
                ", active=" + isActive +
                '}';
    }

    public void update(double deltaTime) {
        if (deactivationTimer > 0) {
            deactivationTimer -= deltaTime;
            if (deactivationTimer <= 0) {
                isActive = true;
                deactivationTimer = 0;
            }
        }

        // Update indicator based on port connections
        updateIndicatorStatus();
    }

    private void updateIndicatorStatus() {
        // Indicators are now handled by the view layer based on network connectivity
        // This method is kept for backward compatibility but no longer controls visibility
        boolean allPortsConnected = areAllPortsConnected();
        setIndicatorVisible(allPortsConnected);
    }

    public boolean areAllPortsConnected() {
        // If the system has no ports at all, it's considered connected by default
        // (e.g., standalone systems that don't need connections)
        if (inputPorts.isEmpty() && outputPorts.isEmpty()) {
            return true;
        }

        // Helper lambdas to check all inputs/outputs
        boolean allInputsConnected = true;
        for (Port port : inputPorts) {
            if (!port.isConnected()) {
                allInputsConnected = false;
                break;
            }
        }

        boolean allOutputsConnected = true;
        for (Port port : outputPorts) {
            if (!port.isConnected()) {
                allOutputsConnected = false;
                break;
            }
        }

        // Consider one-sided systems (e.g., reference source/destination) as connected
        // when ALL of their existing ports are connected.
        if (inputPorts.isEmpty()) {
            return allOutputsConnected; // Source-like systems
        }
        if (outputPorts.isEmpty()) {
            return allInputsConnected; // Destination-like systems
        }

        // Two-sided systems must have all inputs and outputs connected
        return allInputsConnected && allOutputsConnected;
    }

    public void deactivate(double duration) {
        isActive = false;
        deactivationTimer = duration;
        // Immediately update indicator status
        updateIndicatorStatus();
    }

    public void updateDeactivationTimer(double deltaTime) {
        if (deactivationTimer > 0) {
            deactivationTimer -= deltaTime;
            if (deactivationTimer <= 0) {
                deactivationTimer = 0;
                if (!isFailed) {
                    isActive = true;
                    // Immediately update indicator status when reactivated
                    updateIndicatorStatus();
                    java.lang.System.out.println("*** SYSTEM REACTIVATED *** " + getClass().getSimpleName() +
                            " is now active again");
                }
            }
        }
    }

    public boolean isDeactivated() {
        return !isActive && deactivationTimer > 0;
    }

    public void fail() {
        isFailed = true;
        isActive = false;

        // Immediately update indicator status
        updateIndicatorStatus();

        // Phase 2: Return any packets that are en route to this failed system
        returnPacketsToSource();

        java.lang.System.out.println("*** SYSTEM FAILED *** " + getClass().getSimpleName() +
                " has failed permanently. Returning packets to source.");
    }

    private void returnPacketsToSource() {
        if (parentLevel == null) return;

        // Find all wire connections leading to this system
        for (WireConnection connection : parentLevel.getWireConnections()) {
            if (connection.getDestinationPort() != null &&
                    connection.getDestinationPort().getParentSystem() == this) {

                // Return all packets on wires leading to this system
                for (Packet packet : connection.getPacketsOnWire()) {
                    if (packet.isActive() && !packet.isReturningToSource()) {
                        packet.returnToSource();
                    }
                }
            }
        }

        // Also handle packets in input ports
        for (Port inputPort : inputPorts) {
            if (inputPort.getCurrentPacket() != null) {
                Packet packet = inputPort.getCurrentPacket();
                if (packet.isActive() && !packet.isReturningToSource()) {
                    packet.returnToSource();
                    inputPort.releasePacket(); // Remove from failed system
                }
            }
        }
    }

    public boolean hasFailed() {
        return isFailed;
    }

    @JsonIgnore
    public java.awt.geom.Rectangle2D getBounds() {
        return new java.awt.geom.Rectangle2D.Double(
                position.getX() - 20,
                position.getY() - 20,
                40,
                40
        );
    }

    public List<Port> getAvailableOutputPorts() {
        List<Port> available = new ArrayList<>();
        for (Port port : outputPorts) {
            if (port.isConnected()) {
                System connectedSystem = port.getConnectedSystem();
                // Only add if the connected system exists and is not deactivated
                if (connectedSystem == null || !connectedSystem.isDeactivated()) {
                    available.add(port);
                }
            }
        }
        return available;
    }

    public void reset() {
        clearStorage();
        isActive = true;
        deactivationTimer = 0.0;
        isFailed = false;
        indicatorVisible = false;
        
        // Clear packets from ports
        for (Port port : inputPorts) {
            port.setCurrentPacket(null);
        }
        for (Port port : outputPorts) {
            port.setCurrentPacket(null);
        }
    }
}
