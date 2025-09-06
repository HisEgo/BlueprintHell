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

public class GameOverView {
    private GameController gameController;
    private StackPane root;
    private Runnable onRestartLevel;
    private Runnable onBackToMainMenu;
    private Text statsText;
    private Text reasonText;

    public GameOverView(GameController gameController) {
        this.gameController = gameController;
        initializeUI();
    }

    public void setOnRestartLevel(Runnable callback) {
        this.onRestartLevel = callback;
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

        Text title = new Text("Game Over");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        title.setFill(Color.RED);

        // Game statistics
        statsText = new Text();
        statsText.setFont(Font.font("Arial", 16));
        statsText.setFill(Color.WHITE);
        updateStats();

        // Reason display
        reasonText = new Text();
        reasonText.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        reasonText.setFill(Color.ORANGE);
        updateReason();

        Button restartButton = createMenuButton("Restart Level");
        restartButton.setOnAction(e -> {
            if (onRestartLevel != null) {
                onRestartLevel.run();
            }
        });

        Button mainMenuButton = createMenuButton("Main Menu");
        mainMenuButton.setOnAction(e -> {
            if (onBackToMainMenu != null) {
                onBackToMainMenu.run();
            }
        });

        container.getChildren().addAll(title, reasonText, statsText, restartButton, mainMenuButton);
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
                    "Final Statistics:\n" +
                            "Coins Earned: %d\n" +
                            "Packet Loss: %.1f%%\n" +
                            "Wire Length Used: %.1f\n" +
                            "Time Elapsed: %.1f seconds",
                    gameState.getCoins(),
                    gameState.getPacketLoss(),
                    wireLengthUsed,
                    gameState.getTemporalProgress()
            );
            statsText.setText(stats);
        }
    }

    public void refreshStats() {
        updateStats();
        updateReason();
    }

    private void updateReason() {
        if (gameController != null && gameController.getGameState() != null) {
            GameState state = gameController.getGameState();
            String reason = switch (state.getLastGameOverReason()) {
                case EXCESSIVE_PACKET_LOSS -> "Reason: Packet loss exceeded 50%";
                case NETWORK_DISCONNECTED -> "Reason: Network disconnected (no route to destination)";
                case EXCESSIVE_SYSTEM_FAILURES -> "Reason: Too many systems failed";
                default -> "";
            };
            reasonText.setText(reason);
        }
    }

    private Button createMenuButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(200);
        button.setPrefHeight(50);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        button.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: cyan;" +
                        "-fx-border-width: 2;" +
                        "-fx-text-fill: cyan;" +
                        "-fx-cursor: hand;" +
                        "-fx-background-radius: 5;" +
                        "-fx-border-radius: 5;"
        );

        // Hover effects
        button.setOnMouseEntered(e -> {
            button.setStyle(button.getStyle() + "-fx-background-color: rgba(0, 255, 255, 0.1);");
        });
        button.setOnMouseExited(e -> {
            button.setStyle(button.getStyle().replace("-fx-background-color: rgba(0, 255, 255, 0.1);", ""));
        });

        return button;
    }

    public StackPane getRoot() {
        return root;
    }
}
