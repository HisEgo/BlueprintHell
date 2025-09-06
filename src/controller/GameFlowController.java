package controller;

import model.*;

/**
 * Manages game flow, transitions, and win/lose conditions.
 */
public class GameFlowController {
    private GameController gameController;

    public GameFlowController(GameController gameController) {
        this.gameController = gameController;
    }

    /**
     * Checks game flow conditions and handles transitions.
     */
    public void checkGameFlow() {
        GameState gameState = gameController.getGameState();

        // Check for game over condition
        if (gameState.shouldEndGame()) {
            handleGameOver();
            return;
        }

        // Check for level completion
        if (gameState.shouldCompleteLevel()) {
            handleLevelComplete();
            return;
        }
    }

    /**
     * Handles game over condition.
     */
    private void handleGameOver() {
        GameState gameState = gameController.getGameState();
        gameState.setGameOver(true);
        gameController.stopGame();

        // Show game over screen
        if (gameController.getGameOverView() != null) {
            gameController.getGameOverView().refreshStats();
            gameController.getMainApp().getPrimaryStage().getScene().setRoot(
                    gameController.getGameOverView().getRoot()
            );
        }
    }

    /**
     * Handles level completion.
     */
    private void handleLevelComplete() {
        GameState gameState = gameController.getGameState();
        gameState.setLevelComplete(true);
        gameController.stopGame();

        // Play level complete sound
        if (gameController.getSoundManager() != null) {
            gameController.getSoundManager().playLevelCompleteSound();
        }

        // Show level complete screen
        if (gameController.getLevelCompleteView() != null) {
            gameController.getLevelCompleteView().refreshStats();
            gameController.getMainApp().getPrimaryStage().getScene().setRoot(
                    gameController.getLevelCompleteView().getRoot()
            );
        }
    }

    /**
     * Transitions to the next level with a fresh start.
     */
    public void nextLevel() {
        GameState gameState = gameController.getGameState();
        GameLevel currentLevel = gameState.getCurrentLevel();

        if (currentLevel != null) {
            String currentLevelId = currentLevel.getLevelId();
            String nextLevelId = getNextLevelId(currentLevelId);

            if (nextLevelId != null) {
                // Always start with no connections (fresh mode)
                gameController.loadLevel(nextLevelId);
                java.lang.System.out.println("DEBUG: Loading next level with fresh start");

                gameController.startGame();

                // Switch back to game view
                if (gameController.getMainApp() != null) {
                    gameController.getMainApp().getPrimaryStage().getScene().setRoot(
                            gameController.getGameView().getRoot()
                    );
                }
            } else {
                // No more levels, return to main menu
                if (gameController.getMainApp() != null) {
                    gameController.getMainApp().returnToMainMenu();
                }
            }
        }
    }

    /**
     * Gets the next level ID based on the current level.
     */
    private String getNextLevelId(String currentLevelId) {
        switch (currentLevelId) {
            case "level1": return "level2";
            case "level2": return "level3";
            case "level3": return "level4";
            case "level4": return "level5";
            case "level5": return null; // Game completed
            default: return "level1";
        }
    }

    /**
     * Restarts the current level.
     */
    public void restartLevel() {
        GameState gameState = gameController.getGameState();
        GameLevel currentLevel = gameState.getCurrentLevel();

        if (currentLevel != null) {
            // Use the restart method that respects the current game mode
            gameController.restartLevelPreservingPrevious();
            gameController.startGame();
        }
    }
}
