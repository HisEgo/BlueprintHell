package controller;

import model.*;
import model.AbilityType;

import java.util.*;
import java.util.List;

public class CollisionController {

    private GameController gameController;
    private static final int GRID_SIZE = 50; // Size of each grid cell
    private Map<String, List<Packet>> spatialGrid;
    private Map<String, Double> collisionCooldowns; // Track collision cooldowns
    private static final double COLLISION_COOLDOWN = 1.0; // 1 second cooldown

    public CollisionController() {
        this.spatialGrid = new HashMap<>();
        this.collisionCooldowns = new HashMap<>();
    }

    public CollisionController(GameController gameController) {
        this.gameController = gameController;
        this.spatialGrid = new HashMap<>();
        this.collisionCooldowns = new HashMap<>();
    }

    public void setGameController(GameController gameController) {
        this.gameController = gameController;
    }

    private void updateSpatialGrid(List<Packet> packets) {
        spatialGrid.clear();

        for (Packet packet : packets) {
            if (packet.isActive()) {
                String gridKey = getGridKey(packet.getCurrentPosition());
                spatialGrid.computeIfAbsent(gridKey, k -> new ArrayList<>()).add(packet);
            }
        }
    }

    private String getGridKey(Point2D position) {
        int gridX = (int) (position.getX() / GRID_SIZE);
        int gridY = (int) (position.getY() / GRID_SIZE);
        return gridX + "," + gridY;
    }

    private List<Packet> getNeighboringPackets(Point2D position) {
        List<Packet> neighboringPackets = new ArrayList<>();
        String centerKey = getGridKey(position);

        // Get center cell and 8 neighboring cells
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                String key = getNeighborKey(centerKey, dx, dy);
                List<Packet> cellPackets = spatialGrid.get(key);
                if (cellPackets != null) {
                    neighboringPackets.addAll(cellPackets);
                }
            }
        }

        return neighboringPackets;
    }

    private String getNeighborKey(String centerKey, int dx, int dy) {
        String[] parts = centerKey.split(",");
        int gridX = Integer.parseInt(parts[0]) + dx;
        int gridY = Integer.parseInt(parts[1]) + dy;
        return gridX + "," + gridY;
    }

    public void checkCollisions(List<Packet> allPackets) {
        if (allPackets == null || allPackets.size() < 2) {
            return;
        }

        // Update collision cooldowns (reduce over time)
        double currentTime = gameController != null ? gameController.getGameState().getTemporalProgress() : 0.0;
        updateCollisionCooldowns(currentTime);

        // Build spatial grid for broad-phase
        updateSpatialGrid(allPackets);

        // Narrow-phase only among neighbors
        Set<String> processedPairs = new HashSet<>();
        for (Packet packet1 : allPackets) {
            if (!packet1.isActive()) continue;
            List<Packet> candidates = getNeighboringPackets(packet1.getCurrentPosition());
            for (Packet packet2 : candidates) {
                if (packet1 == packet2 || !packet2.isActive()) continue;

                String minId = packet1.getId().compareTo(packet2.getId()) < 0 ? packet1.getId() : packet2.getId();
                String maxId = packet1.getId().compareTo(packet2.getId()) < 0 ? packet2.getId() : packet1.getId();
                String pairId = minId + ":" + maxId;

                if (processedPairs.contains(pairId)) continue;
                processedPairs.add(pairId);

                // Skip if this pair is in cooldown
                if (collisionCooldowns.containsKey(pairId)) {
                    continue;
                }

                double distance = packet1.getCurrentPosition().distanceTo(packet2.getCurrentPosition());
                double threshold = packet1.getSize() + packet2.getSize();

                if (distance <= threshold) {

                    // Add cooldown for this pair
                    collisionCooldowns.put(pairId, currentTime + COLLISION_COOLDOWN);

                    // Handle the collision
                    handleCollision(packet1, packet2, allPackets);
                }
            }
        }
    }

    private void updateCollisionCooldowns(double currentTime) {
        collisionCooldowns.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
    }

    private boolean checkCollision(Packet packet1, Packet packet2) {
        Point2D pos1 = packet1.getCurrentPosition();
        Point2D pos2 = packet2.getCurrentPosition();

        double distance = pos1.distanceTo(pos2);
        double collisionThreshold = packet1.getSize() + packet2.getSize();

        boolean shouldCollide = distance <= collisionThreshold;
        if (shouldCollide) {
            java.lang.System.out.println("COLLISION CHECK: distance=" + distance + ", threshold=" + collisionThreshold + " -> COLLIDE");
        }

        return shouldCollide;
    }

    private void handleCollision(Packet packet1, Packet packet2, List<Packet> allPackets) {
        // Check if collisions are disabled by ability
        if (gameController != null && gameController.isAbilityActive(AbilityType.O_AIRYAMAN)) {
            return; // Collisions disabled
        }

        // FIRST: Separate the packets to prevent them from getting stuck
        separatePackets(packet1, packet2);

        // Play collision sound
        if (gameController != null && gameController.getSoundManager() != null) {
            gameController.getSoundManager().playCollisionSound();
        }

        // Handle special collision behaviors based on packet size and type
        handleSpecialCollisionBehaviors(packet1, packet2);

        // Create shockwave effect (unless disabled by ability)
        if (gameController == null || !gameController.isAbilityActive(AbilityType.O_ATAR)) {
            createShockwave(packet1, packet2, allPackets);
        }
    }

    private void separatePackets(Packet packet1, Packet packet2) {
        Point2D pos1 = packet1.getCurrentPosition();
        Point2D pos2 = packet2.getCurrentPosition();

        // Calculate separation vector
        Vec2D separation = new Vec2D(pos1.getX() - pos2.getX(), pos1.getY() - pos2.getY());
        double distance = separation.magnitude();

        // If packets are exactly on top of each other, create a random separation
        if (distance < 0.1) {
            separation = new Vec2D(Math.random() - 0.5, Math.random() - 0.5);
            distance = separation.magnitude();
        }

        // Normalize and scale separation
        if (distance > 0) {
            separation = separation.normalize();
            double minSeparation = (packet1.getSize() + packet2.getSize()) * 1.5; // 1.5x for buffer

            // Move packets apart
            Vec2D offset = separation.scale(minSeparation / 2.0);
            packet1.setCurrentPosition(new Point2D(pos1.getX() + offset.getX(), pos1.getY() + offset.getY()));
            packet2.setCurrentPosition(new Point2D(pos2.getX() - offset.getX(), pos2.getY() - offset.getY()));

        }
    }

    private void handleSpecialCollisionBehaviors(Packet packet1, Packet packet2) {
        // Size 1 packets reverse direction after collision (Phase 2 requirement)
        if (packet1.getSize() == 1) {
            packet1.reverseDirection();
        }
        if (packet2.getSize() == 1) {
            packet2.reverseDirection();
        }

        // Handle packet collision - both packets increase noise when colliding
        // Both packets get +1 noise, and are destroyed if noise >= size
        double newNoise1 = packet1.getNoiseLevel() + 1.0;
        packet1.setNoiseLevel(newNoise1);
        if (newNoise1 >= packet1.getSize()) {
            packet1.setActive(false);
            packet1.setLost(true);
        }
        
        double newNoise2 = packet2.getNoiseLevel() + 1.0;
        packet2.setNoiseLevel(newNoise2);
        if (newNoise2 >= packet2.getSize()) {
            packet2.setActive(false);
            packet2.setLost(true);
        }
        
        java.lang.System.out.println("*** COLLISION *** Both packets noise increased: " + 
                packet1.getPacketType() + " noise " + (newNoise1 - 1) + " -> " + newNoise1 + 
                " (size " + packet1.getSize() + "), " + packet2.getPacketType() + " noise " + 
                (newNoise2 - 1) + " -> " + newNoise2 + " (size " + packet2.getSize() + ")");
    }

    private void createShockwave(Packet packet1, Packet packet2, List<Packet> allPackets) {
        Point2D collisionPoint = new Point2D(
                (packet1.getCurrentPosition().getX() + packet2.getCurrentPosition().getX()) / 2,
                (packet1.getCurrentPosition().getY() + packet2.getCurrentPosition().getY()) / 2
        );


        for (Packet packet : allPackets) {
            if (packet == packet1 || packet == packet2) {
                continue; // Skip the colliding packets themselves
            }

            double distance = collisionPoint.distanceTo(packet.getCurrentPosition());

            // Limit shockwave radius to 100 pixels
            if (distance <= 100.0) {
                double strength = 1.0 - (distance / 100.0);

                // Skip applying shockwave to size 1 packets that have already been reversed
                if (packet.getSize() == 1 && packet.isReversing()) {
                    continue;
                }

                // Apply shockwave effect to movement vector
                Vec2D currentMovement = packet.getMovementVector();
                Vec2D shockwaveVector = new Vec2D(
                        (collisionPoint.getX() - packet.getCurrentPosition().getX()) * strength * 0.2,
                        (collisionPoint.getY() - packet.getCurrentPosition().getY()) * strength * 0.2
                );

                Vec2D newMovement = new Vec2D(
                        currentMovement.getX() + shockwaveVector.getX(),
                        currentMovement.getY() + shockwaveVector.getY()
                );

                packet.setMovementVector(newMovement);

                // Apply shockwave effect to increase noise level
                packet.applyShockwave(shockwaveVector);
            }
        }
    }

    private List<Packet> getAllActivePackets() {
        // For now, we'll use a simple approach - this should be replaced with proper game state access
        List<Packet> allPackets = new ArrayList<>();

        // If we have a game controller, get packets from there
        if (gameController != null && gameController.getGameState() != null) {
            allPackets.addAll(gameController.getGameState().getActivePackets());
        }

        return allPackets;
    }

    private Vec2D calculateShockwaveVector(Packet packet1, Packet packet2) {
        Point2D pos1 = packet1.getCurrentPosition();
        Point2D pos2 = packet2.getCurrentPosition();

        Vec2D direction = new Vec2D(
                pos2.getX() - pos1.getX(),
                pos2.getY() - pos1.getY()
        );

        return direction.normalize().scale(20.0);
    }

    private GameController getGameController() {
        return gameController;
    }
}
