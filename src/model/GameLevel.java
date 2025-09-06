package model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents a game level with system layout, wire length, and packet injection schedule.
 * POJO class for serialization support.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameLevel {
    private String levelId;
    private String name;
    private String description;
    private double initialWireLength;
    private double levelDuration;
    private List<System> systems;
    private List<WireConnection> wireConnections;
    private Map<Double, List<Packet>> packetInjectionSchedule;
    private List<PacketInjection> packetSchedule; // Preferred JSON format: direct list
    private List<String> connectionRules;
    private boolean isCompleted;

    public GameLevel() {
        this.systems = new ArrayList<>();
        this.wireConnections = new ArrayList<>();
        this.packetInjectionSchedule = new HashMap<>();
        this.packetSchedule = new ArrayList<>(); // Initialize the new field
        this.connectionRules = new ArrayList<>();
        this.isCompleted = false;
    }

    public GameLevel(String levelId, String name, String description,
                     double initialWireLength, double levelDuration) {
        this();
        this.levelId = levelId;
        this.name = name;
        this.description = description;
        this.initialWireLength = initialWireLength;
        this.levelDuration = levelDuration;
    }



    public String getLevelId() {
        return levelId;
    }

    public void setLevelId(String levelId) {
        this.levelId = levelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getInitialWireLength() {
        return initialWireLength;
    }

    public void setInitialWireLength(double initialWireLength) {
        this.initialWireLength = initialWireLength;
    }

    public double getLevelDuration() {
        return levelDuration;
    }

    public void setLevelDuration(double levelDuration) {
        this.levelDuration = levelDuration;
    }

    /**
     * Alias for setLevelDuration for compatibility with GameController.
     */
    public void setDuration(double duration) {
        this.levelDuration = duration;
    }

    /**
     * Gets the packet injection schedule as a list of PacketInjection objects.
     */
    public List<PacketInjection> getPacketSchedule() {
        return packetSchedule;
    }

    /**
     * Sets the packet injection schedule.
     */
    public void setPacketSchedule(List<PacketInjection> packetSchedule) {
        this.packetSchedule = packetSchedule;

        // Bind source systems after setting the schedule
        if (this.packetSchedule != null) {
            bindPacketInjectionSources();
        }
    }

    /**
     * Binds packet injection source IDs to actual system references after JSON deserialization.
     */
    private void bindPacketInjectionSources() {
        if (packetSchedule == null || systems == null) return;

        for (PacketInjection injection : packetSchedule) {
            injection.bindSourceSystem(systems);
        }
    }

    public List<System> getSystems() {
        return systems;
    }

    public void setSystems(List<System> systems) {
        this.systems = systems;
        // Set parent level reference for all systems and parent pointers for their ports
        for (System system : systems) {
            system.setParentLevel(this);
            // Ensure ports are bound back to their parent system after JSON load
            if (system.getInputPorts() != null) {
                for (Port p : system.getInputPorts()) {
                    if (p != null) {
                        p.setParentSystem(system);
                        p.setInput(true);
                    }
                }
            }
            if (system.getOutputPorts() != null) {
                for (Port p : system.getOutputPorts()) {
                    if (p != null) {
                        p.setParentSystem(system);
                        p.setInput(false);
                    }
                }
            }
        }
    }

    public List<WireConnection> getWireConnections() {
        return wireConnections;
    }

    public void setWireConnections(List<WireConnection> wireConnections) {
        this.wireConnections = wireConnections;
    }

    public Map<Double, List<Packet>> getPacketInjectionSchedule() {
        return packetInjectionSchedule;
    }

    public void setPacketInjectionSchedule(Map<Double, List<Packet>> packetInjectionSchedule) {
        this.packetInjectionSchedule = packetInjectionSchedule;
    }

    public List<String> getConnectionRules() {
        return connectionRules;
    }

    public void setConnectionRules(List<String> connectionRules) {
        this.connectionRules = connectionRules;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    /**
     * Adds a system to this level.
     */
    public void addSystem(System system) {
        system.setParentLevel(this);
        // Ensure ports are bound back to this system
        if (system.getInputPorts() != null) {
            for (Port p : system.getInputPorts()) {
                if (p != null) {
                    p.setParentSystem(system);
                    p.setInput(true);
                }
            }
        }
        if (system.getOutputPorts() != null) {
            for (Port p : system.getOutputPorts()) {
                if (p != null) {
                    p.setParentSystem(system);
                    p.setInput(false);
                }
            }
        }
        systems.add(system);
    }

    /**
     * Adds a wire connection to this level.
     */
    public void addWireConnection(WireConnection connection) {
        if (connection != null) {
            wireConnections.add(connection);
        }
    }

    /**
     * Removes a wire connection from this level.
     */
    public void removeWireConnection(WireConnection connection) {
        if (connection != null) {
            wireConnections.remove(connection);
        }
    }

    /**
     * Checks if a wire connection exists between two ports.
     */
    public boolean hasWireConnection(Port port1, Port port2) {
        for (WireConnection connection : wireConnections) {
            if (connection.isActive()) {
                Port source = connection.getSourcePort();
                Port dest = connection.getDestinationPort();

                if ((source == port1 && dest == port2) || (source == port2 && dest == port1)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Schedules packet injection at a specific time.
     */
    public void schedulePacketInjection(double time, Packet packet) {
        packetInjectionSchedule.computeIfAbsent(time, k -> new ArrayList<>()).add(packet);
    }

    /**
     * Schedules multiple packet injections at a specific time.
     */
    public void schedulePacketInjection(double time, List<Packet> packets) {
        packetInjectionSchedule.computeIfAbsent(time, k -> new ArrayList<>()).addAll(packets);
    }

    /**
     * Gets all packets scheduled for injection at a specific time.
     */
    public List<Packet> getPacketsForTime(double time) {
        return packetInjectionSchedule.getOrDefault(time, new ArrayList<>());
    }

    /**
     * Converts packet injection schedule from JSON format to PacketInjection objects.
     * Handles both direct list format and legacy map format.
     */
    public void convertPacketScheduleFromJSON() {
        java.lang.System.out.println("DEBUG: convertPacketScheduleFromJSON called");
        java.lang.System.out.println("DEBUG: packetSchedule size: " + (packetSchedule != null ? packetSchedule.size() : "null"));
        java.lang.System.out.println("DEBUG: packetInjectionSchedule size: " + (packetInjectionSchedule != null ? packetInjectionSchedule.size() : "null"));

        boolean usedDirectList = false;

        // Preferred path: if packetSchedule already populated via JSON list, just resolve sources
        if (this.packetSchedule != null && !this.packetSchedule.isEmpty()) {
            java.lang.System.out.println("DEBUG: Using direct packetSchedule list with " + packetSchedule.size() + " items");
            resolvePacketInjectionSources();
            usedDirectList = true;
        } else {
            java.lang.System.out.println("DEBUG: packetSchedule is null or empty, will use legacy conversion");
        }

        // Backward-compatible path: convert legacy map<Double, List<Packet>> into PacketInjection list
        if (!usedDirectList) {
            List<PacketInjection> converted = new ArrayList<>();
            if (packetInjectionSchedule == null || packetInjectionSchedule.isEmpty()) {
                java.lang.System.out.println("DEBUG: No packet injection schedule found in JSON");
            } else {
                java.lang.System.out.println("DEBUG: Converting " + packetInjectionSchedule.size() + " packet injection entries from legacy JSON format");
                for (Map.Entry<Double, List<Packet>> entry : packetInjectionSchedule.entrySet()) {
                    double time = entry.getKey();
                    List<Packet> packets = entry.getValue();
                    for (Packet packet : packets) {
                        System sourceSystem = findSourceSystemForPacket(packet);
                        if (sourceSystem != null) {
                            PacketType packetType = packet.getPacketType();
                            PacketInjection injection = new PacketInjection(time, packetType, sourceSystem);
                            converted.add(injection);
                        }
                    }
                }
            }
            this.packetSchedule = converted;
        }

        // Sort by time
        if (packetSchedule != null) {
            packetSchedule.sort((a, b) -> Double.compare(a.getTime(), b.getTime()));
            java.lang.System.out.println("DEBUG: Final packet schedule has " + packetSchedule.size() + " injections");

            // Debug: print first few injections
            for (int i = 0; i < Math.min(3, packetSchedule.size()); i++) {
                PacketInjection inj = packetSchedule.get(i);
                java.lang.System.out.println("DEBUG: Injection " + i + ": time=" + inj.getTime() +
                        ", type=" + inj.getPacketType() + ", sourceId=" + inj.getSourceId() +
                        ", sourceSystem=" + (inj.getSourceSystem() != null ? inj.getSourceSystem().getId() : "null"));
            }
        }
    }

    /**
     * Resolves PacketInjection.sourceId to actual System reference after JSON load.
     */
    public void resolvePacketInjectionSources() {
        if (packetSchedule == null) return;
        Map<String, System> idToSystem = new HashMap<>();
        for (System system : systems) {
            idToSystem.put(system.getId(), system);
        }
        for (PacketInjection injection : packetSchedule) {
            if (injection.getSourceSystem() == null && injection.getSourceId() != null) {
                System sys = idToSystem.get(injection.getSourceId());
                if (sys != null) {
                    injection.setSourceSystem(sys);
                } else {
                    // Fallback: if no explicit source, use first source reference system
                    List<ReferenceSystem> sources = getSourceSystems();
                    if (!sources.isEmpty()) {
                        injection.setSourceSystem(sources.get(0));
                    }
                }
            }
        }
    }

    /**
     * Finds the source system for a packet based on its position.
     */
    private System findSourceSystemForPacket(Packet packet) {
        if (packet.getCurrentPosition() == null) {
            return null;
        }

        for (System system : systems) {
            if (system instanceof ReferenceSystem && ((ReferenceSystem) system).isSource()) {
                // Check if packet position is near any output port of this source system
                for (Port port : system.getOutputPorts()) {
                    if (port.getPosition() != null &&
                            isPositionNear(packet.getCurrentPosition(), port.getPosition())) {
                        return system;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Checks if two positions are near each other (within 50 pixels).
     */
    private boolean isPositionNear(Point2D pos1, Point2D pos2) {
        if (pos1 == null || pos2 == null) return false;
        double distance = Math.sqrt(Math.pow(pos1.getX() - pos2.getX(), 2) +
                Math.pow(pos1.getY() - pos2.getY(), 2));
        return distance < 50.0;
    }

    /**
     * Gets all reference systems.
     */
    @JsonIgnore
    public List<ReferenceSystem> getReferenceSystems() {
        List<ReferenceSystem> referenceSystems = new ArrayList<>();
        for (System system : systems) {
            if (system instanceof ReferenceSystem) {
                referenceSystems.add((ReferenceSystem) system);
            }
        }
        return referenceSystems;
    }

    /**
     * Gets all source reference systems.
     */
    @JsonIgnore
    public List<ReferenceSystem> getSourceSystems() {
        List<ReferenceSystem> sourceSystems = new ArrayList<>();
        for (ReferenceSystem refSystem : getReferenceSystems()) {
            if (refSystem.isSource()) {
                sourceSystems.add(refSystem);
            }
        }
        return sourceSystems;
    }

    /**
     * Gets all destination reference systems (all reference systems can receive packets).
     * Note: With the new dual-functionality, all reference systems are potential destinations.
     */
    @JsonIgnore
    public List<ReferenceSystem> getDestinationSystems() {
        // All reference systems can receive packets now
        return getReferenceSystems();
    }

    /**
     * Gets all regular systems (non-reference systems).
     */
    @JsonIgnore
    public List<System> getRegularSystems() {
        List<System> regularSystems = new ArrayList<>();
        for (System system : systems) {
            if (!(system instanceof ReferenceSystem)) {
                regularSystems.add(system);
            }
        }
        return regularSystems;
    }

    /**
     * Calculates the total wire length consumed by all connections.
     */
    @JsonIgnore
    public double getTotalWireLengthConsumed() {
        return wireConnections.stream()
                .mapToDouble(WireConnection::getConsumedLength)
                .sum();
    }

    /**
     * Gets the remaining wire length available.
     */
    @JsonIgnore
    public double getRemainingWireLength() {
        return initialWireLength - getTotalWireLengthConsumed();
    }

    /**
     * Checks if the level has enough wire length remaining.
     */
    @JsonIgnore
    public boolean hasSufficientWireLength() {
        return getRemainingWireLength() > 0;
    }

    /**
     * Validates the level configuration.
     */
    @JsonIgnore
    public boolean isValid() {
        // Check if there's at least one source and one destination
        if (getSourceSystems().isEmpty() || getDestinationSystems().isEmpty()) {
            return false;
        }

        // Check if all wire connections are valid
        for (WireConnection connection : wireConnections) {
            if (!connection.isValid()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "GameLevel{" +
                "levelId='" + levelId + '\'' +
                ", name='" + name + '\'' +
                ", initialWireLength=" + initialWireLength +
                ", levelDuration=" + levelDuration +
                ", systems=" + systems.size() +
                ", wireConnections=" + wireConnections.size() +
                ", completed=" + isCompleted +
                '}';
    }
}
