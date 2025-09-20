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
import javafx.scene.shape.Rectangle;
import app.MainApp;

public class MainMenuView {
    private MainApp mainApp;
    private StackPane root;
    private VBox menuContainer;
    private Button freshStartButton;

    private Button gameLevelsButton;
    private Button gameSettingsButton;
    private Button exitGameButton;

    public MainMenuView(MainApp mainApp) {
        this.mainApp = mainApp;
        initializeUI();
    }

    private void initializeUI() {
        // Create root container
        root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e);");

        // Create background rectangle
        Rectangle background = new Rectangle(800, 600);
        background.setFill(Color.TRANSPARENT);
        background.setStroke(Color.CYAN);
        background.setStrokeWidth(2);

        // Create menu container
        menuContainer = new VBox(20);
        menuContainer.setAlignment(Pos.CENTER);
        menuContainer.setPadding(new Insets(50));

        // Create title
        Text title = new Text("Network Simulation Game");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.setFill(Color.CYAN);

        // Create subtitle
        Text subtitle = new Text("Advanced Programming Course Project");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        subtitle.setFill(Color.LIGHTGRAY);

        // Create description text
        Text description = new Text("Choose your game mode:");
        description.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        description.setFill(Color.LIGHTGRAY);

        // Create buttons
        createButtons();

        // Add components to menu container
        menuContainer.getChildren().addAll(title, subtitle, description, freshStartButton,
                gameLevelsButton, gameSettingsButton, exitGameButton);

        // Add everything to root
        root.getChildren().addAll(background, menuContainer);
    }

    private void createButtons() {
        // Fresh Start button - starts with no wire connections
        freshStartButton = createMenuButton("Fresh Start");
        freshStartButton.setOnAction(e -> mainApp.startFreshGame("level1"));

        // Game Levels button
        gameLevelsButton = createMenuButton("Game Levels");
        gameLevelsButton.setOnAction(e -> mainApp.showLevelSelect());

        // Game Settings button
        gameSettingsButton = createMenuButton("Game Settings");
        gameSettingsButton.setOnAction(e -> mainApp.showSettings());

        // Exit Game button
        exitGameButton = createMenuButton("Exit Game");
        exitGameButton.setOnAction(e -> mainApp.getPrimaryStage().fireEvent(
                new javafx.stage.WindowEvent(mainApp.getPrimaryStage(),
                        javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST)));
    }

    private Button createMenuButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(200);
        button.setPrefHeight(50);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        button.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: cyan;" +
                        "-fx-border-width: 2;" +
                        "-fx-text-fill: cyan;" +
                        "-fx-cursor: hand;"
        );

        // Hover effects
        button.setOnMouseEntered(e -> {
            button.setStyle(
                    "-fx-background-color: rgba(0, 255, 255, 0.2);" +
                            "-fx-border-color: cyan;" +
                            "-fx-border-width: 2;" +
                            "-fx-text-fill: white;" +
                            "-fx-cursor: hand;"
            );
        });

        button.setOnMouseExited(e -> {
            button.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-border-color: cyan;" +
                            "-fx-border-width: 2;" +
                            "-fx-text-fill: cyan;" +
                            "-fx-cursor: hand;"
            );
        });

        return button;
    }

    public StackPane getRoot() {
        return root;
    }
}

