package controller;

import model.*;
import java.util.*;

public class AbilityManager {

    // Active ability effects with their expiration times
    private Map<String, AbilityEffect> activeEffects;
    private GameController gameController;
    private MovementController movementController;

    // Point selection state
    private boolean awaitingPointSelection;
    private AbilityType pendingAbility;
    private Point2D selectedPoint;
    private WireConnection selectedWire;

    public AbilityManager(GameController gameController, MovementController movementController) {
        this.activeEffects = new HashMap<>();
        this.gameController = gameController;
        this.movementController = movementController;
        this.awaitingPointSelection = false;
    }

    public boolean activateAbility(AbilityType abilityType, Point2D clickPoint) {
        switch (abilityType) {
            case SCROLL_OF_AERGIA:
                return activateAergia(clickPoint);
            case SCROLL_OF_SISYPHUS:
                return activateSisyphus(clickPoint);
            case SCROLL_OF_ELIPHAS:
                return activateEliphas(clickPoint);
            default:
                return false;
        }
    }

    private boolean activateAergia(Point2D clickPoint) {
        WireConnection wire = findWireAtPoint(clickPoint);
        if (wire == null) {
            return false;
        }

        // Calculate the exact point on the wire
        Point2D wirePoint = wire.getClosestPointOnWire(clickPoint);
        double progress = wire.getProgressAtPoint(wirePoint);

        // Create the effect
        AergiaEffect effect = new AergiaEffect(wire, progress, wirePoint, 20.0); // 20 second duration
        String effectId = "aergia_" + java.lang.System.currentTimeMillis();
        activeEffects.put(effectId, effect);

        java.lang.System.out.println("Scroll of Aergia activated at " + wirePoint + " on wire " + wire.getId());
        return true;
    }

    private boolean activateSisyphus(Point2D clickPoint) {
        // Find system at click point
        model.System targetSystem = findSystemAtPoint(clickPoint);
        if (targetSystem == null || targetSystem instanceof ReferenceSystem) {
            return false;
        }

        // Create movement effect
        SisyphusEffect effect = new SisyphusEffect(targetSystem, clickPoint, 30.0); // 30 second duration
        String effectId = "sisyphus_" + java.lang.System.currentTimeMillis();
        activeEffects.put(effectId, effect);

        java.lang.System.out.println("Scroll of Sisyphus activated - system " + targetSystem.getId() + " can now be moved");
        return true;
    }

    private boolean activateEliphas(Point2D clickPoint) {
        WireConnection wire = findWireAtPoint(clickPoint);
        if (wire == null) {
            return false;
        }

        Point2D wirePoint = wire.getClosestPointOnWire(clickPoint);
        double progress = wire.getProgressAtPoint(wirePoint);

        // Create continuous realignment effect
        EliphasEffect effect = new EliphasEffect(wire, progress, wirePoint, 30.0); // 30 second duration
        String effectId = "eliphas_" + java.lang.System.currentTimeMillis();
        activeEffects.put(effectId, effect);

        java.lang.System.out.println("Scroll of Eliphas activated at " + wirePoint + " - packet centers will be realigned");
        return true;
    }

    public void update(double deltaTime) {
        List<String> expiredEffects = new ArrayList<>();

        for (Map.Entry<String, AbilityEffect> entry : activeEffects.entrySet()) {
            AbilityEffect effect = entry.getValue();
            effect.update(deltaTime);

            if (effect.isExpired()) {
                expiredEffects.add(entry.getKey());
                java.lang.System.out.println("Ability effect " + entry.getKey() + " expired");
            }
        }

        // Remove expired effects
        for (String effectId : expiredEffects) {
            activeEffects.remove(effectId);
        }
    }

    public void applyEffects(List<Packet> packets) {
        for (AbilityEffect effect : activeEffects.values()) {
            effect.applyToPackets(packets);
        }
    }

    public boolean canMoveSystem(model.System system) {
        for (AbilityEffect effect : activeEffects.values()) {
            if (effect instanceof SisyphusEffect) {
                SisyphusEffect sisyphus = (SisyphusEffect) effect;
                if (sisyphus.getTargetSystem() == system) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean moveSystem(model.System system, Point2D newPosition) {
        if (!canMoveSystem(system)) {
            return false;
        }

        // Check constraints
        Point2D originalPosition = system.getPosition();
        double distance = originalPosition.distanceTo(newPosition);

        // Check radius constraint (100 pixels max)
        if (distance > 100.0) {
            java.lang.System.out.println("Cannot move system: exceeds maximum radius of 100 pixels");
            return false;
        }

        // Check wire length and collision constraints using WiringController
        WiringController wiringController = new WiringController();
        if (!wiringController.canMoveSystem(system, newPosition, gameController.getGameState())) {
            java.lang.System.out.println("Cannot move system: would violate wire constraints");
            return false;
        }

        // Move the system
        system.setPosition(newPosition);

        // Update all connected ports
        for (Port port : system.getInputPorts()) {
            port.updatePositionRelativeToSystem();
        }
        for (Port port : system.getOutputPorts()) {
            port.updatePositionRelativeToSystem();
        }

        java.lang.System.out.println("System moved to " + newPosition);
        return true;
    }

    private WireConnection findWireAtPoint(Point2D clickPoint) {
        if (gameController.getGameState() == null || gameController.getGameState().getWireConnections() == null) {
            return null;
        }

        WireConnection closestWire = null;
        double closestDistance = Double.MAX_VALUE;

        for (WireConnection wire : gameController.getGameState().getWireConnections()) {
            if (!wire.isActive()) continue;

            double distance = wire.getDistanceToPoint(clickPoint);
            if (distance < closestDistance && distance < 20.0) { // 20 pixel tolerance
                closestDistance = distance;
                closestWire = wire;
            }
        }

        return closestWire;
    }

    private model.System findSystemAtPoint(Point2D clickPoint) {
        if (gameController.getGameState() == null || gameController.getGameState().getCurrentLevel() == null) {
            return null;
        }

        for (model.System system : gameController.getGameState().getCurrentLevel().getSystems()) {
            Point2D systemPos = system.getPosition();
            double distance = systemPos.distanceTo(clickPoint);

            if (distance < 30.0) { // 30 pixel tolerance for system selection
                return system;
            }
        }

        return null;
    }

    public List<AbilityEffect> getActiveEffects() {
        return new ArrayList<>(activeEffects.values());
    }

    public void clearAllEffects() {
        activeEffects.clear();
    }
}


