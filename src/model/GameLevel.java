package model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

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

    public void setDuration(double duration) {
        this.levelDuration = duration;
    }

    public List<PacketInjection> getPacketSchedule() {
        return packetSchedule;
    }

    public void setPacketSchedule(List<PacketInjection> packetSchedule) {
        this.packetSchedule = packetSchedule;

        // Bind source systems after setting the schedule
        if (this.packetSchedule != null) {
            bindPacketInjectionSources();
        }
    }

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

    public void addWireConnection(WireConnection connection) {
        if (connection != null) {
            wireConnections.add(connection);
        }
    }

    public void removeWireConnection(WireConnection connection) {
        if (connection != null) {
            wireConnections.remove(connection);
        }
    }

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

    public void schedulePacketInjection(double time, Packet packet) {
        packetInjectionSchedule.computeIfAbsent(time, k -> new ArrayList<>()).add(packet);
    }

    public void schedulePacketInjection(double time, List<Packet> packets) {
        packetInjectionSchedule.computeIfAbsent(time, k -> new ArrayList<>()).addAll(packets);
    }

    public List<Packet> getPacketsForTime(double time) {
        return packetInjectionSchedule.getOrDefault(time, new ArrayList<>());
    }

    public void convertPacketScheduleFromJSON() {
        boolean usedDirectList = false;

        // Preferred path: if packetSchedule already populated via JSON list, just resolve sources
        if (this.packetSchedule != null && !this.packetSchedule.isEmpty()) {
            resolvePacketInjectionSources();
            usedDirectList = true;
        }

        // Backward-compatible path: convert legacy map<Double, List<Packet>> into PacketInjection list
        if (!usedDirectList) {
            List<PacketInjection> converted = new ArrayList<>();
            if (packetInjectionSchedule != null && !packetInjectionSchedule.isEmpty()) {
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
        }
    }

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

    private boolean isPositionNear(Point2D pos1, Point2D pos2) {
        if (pos1 == null || pos2 == null) return false;
        double distance = Math.sqrt(Math.pow(pos1.getX() - pos2.getX(), 2) +
                Math.pow(pos1.getY() - pos2.getY(), 2));
        return distance < 50.0;
    }

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

    @JsonIgnore
    public List<ReferenceSystem> getDestinationSystems() {
        // All reference systems can receive packets now
        return getReferenceSystems();
    }

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

    @JsonIgnore
    public double getTotalWireLengthConsumed() {
        return wireConnections.stream()
                .mapToDouble(WireConnection::getConsumedLength)
                .sum();
    }

    @JsonIgnore
    public double getRemainingWireLength() {
        return initialWireLength - getTotalWireLengthConsumed();
    }

    @JsonIgnore
    public boolean hasSufficientWireLength() {
        return getRemainingWireLength() > 0;
    }

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
