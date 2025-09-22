package app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Screen;
import view.MainMenuView;
import controller.GameController;
import model.GameState;
import model.GameLevel;

import java.awt.*;

public class MainApp {
    
    public static class AppLauncher extends Application {
        private static MainApp mainApp;
        
        public AppLauncher() {
            // Default constructor required by JavaFX
        }
        
        @Override
        public void start(Stage primaryStage) {
            mainApp = new MainApp();
            mainApp.start(primaryStage);
        }
    }
    
    public static void launch(Class<? extends MainApp> appClass, String[] args) {
        Application.launch(AppLauncher.class, args);
    }

    private Stage primaryStage;
    private GameController gameController;
    private GameState gameState;
    private MainMenuView mainMenuView;

    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        try {
            initializeApplication();
            showMainMenu();

            // Don't minimize other applications here - do it when game starts

        } catch (Exception e) {
            showErrorDialog("Application Error", "Failed to start application", e.getMessage());
            Platform.exit();
        }
    }

    private void initializeApplication() {
        // Initialize game state and controller
        gameState = new GameState();
        gameController = new GameController(gameState);
        gameController.setMainApp(this);

        // Initialize main menu view
        mainMenuView = new MainMenuView(this);

        // Set up navigation callbacks AFTER views are initialized
        gameController.setupNavigationCallbacks(
                this::returnToMainMenu,
                this::restartCurrentLevel
        );

        // Configure primary stage
        configurePrimaryStage();
    }

    private void configurePrimaryStage() {
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setResizable(false);
        primaryStage.setTitle("Network Simulation Game");

        // Set to fullscreen
        Screen screen = Screen.getPrimary();
        primaryStage.setX(screen.getVisualBounds().getMinX());
        primaryStage.setY(screen.getVisualBounds().getMinY());
        primaryStage.setWidth(screen.getVisualBounds().getWidth());
        primaryStage.setHeight(screen.getVisualBounds().getHeight());

        // Add close handler
        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            showExitConfirmation();
        });
    }

    public void showMainMenu() {
        // Use setRoot instead of creating a new Scene to avoid the "already set as root" error
        if (primaryStage.getScene() == null) {
            Scene scene = new Scene(mainMenuView.getRoot());
            primaryStage.setScene(scene);
        } else {
            primaryStage.getScene().setRoot(mainMenuView.getRoot());
        }
        primaryStage.show();
    }

    public void startGame(String levelId) {
        try {
            java.lang.System.out.println("MainApp.startGame called with levelId: " + levelId);

            // Force new game start to ensure JSON loading path is used
            // Previously checked for save file and offered to continue; bypass for now
            startNewGame(levelId);

        } catch (Exception e) {
            showErrorDialog("Game Error", "Failed to start game", e.getMessage());
        }
    }


    public void startFreshGame(String levelId) {
        try {
            java.lang.System.out.println("MainApp.startFreshGame called with levelId: " + levelId);

            // Game always starts fresh (no connections preserved)

            // Start a completely fresh game with no preserved connections
            startNewGame(levelId);

        } catch (Exception e) {
            showErrorDialog("Game Error", "Failed to start fresh game", e.getMessage());
        }
    }



    private void showContinuePreviousGameDialog(String newLevelId) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Continue Previous Game");
        alert.setHeaderText("A saved game from a previous session was found.");
        alert.setContentText("Would you like to continue the previously saved game? If you decline, the save file will be deleted and a new game will begin.");

        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                // User wants to continue previous game
                continueFromSaveFile();
            } else {
                // User declines, delete save file and start new game
                gameController.deleteSaveFile();
                startNewGame(newLevelId);
            }
        });
    }

    private void continueFromSaveFile() {
        try {
            if (gameController.loadSavedGame()) {
                // Show static game state for a few seconds before resuming
                showStaticGameStateAndResume();
            } else {
                showErrorDialog("Load Error", "Failed to load saved game", "The save file may be corrupted. Starting a new game instead.");
                gameController.deleteSaveFile();
                startNewGame("level1"); // Default to level 1 if load fails
            }
        } catch (Exception e) {
            showErrorDialog("Load Error", "Failed to load saved game", e.getMessage());
            gameController.deleteSaveFile();
            startNewGame("level1");
        }
    }

    private void startNewGame(String levelId) {
        try {
            // Load the level first
            gameController.loadLevel(levelId);

            // Now start the game (this will ensure the level is set in the game view)
            gameController.startGame();

            // Switch to game view
            java.lang.System.out.println("Switching to game view...");
            if (primaryStage.getScene() == null) {
                Scene gameScene = new Scene(gameController.getGameView().getRoot());
                primaryStage.setScene(gameScene);
                java.lang.System.out.println("Created new scene with game view");
            } else {
                primaryStage.getScene().setRoot(gameController.getGameView().getRoot());
                java.lang.System.out.println("Set game view as scene root");
            }

            // Ensure the game window is fully visible and focused first
            Platform.runLater(() -> {
                primaryStage.show();
                primaryStage.requestFocus();
                primaryStage.toFront();

                // Only minimize other applications if the feature is enabled
                // For now, let's disable this feature to prevent issues
                // TODO: Implement a more robust minimize feature that doesn't affect the game window
                /*
                // Add a small delay to ensure the window is fully visible and focused
                new Thread(() -> {
                    try {
                        Thread.sleep(500); // Wait 500ms for window to be fully visible
                        Platform.runLater(this::minimizeOtherApplications);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
                */
            });

        } catch (Exception e) {
            showErrorDialog("Game Error", "Failed to start new game", e.getMessage());
        }
    }

    private void showStaticGameStateAndResume() {
        try {
            // Switch to game view first
            if (primaryStage.getScene() == null) {
                Scene gameScene = new Scene(gameController.getGameView().getRoot());
                primaryStage.setScene(gameScene);
            } else {
                primaryStage.getScene().setRoot(gameController.getGameView().getRoot());
            }

            // Ensure window is visible
            primaryStage.show();
            primaryStage.requestFocus();
            primaryStage.toFront();

            // Show static state for 3 seconds before resuming
            Platform.runLater(() -> {
                // The game view will show the static state automatically when loaded
                // After 3 seconds, start the game
                new Thread(() -> {
                    try {
                        Thread.sleep(3000); // Show static state for 3 seconds
                        Platform.runLater(() -> {
                            gameController.startGame(); // Resume the game
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            });

        } catch (Exception e) {
            showErrorDialog("Resume Error", "Failed to resume game", e.getMessage());
        }
    }

    public void showLevelSelect() {
        try {
            if (primaryStage.getScene() == null) {
                Scene levelScene = new Scene(gameController.getLevelSelectView().getRoot());
                primaryStage.setScene(levelScene);
            } else {
                primaryStage.getScene().setRoot(gameController.getLevelSelectView().getRoot());
            }
        } catch (Exception e) {
            showErrorDialog("Level Select Error", "Failed to load level selection", e.getMessage());
        }
    }

    public void showSettings() {
        try {
            if (primaryStage.getScene() == null) {
                Scene settingsScene = new Scene(gameController.getSettingsView().getRoot());
                primaryStage.setScene(settingsScene);
            } else {
                primaryStage.getScene().setRoot(gameController.getSettingsView().getRoot());
            }
        } catch (Exception e) {
            showErrorDialog("Settings Error", "Failed to load settings", e.getMessage());
        }
    }

    public void returnToMainMenu() {
        // Disable auto-save when manually returning to main menu
        if (gameController != null) {
            gameController.setAutoSaveEnabled(false);
            // Delete the save file since this is a manual exit
            gameController.deleteSaveFile();
        }
        showMainMenu();
    }

    public void restartCurrentLevel() {
        if (gameController != null && gameController.getGameState() != null) {
            GameLevel currentLevel = gameController.getGameState().getCurrentLevel();
            if (currentLevel != null) {
                // Preserve previous-level wires per spec; clear only current-level wiring
                gameController.restartLevelPreservingPrevious();
                
                // Start the game after restart
                gameController.startGame();
                
                // Switch back to game view
                try {
                    if (primaryStage.getScene() == null) {
                        Scene gameScene = new Scene(gameController.getGameView().getRoot());
                        primaryStage.setScene(gameScene);
                    } else {
                        primaryStage.getScene().setRoot(gameController.getGameView().getRoot());
                    }
                    primaryStage.show();
                    primaryStage.requestFocus();
                    primaryStage.toFront();
                } catch (Exception e) {
                    System.err.println("Failed to switch to game view after restart: " + e.getMessage());
                }
            }
        }
    }

    public void restartCurrentLevelNow() {
        if (gameController != null && gameController.getGameState() != null) {
            GameLevel currentLevel = gameController.getGameState().getCurrentLevel();
            if (currentLevel != null) {
                // Ensure we don't prompt to continue; wipe any save and restart while
                // preserving prior-level wiring per spec
                gameController.deleteSaveFile();
                gameController.restartLevelPreservingPrevious();
                
                // Start the game after restart
                gameController.startGame();
                
                // Switch back to game view (for pause menu restart, we're already in game view, but ensure it's focused)
                try {
                    if (primaryStage.getScene() == null) {
                        Scene gameScene = new Scene(gameController.getGameView().getRoot());
                        primaryStage.setScene(gameScene);
                    } else {
                        primaryStage.getScene().setRoot(gameController.getGameView().getRoot());
                    }
                    primaryStage.show();
                    primaryStage.requestFocus();
                    primaryStage.toFront();
                } catch (Exception e) {
                    System.err.println("Failed to switch to game view after pause menu restart: " + e.getMessage());
                }
            }
        }
    }

    private void minimizeOtherApplications() {
        try {
            // First, ensure our game window is focused and visible
            Platform.runLater(() -> {
                primaryStage.requestFocus();
                primaryStage.toFront();
            });

            // Wait a bit for the focus to take effect
            Thread.sleep(200);

            String osName = System.getProperty("os.name").toLowerCase();

            if (osName.contains("windows")) {
                minimizeWindowsApplications();
            } else if (osName.contains("mac")) {
                minimizeMacApplications();
            } else if (osName.contains("linux")) {
                minimizeLinuxApplications();
            }
        } catch (Exception e) {
            // Silently handle any errors in minimizing other applications
            System.err.println("Could not minimize other applications: " + e.getMessage());
        }
    }

    private void minimizeWindowsApplications() {
        try {
            // Use Robot to press Windows + D to show desktop (more reliable than Windows + M)
            Robot robot = new Robot();
            robot.setAutoDelay(50); // Set auto delay for more reliable key presses

            robot.keyPress(java.awt.event.KeyEvent.VK_WINDOWS);
            robot.keyPress(java.awt.event.KeyEvent.VK_D);
            robot.delay(100);
            robot.keyRelease(java.awt.event.KeyEvent.VK_D);
            robot.keyRelease(java.awt.event.KeyEvent.VK_WINDOWS);

            // Wait a bit for the minimize action to complete
            robot.delay(200);

        } catch (AWTException e) {
            System.err.println("Failed to minimize Windows applications: " + e.getMessage());
        }
    }

    private void minimizeMacApplications() {
        try {
            // Use Robot to press Command + H to hide applications
            Robot robot = new Robot();
            robot.setAutoDelay(50); // Set auto delay for more reliable key presses

            robot.keyPress(java.awt.event.KeyEvent.VK_META);
            robot.keyPress(java.awt.event.KeyEvent.VK_H);
            robot.delay(100);
            robot.keyRelease(java.awt.event.KeyEvent.VK_H);
            robot.keyRelease(java.awt.event.KeyEvent.VK_META);

            // Wait a bit for the minimize action to complete
            robot.delay(200);

        } catch (AWTException e) {
            System.err.println("Failed to minimize Mac applications: " + e.getMessage());
        }
    }

    private void minimizeLinuxApplications() {
        try {
            // Use Robot to press Super + D to show desktop (most Linux DEs)
            Robot robot = new Robot();
            robot.setAutoDelay(50); // Set auto delay for more reliable key presses

            robot.keyPress(java.awt.event.KeyEvent.VK_META);
            robot.keyPress(java.awt.event.KeyEvent.VK_D);
            robot.delay(100);
            robot.keyRelease(java.awt.event.KeyEvent.VK_D);
            robot.keyRelease(java.awt.event.KeyEvent.VK_META);

            // Wait a bit for the minimize action to complete
            robot.delay(200);

        } catch (AWTException e) {
            System.err.println("Failed to minimize Linux applications: " + e.getMessage());
        }
    }

    private void showExitConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Game");
        alert.setHeaderText("Are you sure you want to exit?");
        alert.setContentText("Any unsaved progress will be lost.");

        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                // Disable auto-save and delete save file since this is a manual exit
                if (gameController != null) {
                    gameController.setAutoSaveEnabled(false);
                    gameController.deleteSaveFile();
                }
                Platform.exit();
            }
        });
    }

    private void showErrorDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public GameController getGameController() {
        return gameController;
    }

    public GameState getGameState() {
        return gameState;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }
} 