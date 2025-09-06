package controller;

import model.*;
import java.util.*;

/**
 * Handles wire connection logic and validation.
 */
public class WiringController {

    public WiringController() {
    }

    /**
     * Creates a wire connection between two ports.
     */
    public boolean createWireConnection(Port sourcePort, Port destinationPort, GameState gameState) {
        java.lang.System.out.println("Creating wire connection between ports:");
        java.lang.System.out.println("  Source port: " + sourcePort.getShape() + " at " + sourcePort.getPosition());
        java.lang.System.out.println("  Destination port: " + destinationPort.getShape() + " at " + destinationPort.getPosition());

        // Check if connection is valid
        if (!isValidConnection(sourcePort, destinationPort, gameState)) {
            java.lang.System.out.println("  Connection validation failed");
            return false;
        }

        // Calculate wire length
        double wireLength = calculateWireLength(sourcePort, destinationPort);
        java.lang.System.out.println("  Calculated wire length: " + wireLength);

        // Check if enough wire length is available
        if (wireLength > gameState.getRemainingWireLength()) {
            java.lang.System.out.println("  Not enough wire length available. Required: " + wireLength + ", Available: " + gameState.getRemainingWireLength());
            return false;
        }

        // Create wire connection
        WireConnection connection = new WireConnection(sourcePort, destinationPort, wireLength);
        java.lang.System.out.println("  Wire connection created with ID: " + connection.getId());
        java.lang.System.out.println("  Path points: " + connection.getPathPoints().size() + " points");
        for (int i = 0; i < connection.getPathPoints().size(); i++) {
            Point2D point = connection.getPathPoints().get(i);
            java.lang.System.out.println("    Point " + i + ": (" + point.getX() + ", " + point.getY() + ")");
        }

        gameState.addWireConnection(connection);

        // Update port connection status
        sourcePort.setConnected(true);
        destinationPort.setConnected(true);

        // Immediately update system indicators for both connected systems
        sourcePort.getParentSystem().update(0.0); // Force indicator update
        destinationPort.getParentSystem().update(0.0); // Force indicator update

        // Update remaining wire length
        gameState.setRemainingWireLength(gameState.getRemainingWireLength() - wireLength);

        java.lang.System.out.println("  Wire connection created successfully!");
        return true;
    }

    /**
     * Checks if a connection is valid.
     */
    private boolean isValidConnection(Port sourcePort, Port destinationPort, GameState gameState) {
        java.lang.System.out.println("  Validating connection...");

        // Check if ports are from the same system
        if (sourcePort.getParentSystem() == destinationPort.getParentSystem()) {
            java.lang.System.out.println("    FAILED: Ports are from the same system");
            return false;
        }

        // Port shape compatibility check - all shapes can now connect
        if (!sourcePort.getShape().isCompatibleWith(destinationPort.getShape())) {
            java.lang.System.out.println("    FAILED: Port shapes are not compatible");
            return false;
        }

        // Check if one is input and one is output (prevent output-to-output connections)
        if (sourcePort.isInput() == destinationPort.isInput()) {
            java.lang.System.out.println("    FAILED: Both ports are " + (sourcePort.isInput() ? "input" : "output") + " ports");
            return false;
        }

        // Check if connection already exists
        if (gameState.hasWireConnection(sourcePort, destinationPort)) {
            java.lang.System.out.println("    FAILED: Connection already exists");
            return false;
        }

        // Check if EITHER port is already connected (prevent multiple connections per port)
        if (sourcePort.isConnected() || destinationPort.isConnected()) {
            java.lang.System.out.println("    FAILED: One or both ports are already connected");
            return false;
        }

        // Check if wire would pass over any systems (Phase 2 requirement)
        WireConnection tempConnection = new WireConnection(sourcePort, destinationPort);
        if (tempConnection.passesOverSystems(gameState.getCurrentLevel().getSystems())) {
            java.lang.System.out.println("    FAILED: Wire would pass over systems");
            return false;
        }

        java.lang.System.out.println("    Connection validation passed");
        return true;
    }

    /**
     * Calculates wire length between two ports.
     */
    private double calculateWireLength(Port sourcePort, Port destinationPort) {
        Point2D sourcePos = sourcePort.getPosition();
        Point2D destPos = destinationPort.getPosition();
        return sourcePos.distanceTo(destPos);
    }

    /**
     * Calculates wire length including all bends.
     */
    private double calculateWireLengthWithBends(WireConnection connection) {
        return connection.getTotalLength();
    }

    /**
     * Checks if the network is a connected graph.
     */
    public boolean isNetworkConnected(GameState gameState) {
        List<model.System> systems = gameState.getCurrentLevel().getSystems();
        if (systems.isEmpty()) {
            return false;
        }

        // Use depth-first search to check connectivity
        Set<String> visited = new HashSet<>();
        dfs(systems.get(0), visited, gameState);

        // Check if all systems are reachable
        return visited.size() == systems.size();
    }

    /**
     * Counts how many systems are reachable from the first system via active wires.
     * This is useful for UI to display "reachable X/Y" without forcing full connectivity.
     */
    public int getReachableSystemCount(GameState gameState) {
        List<model.System> systems = gameState.getCurrentLevel().getSystems();
        if (systems.isEmpty()) {
            return 0;
        }

        Set<String> visited = new HashSet<>();
        dfs(systems.get(0), visited, gameState);
        return visited.size();
    }

    /**
     * Checks if all ports in the network are connected.
     * This ensures that every port on every system has a wire connection.
     */
    public boolean areAllPortsConnected(GameState gameState) {
        List<model.System> systems = gameState.getCurrentLevel().getSystems();
        if (systems.isEmpty()) {
            java.lang.System.out.println("DEBUG: areAllPortsConnected - no systems found");
            return false;
        }

        java.lang.System.out.println("DEBUG: areAllPortsConnected - checking " + systems.size() + " systems");
        for (model.System system : systems) {
            // Skip systems that don't have any ports (like HUD display elements)
            List<Port> allPorts = system.getAllPorts();
            if (allPorts.isEmpty()) {
                java.lang.System.out.println("DEBUG: System " + system.getClass().getSimpleName() + " has no ports, skipping");
                continue;
            }

            java.lang.System.out.println("DEBUG: System " + system.getClass().getSimpleName() + " has " + allPorts.size() + " ports");
            for (Port port : allPorts) {
                java.lang.System.out.println("DEBUG: Port " + port.getShape() + " " + (port.isInput() ? "input" : "output") + " connected: " + port.isConnected());
                if (!port.isConnected()) {
                    java.lang.System.out.println("DEBUG: Found unconnected port " + port.getShape() + " " + (port.isInput() ? "input" : "output"));
                    return false; // Found an unconnected port
                }
            }
        }

        java.lang.System.out.println("DEBUG: All ports are connected!");
        return true; // All ports are connected
    }

    /**
     * Gets the count of connected ports vs total ports in the network.
     * Useful for UI to display port connectivity status.
     */
    public int[] getPortConnectivityCounts(GameState gameState) {
        List<model.System> systems = gameState.getCurrentLevel().getSystems();
        int totalPorts = 0;
        int connectedPorts = 0;

        for (model.System system : systems) {
            // Skip systems that don't have any ports (like HUD display elements)
            List<Port> allPorts = system.getAllPorts();
            if (allPorts.isEmpty()) {
                continue;
            }

            totalPorts += allPorts.size();
            for (Port port : allPorts) {
                if (port.isConnected()) {
                    connectedPorts++;
                }
            }
        }

        return new int[]{connectedPorts, totalPorts};
    }

    /**
     * Gets a list of unconnected ports in the network.
     * Useful for providing user feedback about what needs to be completed.
     */
    public List<Port> getUnconnectedPorts(GameState gameState) {
        List<Port> unconnectedPorts = new ArrayList<>();
        List<model.System> systems = gameState.getCurrentLevel().getSystems();

        for (model.System system : systems) {
            // Skip systems that don't have any ports (like HUD display elements)
            List<Port> allPorts = system.getAllPorts();
            if (allPorts.isEmpty()) {
                continue;
            }

            for (Port port : allPorts) {
                if (!port.isConnected()) {
                    unconnectedPorts.add(port);
                }
            }
        }

        return unconnectedPorts;
    }

    /**
     * Validates level design for port connectivity feasibility.
     * Checks if it's mathematically possible to connect all ports.
     * Returns a validation result with details about any issues found.
     */
    public LevelValidationResult validateLevelDesign(GameState gameState) {
        List<model.System> systems = gameState.getCurrentLevel().getSystems();
        int totalInputPorts = 0;
        int totalOutputPorts = 0;
        Map<PortShape, Integer> inputPortShapes = new HashMap<>();
        Map<PortShape, Integer> outputPortShapes = new HashMap<>();

        for (model.System system : systems) {
            List<Port> allPorts = system.getAllPorts();
            if (allPorts.isEmpty()) {
                continue;
            }

            for (Port port : allPorts) {
                if (port.isInput()) {
                    totalInputPorts++;
                    inputPortShapes.merge(port.getShape(), 1, Integer::sum);
                } else {
                    totalOutputPorts++;
                    outputPortShapes.merge(port.getShape(), 1, Integer::sum);
                }
            }
        }

        // Check if total port counts are balanced
        boolean balancedPorts = totalInputPorts == totalOutputPorts;

        // Check if port shapes are compatible
        boolean compatibleShapes = true;
        StringBuilder shapeIssues = new StringBuilder();

        for (PortShape shape : PortShape.values()) {
            int inputCount = inputPortShapes.getOrDefault(shape, 0);
            int outputCount = outputPortShapes.getOrDefault(shape, 0);

            if (inputCount != outputCount) {
                compatibleShapes = false;
                shapeIssues.append(String.format(" %s: %d input vs %d output,",
                        shape, inputCount, outputCount));
            }
        }

        return new LevelValidationResult(
                balancedPorts,
                compatibleShapes,
                totalInputPorts,
                totalOutputPorts,
                inputPortShapes,
                outputPortShapes,
                shapeIssues.toString()
        );
    }

    /**
     * Depth-first search to check network connectivity.
     */
    private void dfs(model.System system, Set<String> visited, GameState gameState) {
        visited.add(system.getId());

        // Get all connected systems
        for (WireConnection connection : gameState.getWireConnections()) {
            if (connection.isActive()) {
                model.System connectedSystem = null;

                if (connection.getSourcePort().getParentSystem().getId().equals(system.getId())) {
                    connectedSystem = connection.getDestinationPort().getParentSystem();
                } else if (connection.getDestinationPort().getParentSystem().getId().equals(system.getId())) {
                    connectedSystem = connection.getSourcePort().getParentSystem();
                }

                if (connectedSystem != null && !visited.contains(connectedSystem.getId())) {
                    dfs(connectedSystem, visited, gameState);
                }
            }
        }
    }

    /**
     * Validates that the network becomes a connected graph after adding a connection.
     */
    public boolean willCreateConnectedGraph(Port sourcePort, Port destinationPort, GameState gameState) {
        // Temporarily add the connection
        WireConnection tempConnection = new WireConnection(sourcePort, destinationPort, 0.0);
        gameState.addWireConnection(tempConnection);

        // Check if network is connected
        boolean isConnected = isNetworkConnected(gameState);

        // Remove temporary connection
        gameState.removeWireConnection(tempConnection);

        return isConnected;
    }

    /**
     * Adds a bend to a wire connection with system validation.
     */
    public boolean addBendToWire(WireConnection connection, Point2D bendPosition, GameState gameState) {
        if (connection == null || !connection.isActive()) {
            return false;
        }

        // Check if player has enough coins (1 coin per bend)
        if (gameState.getCoins() < 1) {
            return false;
        }

        // Calculate the wire length before adding the bend
        double lengthBeforeBend = connection.getTotalLength();

        // Try to add the bend
        if (connection.addBend(bendPosition, gameState.getCurrentLevel().getSystems())) {
            // Calculate the new wire length after adding the bend
            double lengthAfterBend = connection.getTotalLength();
            double lengthIncrease = lengthAfterBend - lengthBeforeBend;

            // Check if there's enough remaining wire length for the bend
            if (lengthIncrease > gameState.getRemainingWireLength()) {
                // Not enough wire length, remove the bend from the list and fail
                List<WireBend> bends = connection.getBends();
                if (!bends.isEmpty()) {
                    bends.remove(bends.size() - 1);
                }
                return false;
            }

            // Deduct the additional wire length from the available pool
            gameState.setRemainingWireLength(gameState.getRemainingWireLength() - lengthIncrease);

            // Deduct coin cost
            gameState.spendCoins(1);
            return true;
        }

        return false;
    }

    /**
     * Moves a bend on a wire connection with system validation.
     */
    public boolean moveBendOnWire(WireConnection connection, int bendIndex, Point2D newPosition, GameState gameState) {
        if (connection == null || !connection.isActive()) {
            return false;
        }

        // Calculate the wire length before moving the bend
        double lengthBeforeMove = connection.getTotalLength();

        // Try to move the bend with more permissive validation for better user experience
        if (connection.moveBendPermissive(bendIndex, newPosition, gameState.getCurrentLevel().getSystems())) {
            // Calculate the new wire length after moving the bend
            double lengthAfterMove = connection.getTotalLength();
            double lengthChange = lengthAfterMove - lengthBeforeMove;

            // Update the remaining wire length based on the change
            if (lengthChange > 0) {
                // Wire became longer, need to deduct additional length
                if (lengthChange > gameState.getRemainingWireLength()) {
                    // Not enough wire length, revert the move and fail
                    // We need to restore the original position
                    Point2D originalPosition = connection.getBends().get(bendIndex).getPosition();
                    connection.moveBend(bendIndex, originalPosition, gameState.getCurrentLevel().getSystems());
                    return false;
                }
                gameState.setRemainingWireLength(gameState.getRemainingWireLength() - lengthChange);
            } else if (lengthChange < 0) {
                // Wire became shorter, add the saved length back to the pool
                gameState.setRemainingWireLength(gameState.getRemainingWireLength() - lengthChange);
            }

            return true;
        }

        return false;
    }

    /**
     * Moves a bend on a wire with maximum freedom (no length constraints).
     * This method allows completely free bend movement for the best user experience.
     */
    public boolean moveBendFreely(WireConnection connection, int bendIndex, Point2D newPosition, GameState gameState) {
        if (connection == null || !connection.isActive()) {
            return false;
        }

        // Use the permissive movement method for maximum freedom
        if (connection.moveBendPermissive(bendIndex, newPosition, gameState.getCurrentLevel().getSystems())) {
            // Don't check wire length constraints - allow free movement
            // This gives users complete control over wire bending
            return true;
        }

        return false;
    }

    /**
     * Removes a wire connection and restores its length to the available wire pool.
     * Phase 2: Prevents deletion of connections from previous levels.
     */
    public boolean removeWireConnection(WireConnection connection, GameState gameState) {
        if (connection == null || !connection.isActive()) {
            return false;
        }



        // Get the total length of the wire including bends
        double wireLength = connection.getTotalLength();

        // Disconnect the ports
        Port sourcePort = connection.getSourcePort();
        Port destinationPort = connection.getDestinationPort();

        if (sourcePort != null) {
            sourcePort.setConnected(false);
        }
        if (destinationPort != null) {
            destinationPort.setConnected(false);
        }

        // Immediately update system indicators for both disconnected systems
        if (sourcePort != null) {
            sourcePort.getParentSystem().update(0.0); // Force indicator update
        }
        if (destinationPort != null) {
            destinationPort.getParentSystem().update(0.0); // Force indicator update
        }

        // Remove the connection from the game state
        gameState.removeWireConnection(connection);

        // Restore the wire length to available pool
        double currentLength = gameState.getRemainingWireLength();
        gameState.setRemainingWireLength(currentLength + wireLength);

        // Deactivate the connection
        connection.setActive(false);

        return true;
    }

    /**
     * Merges two compatible wire connections into one.
     * This functionality allows connecting wires that share a common system.
     */
    public boolean mergeWireConnections(WireConnection wire1, WireConnection wire2, GameState gameState) {
        if (wire1 == null || wire2 == null || !wire1.isActive() || !wire2.isActive()) {
            return false;
        }

        // Find the common system/port between the two wires
        Port commonPort = findCommonPort(wire1, wire2);
        if (commonPort == null) {
            return false; // No common connection point
        }

        // Determine the new connection endpoints
        Port newSourcePort = getOtherPort(wire1, commonPort);
        Port newDestinationPort = getOtherPort(wire2, commonPort);

        if (newSourcePort == null || newDestinationPort == null) {
            return false;
        }

        // Check if the new connection would be valid
        if (!isValidConnection(newSourcePort, newDestinationPort, gameState)) {
            return false;
        }

        // Calculate total length needed for the merged wire
        double totalLength = wire1.getTotalLength() + wire2.getTotalLength();

        // Create the new merged connection
        WireConnection mergedConnection = new WireConnection(newSourcePort, newDestinationPort, totalLength);

        // Remove the old connections (without restoring length since we're using it)
        wire1.setActive(false);
        wire2.setActive(false);
        gameState.removeWireConnection(wire1);
        gameState.removeWireConnection(wire2);

        // Add the new merged connection
        gameState.addWireConnection(mergedConnection);

        // Update port connections
        newSourcePort.setConnected(true);
        newDestinationPort.setConnected(true);
        commonPort.setConnected(false); // Common port is no longer connected

        // Immediately update system indicators for all affected systems
        newSourcePort.getParentSystem().update(0.0); // Force indicator update
        newDestinationPort.getParentSystem().update(0.0); // Force indicator update
        commonPort.getParentSystem().update(0.0); // Force indicator update

        return true;
    }

    /**
     * Finds a common port between two wire connections.
     */
    private Port findCommonPort(WireConnection wire1, WireConnection wire2) {
        if (wire1.getSourcePort() == wire2.getSourcePort() ||
                wire1.getSourcePort() == wire2.getDestinationPort()) {
            return wire1.getSourcePort();
        }
        if (wire1.getDestinationPort() == wire2.getSourcePort() ||
                wire1.getDestinationPort() == wire2.getDestinationPort()) {
            return wire1.getDestinationPort();
        }
        return null;
    }

    /**
     * Gets the port that is not the specified port from a wire connection.
     */
    private Port getOtherPort(WireConnection connection, Port excludePort) {
        if (connection.getSourcePort() == excludePort) {
            return connection.getDestinationPort();
        } else if (connection.getDestinationPort() == excludePort) {
            return connection.getSourcePort();
        }
        return null;
    }

    /**
     * Validates that a system can be moved to a new position without breaking constraints.
     * Checks wire length budget and prevents wires from passing over systems.
     */
    public boolean canMoveSystem(model.System system, Point2D newPosition, GameState gameState) {
        if (system instanceof ReferenceSystem) {
            return false; // Cannot move reference systems
        }

        Point2D originalPosition = system.getPosition();
        List<model.System> otherSystems = new ArrayList<>(gameState.getCurrentLevel().getSystems());
        otherSystems.remove(system);

        // Temporarily move the system to check constraints
        system.setPosition(newPosition);

        // Update all connected ports' positions
        for (Port port : system.getInputPorts()) {
            port.updatePositionRelativeToSystem();
        }
        for (Port port : system.getOutputPorts()) {
            port.updatePositionRelativeToSystem();
        }

        boolean canMove = true;

        // Check if any connected wires would exceed wire length budget
        double totalWireLengthChange = 0.0;
        for (WireConnection connection : gameState.getWireConnections()) {
            if (connection.isActive() &&
                    (connection.getSourcePort().getParentSystem() == system ||
                            connection.getDestinationPort().getParentSystem() == system)) {

                double oldLength = connection.getWireLength();
                double newLength = connection.getTotalLength(); // Recalculated with new position
                totalWireLengthChange += (newLength - oldLength);

                // Check if any wire would pass over other systems
                if (connection.passesOverSystems(otherSystems)) {
                    canMove = false;
                    break;
                }
            }
        }

        // Check if total wire length change would exceed available budget
        if (canMove && totalWireLengthChange > gameState.getRemainingWireLength()) {
            canMove = false;
        }

        // Restore original position if move is not valid
        if (!canMove) {
            system.setPosition(originalPosition);
            for (Port port : system.getInputPorts()) {
                port.updatePositionRelativeToSystem();
            }
            for (Port port : system.getOutputPorts()) {
                port.updatePositionRelativeToSystem();
            }
        }

        return canMove;
    }

    /**
     * Moves a system to a new position and updates wire lengths accordingly.
     * Should only be called after canMoveSystem() returns true.
     */
    public boolean moveSystem(model.System system, Point2D newPosition, GameState gameState) {
        if (!canMoveSystem(system, newPosition, gameState)) {
            return false;
        }

        // Calculate wire length changes before moving
        double totalWireLengthChange = 0.0;
        List<WireConnection> affectedConnections = new ArrayList<>();

        for (WireConnection connection : gameState.getWireConnections()) {
            if (connection.isActive() &&
                    (connection.getSourcePort().getParentSystem() == system ||
                            connection.getDestinationPort().getParentSystem() == system)) {

                double oldLength = connection.getWireLength();
                affectedConnections.add(connection);
                totalWireLengthChange += (connection.getTotalLength() - oldLength);
            }
        }

        // Move the system
        system.setPosition(newPosition);

        // Update port positions
        for (Port port : system.getInputPorts()) {
            port.updatePositionRelativeToSystem();
        }
        for (Port port : system.getOutputPorts()) {
            port.updatePositionRelativeToSystem();
        }

        // Update wire lengths and consume additional wire length
        for (WireConnection connection : affectedConnections) {
            double newLength = connection.getTotalLength();
            connection.setWireLength(newLength);
        }

        // Deduct the additional wire length from available budget
        if (totalWireLengthChange > 0) {
            gameState.setRemainingWireLength(gameState.getRemainingWireLength() - totalWireLengthChange);
        } else {
            // If wires became shorter, add the saved length back
            gameState.setRemainingWireLength(gameState.getRemainingWireLength() - totalWireLengthChange);
        }

        return true;
    }

    /**
     * Gets the total wire length used by all active connections.
     * Useful for HUD display and wire length management.
     */
    public double getTotalWireLengthUsed(GameState gameState) {
        double totalUsed = 0.0;
        for (WireConnection connection : gameState.getWireConnections()) {
            if (connection.isActive()) {
                totalUsed += connection.getTotalLength();
            }
        }
        return totalUsed;
    }

    /**
     * Gets the total wire length available in the level.
     * This is the sum of remaining wire length and used wire length.
     */
    public double getTotalWireLengthAvailable(GameState gameState) {
        return gameState.getRemainingWireLength() + getTotalWireLengthUsed(gameState);
    }

    /**
     * Checks if there's enough wire length available to add a bend at the specified position.
     * This method calculates the potential length increase without actually adding the bend.
     */
    public boolean canAddBend(WireConnection connection, Point2D bendPosition, GameState gameState) {
        if (connection == null || !connection.isActive()) {
            return false;
        }

        // Calculate the wire length before adding the bend
        double lengthBeforeBend = connection.getTotalLength();

        // Temporarily add the bend to calculate the new length
        List<WireBend> originalBends = new ArrayList<>(connection.getBends());
        WireBend tempBend = new WireBend(bendPosition, 50.0);
        connection.getBends().add(tempBend);

        // Calculate the new wire length after adding the bend
        double lengthAfterBend = connection.getTotalLength();
        double lengthIncrease = lengthAfterBend - lengthBeforeBend;

        // Remove the temporary bend
        connection.getBends().remove(tempBend);

        // Check if there's enough remaining wire length for the bend
        return lengthIncrease <= gameState.getRemainingWireLength();
    }

    /**
     * Checks if moving a bend to a new position would require additional wire length.
     * This method calculates the potential length change without actually moving the bend.
     */
    public boolean canMoveBend(WireConnection connection, int bendIndex, Point2D newPosition, GameState gameState) {
        if (connection == null || !connection.isActive() || bendIndex < 0 || bendIndex >= connection.getBends().size()) {
            return false;
        }

        // Calculate the wire length before moving the bend
        double lengthBeforeMove = connection.getTotalLength();

        // Temporarily move the bend to calculate the new length
        WireBend bend = connection.getBends().get(bendIndex);
        Point2D originalPosition = bend.getPosition();
        bend.moveTo(newPosition, originalPosition);

        // Calculate the new wire length after moving the bend
        double lengthAfterMove = connection.getTotalLength();
        double lengthChange = lengthAfterMove - lengthBeforeMove;

        // Restore the original position
        bend.moveTo(originalPosition, newPosition);

        // If the wire would become longer, check if there's enough wire length available
        if (lengthChange > 0) {
            return lengthChange <= gameState.getRemainingWireLength();
        }

        // If the wire would become shorter or stay the same, it's always allowed
        return true;
    }
}
