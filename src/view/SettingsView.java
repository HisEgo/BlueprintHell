package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import controller.GameController;

public class SettingsView {
    private GameController gameController;
    private StackPane root;
    private Runnable onBackToMainMenu;

    public SettingsView(GameController gameController) {
        this.gameController = gameController;
        initializeUI();
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

        Text title = new Text("Game Settings");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.setFill(Color.CYAN);

        Label volumeLabel = new Label("Volume");
        volumeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        volumeLabel.setTextFill(Color.CYAN);

        Slider volumeSlider = new Slider(0, 100, 50);
        volumeSlider.setShowTickLabels(true);
        volumeSlider.setShowTickMarks(true);
        volumeSlider.setPrefWidth(300);

        // Connect volume slider to SoundManager
        volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (gameController != null && gameController.getSoundManager() != null) {
                // Convert percentage (0-100) to volume (0.0-1.0)
                double volume = newValue.doubleValue() / 100.0;
                gameController.getSoundManager().setVolume(volume);
            }
        });

        // Set initial volume from SoundManager
        if (gameController != null && gameController.getSoundManager() != null) {
            double currentVolume = gameController.getSoundManager().getVolume();
            volumeSlider.setValue(currentVolume * 100.0);
        }

        Button backButton = createMenuButton("Back to Main Menu");
        backButton.setOnAction(e -> {
            if (onBackToMainMenu != null) {
                onBackToMainMenu.run();
            }
        });

        container.getChildren().addAll(title, volumeLabel, volumeSlider, backButton);
        root.getChildren().add(container);
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
                        "-fx-cursor: hand;"
        );
        return button;
    }

    public StackPane getRoot() {
        return root;
    }
}
