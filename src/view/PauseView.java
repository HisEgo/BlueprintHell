package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import controller.GameController;

/**
 * Pause menu overlay that allows resuming, restarting the level, or returning to the main menu.
 */
public class PauseView {
    private final GameController gameController;
    private final StackPane root;
    private final VBox container;
    private boolean isVisible;

    public PauseView(GameController gameController) {
        this.gameController = gameController;
        this.isVisible = false;
        this.root = new StackPane();
        this.container = new VBox(10); // Smaller spacing
        initializeUI();
    }

    private void initializeUI() {
        // Transparent background so game is visible behind
        root.setStyle("-fx-background-color: transparent;");
        root.setVisible(false);
        root.setPickOnBounds(false); // Allow clicks to pass through to game

        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(20)); // Smaller padding for compact menu
        container.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #1a1a2e, #0f1b2d);" +
                        "-fx-border-color: cyan;" +
                        "-fx-border-width: 2;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;"
        );
        // Set smaller size for compact menu
        container.setPrefSize(280, 200);
        container.setMaxSize(280, 200);

        Label title = new Label("Paused");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24)); // Smaller title
        title.setTextFill(Color.WHITE);

        Button resumeButton = createMenuButton("Resume");
        resumeButton.setOnAction(e -> {
            // If simulation is paused, resume it
            if (gameController.isSimulationMode() && gameController.getGameState().isPaused()) {
                gameController.resumeGame();
            }
            // Always hide the overlay on resume click
            hide();
        });

        Button restartButton = createMenuButton("Restart Level");
        restartButton.setOnAction(e -> {
            if (gameController.getMainApp() != null) {
                // Hide overlay immediately to reflect action
                hide();
                // Restart current level without prompts
                gameController.getMainApp().restartCurrentLevelNow();
            }
        });

        Button mainMenuButton = createMenuButton("Main Menu");
        mainMenuButton.setOnAction(e -> {
            if (gameController.getMainApp() != null) {
                gameController.getMainApp().returnToMainMenu();
            }
        });

        container.getChildren().addAll(title, resumeButton, restartButton, mainMenuButton);
        root.getChildren().add(container);
    }

    private Button createMenuButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(180); // Smaller width
        button.setPrefHeight(35); // Smaller height
        button.setFont(Font.font("Arial", FontWeight.BOLD, 12)); // Smaller font
        button.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: cyan;" +
                        "-fx-border-width: 2;" +
                        "-fx-text-fill: white;" +
                        "-fx-cursor: hand;"
        );
        return button;
    }

    public void show() {
        isVisible = true;
        root.setVisible(true);
        root.toFront();
    }

    public void hide() {
        isVisible = false;
        root.setVisible(false);
    }

    public void toggleVisibility() {
        if (isVisible) hide(); else show();
    }

    public StackPane getRoot() {
        return root;
    }

    public boolean isVisible() {
        return isVisible;
    }
}
