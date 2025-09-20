package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import controller.GameController;

public class LevelSelectView {
    private GameController gameController;
    private StackPane root;
    private Runnable onBackToMainMenu;

    public LevelSelectView(GameController gameController) {
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

        // Title
        Label title = new Label("Select Level");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.setTextFill(Color.CYAN);

        // Level buttons
        VBox levelButtons = new VBox(15);
        levelButtons.setAlignment(Pos.CENTER);

        // Level 1 - Tutorial
        Button level1Button = createLevelButton("Level 1: Tutorial - Basic Network",
                "Learn the basics with messenger packets and normal systems", "level1", Color.GREEN);

        // Level 2 - Intermediate
        Button level2Button = createLevelButton("Level 2: Intermediate - Spy Networks",
                "Introduces spy systems and confidential packets", "level2", Color.YELLOW);

        // Level 3 - Advanced
        Button level3Button = createLevelButton("Level 3: Advanced - Saboteurs & VPN",
                "Adds saboteur systems and VPN protection", "level3", Color.ORANGE);

        // Level 4 - Expert
        Button level4Button = createLevelButton("Level 4: Expert - Anti-Trojan & Bulk",
                "Features anti-trojan systems and bulk packets", "level4", Color.RED);

        // Level 5 - Master
        Button level5Button = createLevelButton("Level 5: Master - Complete Network",
                "All systems and complex packet types", "level5", Color.PURPLE);

        levelButtons.getChildren().addAll(
                level1Button, level2Button, level3Button, level4Button, level5Button
        );

        // Back button
        Button backButton = createMenuButton("Back to Main Menu");
        backButton.setOnAction(e -> {
            if (onBackToMainMenu != null) {
                onBackToMainMenu.run();
            }
        });

        container.getChildren().addAll(title, levelButtons, backButton);
        root.getChildren().add(container);
    }

    private Button createLevelButton(String title, String description, String levelId, Color difficultyColor) {
        VBox buttonContent = new VBox(5);
        buttonContent.setAlignment(Pos.CENTER);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.WHITE);

        Label descLabel = new Label(description);
        descLabel.setFont(Font.font("Arial", 10));
        descLabel.setTextFill(Color.LIGHTGRAY);
        descLabel.setWrapText(true);

        buttonContent.getChildren().addAll(titleLabel, descLabel);

        Button button = new Button();
        button.setGraphic(buttonContent);
        button.setPrefWidth(400);
        button.setPrefHeight(80);
        button.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: " + difficultyColor.toString().replace("0x", "#") + ";" +
                        "-fx-border-width: 2;" +
                        "-fx-cursor: hand;"
        );

        button.setOnAction(e -> {
            System.out.println("Level button clicked: " + levelId);
            try {
                if (gameController.getMainApp() != null) {
                    System.out.println("Using MainApp.startGame for proper scene transition");
                    gameController.getMainApp().startGame(levelId);
                } else {
                    System.err.println("MainApp is null, falling back to direct controller calls");
                    gameController.loadLevel(levelId);
                    gameController.startGame();
                }
                System.out.println("Level transition completed: " + levelId);
            } catch (Exception ex) {
                System.err.println("Error loading/starting level " + levelId + ": " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        return button;
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

