package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import controller.GameController;
import model.GameState;

public class LevelCompleteView {
    private GameController gameController;
    private StackPane root;
    private Runnable onNextLevel;
    private Runnable onBackToMainMenu;
    private Text statsText;

    public LevelCompleteView(GameController gameController) {
        this.gameController = gameController;
        initializeUI();
    }

    public void setOnNextLevel(Runnable callback) {
        this.onNextLevel = callback;
    }

    public void setOnBackToMainMenu(Runnable callback) {
        this.onBackToMainMenu = callback;
    }

    private void initializeUI() {
        root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e);");

        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(50));

        Text title = new Text("Level Complete!");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        title.setFill(Color.GREEN);

        // Level completion statistics
        statsText = new Text();
        statsText.setFont(Font.font("Arial", 16));
        statsText.setFill(Color.WHITE);
        updateStats();

        Button nextLevelButton = createMenuButton("Next Level");
        nextLevelButton.setOnAction(e -> {
            if (onNextLevel != null) {
                onNextLevel.run();
            }
        });

        Button mainMenuButton = createMenuButton("Main Menu");
        mainMenuButton.setOnAction(e -> {
            if (onBackToMainMenu != null) {
                onBackToMainMenu.run();
            }
        });

        container.getChildren().addAll(title, statsText, nextLevelButton, mainMenuButton);
        root.getChildren().add(container);
    }

    private void updateStats() {
        if (gameController != null && gameController.getGameState() != null) {
            GameState gameState = gameController.getGameState();
            double wireLengthUsed = 0.0;
            if (gameState.getCurrentLevel() != null) {
                wireLengthUsed = gameState.getCurrentLevel().getInitialWireLength() - gameState.getRemainingWireLength();
            }

            String stats = String.format(
                    "Level Statistics:\n" +
                            "Coins Earned: %d\n" +
                            "Packet Loss: %.1f%%\n" +
                            "Wire Length Used: %.1f\n" +
                            "Time Elapsed: %.1f seconds\n" +
                            "Performance: %s",
                    gameState.getCoins(),
                    gameState.getPacketLoss(),
                    wireLengthUsed,
                    gameState.getTemporalProgress(),
                    getPerformanceRating(gameState.getPacketLoss())
            );
            statsText.setText(stats);
        }
    }

    private String getPerformanceRating(double packetLoss) {
        if (packetLoss <= 10.0) return "Excellent! ⭐⭐⭐";
        if (packetLoss <= 25.0) return "Good! ⭐⭐";
        if (packetLoss <= 50.0) return "Passable ⭐";
        return "Needs Improvement";
    }

    public void refreshStats() {
        updateStats();
    }

    private Button createMenuButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(200);
        button.setPrefHeight(50);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        button.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #00ff00;" +
                        "-fx-border-width: 2;" +
                        "-fx-text-fill: #00ff00;" +
                        "-fx-cursor: hand;" +
                        "-fx-background-radius: 5;" +
                        "-fx-border-radius: 5;"
        );

        // Hover effects
        button.setOnMouseEntered(e -> {
            button.setStyle(button.getStyle() + "-fx-background-color: rgba(0, 255, 0, 0.1);");
        });
        button.setOnMouseExited(e -> {
            button.setStyle(button.getStyle().replace("-fx-background-color: rgba(0, 255, 0, 0.1);", ""));
        });

        return button;
    }

    public StackPane getRoot() {
        return root;
    }
}
