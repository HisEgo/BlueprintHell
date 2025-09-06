package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import model.*;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.lang.System;

/**
 * Manages real-time game saving and loading with validation and synchronization.
 * Implements secure save mechanisms to prevent cheating.
 */
public class GameSaveManager {
    private static final String SAVE_FILE_PATH = "game_save.json";
    private static final String SAVE_BACKUP_PATH = "game_save_backup.json";
    private static final double SAVE_INTERVAL = 5.0; // Save every 5 seconds
    private static final String SAVE_VERSION = "2.0"; // Phase 2 save version
    private static final String TEMP_FILE_PREFIX = "game_save_temp";

    private final ObjectMapper objectMapper;
    private final ReentrantReadWriteLock saveLock;
    private double lastSaveTime;
    private boolean isSaving;
    private boolean autoSaveEnabled;
    private static boolean shutdownHookRegistered = false;

    public GameSaveManager() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        // Remove default typing to avoid serializing UI/geometry internals and recursion
        // Configure mapper to only serialize our POJO model types via explicit getters
        // and ignore unknown/foreign types encountered inside maps/lists.
        // Configure for handling complex object graphs
        this.saveLock = new ReentrantReadWriteLock();
        this.lastSaveTime = 0.0;
        this.isSaving = false;
        this.autoSaveEnabled = true;

        // Register shutdown hook to clean up temporary files (only once)
        registerShutdownHook();
    }

    /**
     * Saves the current game state with synchronization.
     */
    public boolean saveGame(GameState gameState) {
        if (isSaving) {
            return false; // Prevent concurrent saves
        }

        saveLock.writeLock().lock();
        try {
            isSaving = true;

            // Create save data with validation hash
            GameSaveData saveData = createSaveData(gameState);
            saveData.setValidationHash(calculateValidationHash(saveData));

            // Write to temporary file first
            Path tempFile = Files.createTempFile(TEMP_FILE_PREFIX, ".json");
            try {
                objectMapper.writeValue(tempFile.toFile(), saveData);

                // Atomic move to actual save file
                Files.move(tempFile, Paths.get(SAVE_FILE_PATH), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception tempFileException) {
                // Clean up temporary file if something goes wrong
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException cleanupException) {
                    System.err.println("Failed to clean up temporary file: " + cleanupException.getMessage());
                }
                throw tempFileException; // Re-throw the original exception
            }

            // Create backup
            Files.copy(Paths.get(SAVE_FILE_PATH), Paths.get(SAVE_BACKUP_PATH), StandardCopyOption.REPLACE_EXISTING);

            lastSaveTime = System.currentTimeMillis() / 1000.0;
            return true;

        } catch (Exception e) {
            System.err.println("Error saving game: " + e.getMessage());
            return false;
        } finally {
            isSaving = false;
            saveLock.writeLock().unlock();
        }
    }

    /**
     * Loads a saved game with validation.
     */
    public GameState loadGame() throws GameLoadException {
        saveLock.readLock().lock();
        try {
            if (!Files.exists(Paths.get(SAVE_FILE_PATH))) {
                throw new GameLoadException("No save file found");
            }

            // Read save data
            GameSaveData saveData = objectMapper.readValue(
                    new File(SAVE_FILE_PATH),
                    GameSaveData.class
            );

            // Validate save data
            if (!validateSaveData(saveData)) {
                throw new GameLoadException("Save file validation failed - possible corruption or cheating");
            }

            // Reconstruct game state
            return reconstructGameState(saveData);

        } catch (IOException e) {
            throw new GameLoadException("Error reading save file: " + e.getMessage());
        } finally {
            saveLock.readLock().unlock();
        }
    }

    /**
     * Checks if a save file exists.
     */
    public boolean hasSaveFile() {
        return Files.exists(Paths.get(SAVE_FILE_PATH));
    }

    /**
     * Deletes the save file.
     */
    public boolean deleteSaveFile() {
        try {
            Files.deleteIfExists(Paths.get(SAVE_FILE_PATH));
            Files.deleteIfExists(Paths.get(SAVE_BACKUP_PATH));
            return true;
        } catch (IOException e) {
            System.err.println("Error deleting save file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates the save timer and saves if needed.
     */
    public void updateSaveTimer(GameState gameState, double currentTime) {
        if (autoSaveEnabled && currentTime - lastSaveTime >= SAVE_INTERVAL) {
            saveGame(gameState);
        }
    }

    /**
     * Enables or disables auto-save functionality.
     * Should be disabled when player manually exits the game.
     */
    public void setAutoSaveEnabled(boolean enabled) {
        this.autoSaveEnabled = enabled;
    }

    /**
     * Checks if auto-save is currently enabled.
     */
    public boolean isAutoSaveEnabled() {
        return autoSaveEnabled;
    }

    /**
     * Creates save data from game state.
     */
    private GameSaveData createSaveData(GameState gameState) {
        GameSaveData saveData = new GameSaveData();
        saveData.setVersion(SAVE_VERSION);
        saveData.setTimestamp(System.currentTimeMillis());
        saveData.setGameState(gameState);
        saveData.setChecksum(calculateChecksum(gameState));
        return saveData;
    }

    /**
     * Calculates a checksum for the game state.
     */
    private String calculateChecksum(GameState gameState) {
        // Enhanced checksum calculation including all game components
        int checksum = 0;
        checksum += gameState.getCoins();
        checksum += gameState.getRemainingWireLength();
        checksum += gameState.getPacketLoss();
        checksum += gameState.getTemporalProgress();

        // Include active packets
        for (Packet packet : gameState.getActivePackets()) {
            checksum += packet.getSize();
            checksum += (int)packet.getNoiseLevel();
        }

        // Include systems count and positions
        for (model.System system : gameState.getSystems()) {
            checksum += system.getId().hashCode();
            if (system.getPosition() != null) {
                checksum += (int)system.getPosition().getX();
                checksum += (int)system.getPosition().getY();
            }
            checksum += system.getInputPorts().size();
            checksum += system.getOutputPorts().size();
        }

        // Include wire connections
        for (WireConnection connection : gameState.getWireConnections()) {
            checksum += connection.getId().hashCode();
            checksum += (int)connection.getWireLength();
            checksum += connection.isActive() ? 1 : 0;
        }

        return Integer.toHexString(checksum);
    }

    /**
     * Calculates validation hash for save data.
     */
    private String calculateValidationHash(GameSaveData saveData) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String dataToHash = saveData.getVersion() +
                    saveData.getTimestamp() +
                    saveData.getChecksum();
            byte[] hash = digest.digest(dataToHash.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return "invalid";
        }
    }

    /**
     * Validates save data integrity.
     */
    private boolean validateSaveData(GameSaveData saveData) {
        // Check version compatibility
        if (!SAVE_VERSION.equals(saveData.getVersion())) {
            return false;
        }

        // Check timestamp (not too old)
        long currentTime = System.currentTimeMillis();
        if (currentTime - saveData.getTimestamp() > 24 * 60 * 60 * 1000) { // 24 hours
            return false;
        }

        // Verify checksum
        String expectedChecksum = calculateChecksum(saveData.getGameState());
        if (!expectedChecksum.equals(saveData.getChecksum())) {
            return false;
        }

        // Verify validation hash
        String expectedHash = calculateValidationHash(saveData);
        return expectedHash.equals(saveData.getValidationHash());
    }

    /**
     * Reconstructs game state from save data.
     */
    private GameState reconstructGameState(GameSaveData saveData) {
        // For now, return the saved game state directly
        // In a full implementation, you might need to reconstruct objects
        return saveData.getGameState();
    }

    /**
     * Exception thrown when game loading fails.
     */
    public static class GameLoadException extends Exception {
        public GameLoadException(String message) {
            super(message);
        }
    }

    /**
     * Registers a shutdown hook to clean up temporary files on application exit.
     */
    private static synchronized void registerShutdownHook() {
        if (!shutdownHookRegistered) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                cleanupTemporaryFiles();
            }));
            shutdownHookRegistered = true;
        }
    }

    /**
     * Cleans up any leftover temporary files in the system temp directory.
     */
    private static void cleanupTemporaryFiles() {
        try {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
            Files.list(tempDir)
                    .filter(path -> path.getFileName().toString().startsWith(TEMP_FILE_PREFIX))
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                            System.out.println("Cleaned up temporary save file: " + path.getFileName());
                        } catch (IOException e) {
                            System.err.println("Could not delete temporary file: " + path.getFileName());
                        }
                    });
        } catch (IOException e) {
            System.err.println("Error during temporary file cleanup: " + e.getMessage());
        }
    }

    /**
     * Manually triggers cleanup of temporary files (can be called during runtime).
     */
    public static void cleanupTemporaryFilesNow() {
        cleanupTemporaryFiles();
    }

    /**
     * Inner class for save data structure.
     */
    public static class GameSaveData {
        private String version;
        private long timestamp;
        private GameState gameState;
        private String checksum;
        private String validationHash;

        // Getters and setters
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public GameState getGameState() { return gameState; }
        public void setGameState(GameState gameState) { this.gameState = gameState; }

        public String getChecksum() { return checksum; }
        public void setChecksum(String checksum) { this.checksum = checksum; }

        public String getValidationHash() { return validationHash; }
        public void setValidationHash(String validationHash) { this.validationHash = validationHash; }
    }
}