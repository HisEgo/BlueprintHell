package controller;

import model.*;

import java.lang.System;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TemporalManager {
    private final Map<Double, TemporalState> temporalStates;
    private final double stateSaveInterval;
    private double lastSavedTime;
    private final int maxStatesToKeep;
    private final List<Double> stateTimes;

    public TemporalManager() {
        this.temporalStates = new ConcurrentHashMap<>();
        this.stateSaveInterval = 0.5; // Save state every 0.5 seconds
        this.lastSavedTime = -1.0;
        this.maxStatesToKeep = 200; // Keep up to 200 states (100 seconds at 0.5s intervals)
        this.stateTimes = new ArrayList<>();
    }

    public void saveStateIfNeeded(double currentTime, GameState gameState, GameLevel level) {
        if (currentTime - lastSavedTime >= stateSaveInterval) {
            saveState(currentTime, gameState, level);
            lastSavedTime = currentTime;
        }
    }

    public void saveState(double time, GameState gameState, GameLevel level) {
        // Round time to nearest interval for consistent storage
        double roundedTime = Math.round(time / stateSaveInterval) * stateSaveInterval;
        
        TemporalState state = new TemporalState(roundedTime, gameState, level);
        temporalStates.put(roundedTime, state);
        stateTimes.add(roundedTime);
        stateTimes.sort(Double::compareTo);

        // Clean up old states if we have too many
        cleanupOldStates();

        System.out.println("Saved temporal state at time " + String.format("%.2f", roundedTime) + "s");
    }

    public boolean restoreToTime(double targetTime, GameState gameState, GameLevel level) {
        // Find the closest state at or before the target time
        Double closestTime = findClosestStateTime(targetTime);
        
        if (closestTime == null) {
            System.out.println("No temporal state found for time " + String.format("%.2f", targetTime) + "s");
            return false;
        }

        TemporalState state = temporalStates.get(closestTime);
        if (state == null) {
            System.out.println("Temporal state not found for time " + String.format("%.2f", closestTime) + "s");
            return false;
        }

        System.out.println("Restoring to temporal state at time " + String.format("%.2f", closestTime) + "s");
        state.restoreTo(gameState, level);
        
        // If we're going to a time before the closest state, we need to simulate forward
        if (targetTime > closestTime) {
            simulateForwardFromState(closestTime, targetTime, gameState, level);
        }
        
        return true;
    }

    private Double findClosestStateTime(double targetTime) {
        Double closestTime = null;
        
        for (Double time : stateTimes) {
            if (time <= targetTime) {
                if (closestTime == null || time > closestTime) {
                    closestTime = time;
                }
            }
        }
        
        return closestTime;
    }

    private void simulateForwardFromState(double fromTime, double toTime, GameState gameState, GameLevel level) {
        System.out.println("Simulating forward from " + String.format("%.2f", fromTime) + "s to " + String.format("%.2f", toTime) + "s");
        
        double timeStep = 0.1; // 100ms simulation steps
        double currentTime = fromTime;
        double originalTemporalProgress = gameState.getTemporalProgress();
        
        while (currentTime < toTime) {
            double stepSize = Math.min(timeStep, toTime - currentTime);
            
            // Update temporal progress temporarily
            gameState.setTemporalProgress(currentTime + stepSize);
            
            // Process packet injections for this time
            processPacketInjectionsForTime(currentTime + stepSize, gameState, level);
            
            // Update packet movement
            updatePacketMovement(stepSize, gameState);
            
            // Process game mechanics
            updateWirePacketMovement(stepSize, gameState, level);
            processWireConnections(gameState, level);
            processSystemTransfers(gameState, level);
            
            currentTime += stepSize;
        }
        
        // Restore original temporal progress
        gameState.setTemporalProgress(originalTemporalProgress);
    }

    private void processPacketInjectionsForTime(double targetTime, GameState gameState, GameLevel level) {
        if (level == null) return;

        for (PacketInjection injection : level.getPacketSchedule()) {
            if (!injection.isExecuted() && injection.getTime() <= targetTime) {
                // Create and inject packet using the injection's createPacket method
                Packet packet = injection.createPacket();
                packet.setBaseSpeed(50.0); // Default speed
                
                gameState.getActivePackets().add(packet);
                injection.setExecuted(true);
                
                System.out.println("Injected packet at time " + String.format("%.2f", targetTime) + "s");
            }
        }
    }

    private void updatePacketMovement(double deltaTime, GameState gameState) {
        // This would integrate with MovementController
        // For now, we'll do basic position updates
        for (Packet packet : gameState.getActivePackets()) {
            if (packet.isActive() && packet.getCurrentWire() != null) {
                // Update packet position along wire
                double speed = packet.getBaseSpeed();
                double distance = speed * deltaTime;
                
                // Update path progress
                double currentProgress = packet.getPathProgress();
                double wireLength = packet.getCurrentWire().getWireLength();
                double newProgress = Math.min(1.0, currentProgress + distance / wireLength);
                packet.setPathProgress(newProgress);
                
                // Update position
                packet.updatePositionOnWire();
            }
        }
    }

    private void updateWirePacketMovement(double deltaTime, GameState gameState, GameLevel level) {
        // This would integrate with the existing wire movement logic
        // For now, we'll do basic updates
        for (WireConnection connection : level.getWireConnections()) {
            List<Packet> packetsToRemove = new ArrayList<>();
            
            for (Packet packet : connection.getPacketsOnWire()) {
                if (packet.getPathProgress() >= 1.0) {
                    // Packet reached end of wire
                    packetsToRemove.add(packet);
                    // Move to next wire or destination
                    // This would integrate with existing logic
                }
            }
            
            for (Packet packet : packetsToRemove) {
                connection.releasePacket(packet);
            }
        }
    }

    private void processWireConnections(GameState gameState, GameLevel level) {
        // This would integrate with existing wire connection logic
        // For now, we'll do basic processing
        for (WireConnection connection : level.getWireConnections()) {
            for (Packet packet : connection.getPacketsOnWire()) {
                if (packet.getPathProgress() >= 1.0) {
                    // Packet reached end of wire, process transfer
                    // This would integrate with existing transfer logic
                }
            }
        }
    }

    private void processSystemTransfers(GameState gameState, GameLevel level) {
        // This would integrate with existing system transfer logic
        // For now, we'll do basic processing
        for (model.System system : level.getSystems()) {
            // Process packets in system storage
            // This would integrate with existing system logic
        }
    }

    private void cleanupOldStates() {
        if (stateTimes.size() > maxStatesToKeep) {
            // Remove oldest states
            int statesToRemove = stateTimes.size() - maxStatesToKeep;
            for (int i = 0; i < statesToRemove; i++) {
                Double timeToRemove = stateTimes.get(0);
                temporalStates.remove(timeToRemove);
                stateTimes.remove(0);
            }
        }
    }

    public void clearAllStates() {
        temporalStates.clear();
        stateTimes.clear();
        lastSavedTime = -1.0;
        System.out.println("Cleared all temporal states");
    }

    public int getStateCount() {
        return temporalStates.size();
    }

    public String getTimeRange() {
        if (stateTimes.isEmpty()) {
            return "No states stored";
        }
        
        double minTime = stateTimes.get(0);
        double maxTime = stateTimes.get(stateTimes.size() - 1);
        return String.format("%.2fs - %.2fs", minTime, maxTime);
    }
}

