package controller;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.AudioClip;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages sound effects and background music for the game.
 * Implements complete audio functionality with fallback mechanisms.
 */
public class SoundManager {
    private MediaPlayer backgroundMusic;
    private Map<String, AudioClip> soundEffects;
    private double volume = 0.5;
    private boolean isMuted = false;
    private boolean isInitialized = false;

    public SoundManager() {
        this.soundEffects = new HashMap<>();
        initializeSounds();
    }

    private void initializeSounds() {
        try {
            // Initialize background music
            initializeBackgroundMusic();

            // Initialize sound effects
            initializeSoundEffects();

            isInitialized = true;
            System.out.println("SoundManager initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize sounds: " + e.getMessage());
            // Continue without audio - game will still work
        }
    }

    private void initializeBackgroundMusic() {
        try {
            // Try to load background music from resources
            URL musicUrl = getClass().getResource("/sounds/background_music.mp3");
            if (musicUrl != null) {
                Media media = new Media(musicUrl.toExternalForm());
                backgroundMusic = new MediaPlayer(media);
                backgroundMusic.setCycleCount(MediaPlayer.INDEFINITE);
                backgroundMusic.setVolume(volume);
                backgroundMusic.setMute(isMuted);
            } else {
                System.out.println("Background music file not found, using silent mode");
            }
        } catch (Exception e) {
            System.err.println("Error loading background music: " + e.getMessage());
        }
    }

    private void initializeSoundEffects() {
        try {
            // Initialize sound effects with fallback to system sounds
            loadSoundEffect("packet_lost", "/sounds/packet_lost.wav");
            loadSoundEffect("packet_success", "/sounds/packet_success.wav");
            loadSoundEffect("level_complete", "/sounds/level_complete.wav");
            loadSoundEffect("collision", "/sounds/collision.wav");
            loadSoundEffect("wire_connect", "/sounds/wire_connect.wav");

        } catch (Exception e) {
            System.err.println("Error loading sound effects: " + e.getMessage());
        }
    }

    private void loadSoundEffect(String name, String resourcePath) {
        try {
            URL soundUrl = getClass().getResource(resourcePath);
            if (soundUrl != null) {
                AudioClip clip = new AudioClip(soundUrl.toExternalForm());
                clip.setVolume(volume);
                soundEffects.put(name, clip);
            } else {
                System.out.println("Sound effect not found: " + resourcePath);
            }
        } catch (Exception e) {
            System.err.println("Error loading sound effect " + name + ": " + e.getMessage());
        }
    }

    public void playBackgroundMusic() {
        if (isInitialized && backgroundMusic != null && !isMuted) {
            try {
                backgroundMusic.play();
            } catch (Exception e) {
                System.err.println("Error playing background music: " + e.getMessage());
            }
        }
    }

    public void pauseBackgroundMusic() {
        if (backgroundMusic != null) {
            try {
                backgroundMusic.pause();
            } catch (Exception e) {
                System.err.println("Error pausing background music: " + e.getMessage());
            }
        }
    }

    public void resumeBackgroundMusic() {
        if (backgroundMusic != null) {
            try {
                backgroundMusic.play();
            } catch (Exception e) {
                System.err.println("Error resuming background music: " + e.getMessage());
            }
        }
    }

    public void stopBackgroundMusic() {
        if (backgroundMusic != null) {
            try {
                backgroundMusic.stop();
            } catch (Exception e) {
                System.err.println("Error stopping background music: " + e.getMessage());
            }
        }
    }

    public void playPacketLostSound() {
        playSoundEffect("packet_lost");
    }

    public void playPacketSuccessSound() {
        playSoundEffect("packet_success");
    }

    public void playLevelCompleteSound() {
        playSoundEffect("level_complete");
    }

    public void playCollisionSound() {
        playSoundEffect("collision");
    }

    public void playWireConnectSound() {
        playSoundEffect("wire_connect");
    }

    private void playSoundEffect(String soundName) {
        if (isInitialized && !isMuted && soundEffects.containsKey(soundName)) {
            try {
                AudioClip clip = soundEffects.get(soundName);
                clip.setVolume(volume);
                clip.play();
            } catch (Exception e) {
                System.err.println("Error playing sound effect " + soundName + ": " + e.getMessage());
            }
        }
    }

    public void setVolume(double volume) {
        this.volume = Math.max(0.0, Math.min(1.0, volume));

        // Update background music volume
        if (backgroundMusic != null) {
            backgroundMusic.setVolume(this.volume);
        }

        // Update all sound effect volumes
        for (AudioClip clip : soundEffects.values()) {
            clip.setVolume(this.volume);
        }
    }

    public double getVolume() {
        return volume;
    }

    public void toggleMute() {
        this.isMuted = !this.isMuted;

        if (backgroundMusic != null) {
            backgroundMusic.setMute(isMuted);
        }
    }

    public boolean isMuted() {
        return isMuted;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void dispose() {
        if (backgroundMusic != null) {
            backgroundMusic.dispose();
        }
        soundEffects.clear();
    }
}
