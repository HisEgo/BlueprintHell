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
import model.AbilityType;

public class ShopView {
    private GameController gameController;
    private javafx.scene.layout.Pane root;
    private VBox shopContainer;
    private boolean isVisible;
    private javafx.stage.Popup popup;

    public ShopView(GameController gameController) {
        this.gameController = gameController;
        this.isVisible = false;
        initializeUI();
    }

    private void initializeUI() {
        // Create the root pane as a direct overlay (no popup)
        root = new javafx.scene.layout.Pane();
        root.setStyle("-fx-background-color: rgba(255, 255, 0, 0.9);"); // Semi-transparent bright yellow
        root.setPrefSize(1536, 824); // Match screen size
        root.setMaxSize(1536, 824);
        root.setVisible(false); // Start hidden

        // Create a bright yellow rectangle that covers the entire screen
        javafx.scene.shape.Rectangle testRect = new javafx.scene.shape.Rectangle(0, 0, 1536, 824);
        testRect.setFill(javafx.scene.paint.Color.YELLOW);
        testRect.setStroke(javafx.scene.paint.Color.RED);
        testRect.setStrokeWidth(10);

        // Add a text label to make it obvious
        javafx.scene.text.Text testText = new javafx.scene.text.Text("SHOP IS VISIBLE! Press S to close");
        testText.setFont(javafx.scene.text.Font.font("Arial", 48));
        testText.setFill(javafx.scene.paint.Color.BLACK);
        testText.setX(300);
        testText.setY(400);

        root.getChildren().add(testRect);
        root.getChildren().add(testText);

        // Initialize shop container (but don't add it yet)
        shopContainer = new VBox(20);
        shopContainer.setAlignment(Pos.CENTER);
        shopContainer.setPadding(new Insets(40));
        shopContainer.setPrefSize(600, 400);
        shopContainer.setMaxSize(600, 400);
        shopContainer.setLayoutX(420);
        shopContainer.setLayoutY(230);
        shopContainer.setMinSize(600, 400);
        shopContainer.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e);" +
                        "-fx-border-color: cyan;" +
                        "-fx-border-width: 2;" +
                        "-fx-background-radius: 10;"
        );

        // Title
        Label title = new Label("Network Abilities Shop");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.CYAN);

        // Phase 1 Abilities
        Label phase1Title = new Label("Phase 1 Abilities:");
        phase1Title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        phase1Title.setTextFill(Color.WHITE);

        VBox phase1Abilities = new VBox(10);
        phase1Abilities.getChildren().addAll(
                createAbilityButton(AbilityType.O_ATAR),
                createAbilityButton(AbilityType.O_AIRYAMAN),
                createAbilityButton(AbilityType.O_ANAHITA)
        );

        // Phase 2 Abilities
        Label phase2Title = new Label("Phase 2 Abilities:");
        phase2Title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        phase2Title.setTextFill(Color.WHITE);

        VBox phase2Abilities = new VBox(10);
        phase2Abilities.getChildren().addAll(
                createAbilityButton(AbilityType.SCROLL_OF_AERGIA),
                createAbilityButton(AbilityType.SCROLL_OF_SISYPHUS),
                createAbilityButton(AbilityType.SCROLL_OF_ELIPHAS)
        );

        // Close button
        Button closeButton = new Button("Close Shop");
        closeButton.setPrefWidth(150);
        closeButton.setPrefHeight(40);
        closeButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        closeButton.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: red;" +
                        "-fx-border-width: 2;" +
                        "-fx-text-fill: red;" +
                        "-fx-cursor: hand;"
        );
        closeButton.setOnAction(e -> {
            if (gameController != null) {
                gameController.toggleShop();
            }
        });

        shopContainer.getChildren().addAll(
                title, phase1Title, phase1Abilities, phase2Title, phase2Abilities, closeButton
        );

        root.getChildren().add(shopContainer);
        root.setVisible(false);
    }

    private HBox createAbilityButton(AbilityType abilityType) {
        HBox abilityRow = new HBox(15);
        abilityRow.setAlignment(Pos.CENTER_LEFT);
        abilityRow.setPadding(new Insets(5));

        // Ability name and description
        VBox abilityInfo = new VBox(2);
        Label nameLabel = new Label(abilityType.getDisplayName());
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        nameLabel.setTextFill(Color.CYAN);

        Label descLabel = new Label(abilityType.getDescription());
        descLabel.setFont(Font.font("Arial", 10));
        descLabel.setTextFill(Color.LIGHTGRAY);

        abilityInfo.getChildren().addAll(nameLabel, descLabel);

        // Cost label
        Label costLabel = new Label(abilityType.getCost() + " coins");
        costLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        costLabel.setTextFill(Color.YELLOW);

        // Buy button
        Button buyButton = new Button("Buy");
        buyButton.setPrefWidth(80);
        buyButton.setPrefHeight(30);
        buyButton.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        buyButton.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: green;" +
                        "-fx-border-width: 1;" +
                        "-fx-text-fill: green;" +
                        "-fx-cursor: hand;"
        );

        buyButton.setOnAction(e -> {
            if (gameController.activateAbility(abilityType)) {
                updateButtonStates();
            } else {
                // Show error message or visual feedback
                buyButton.setText("Can't Buy");
                buyButton.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-border-color: red;" +
                                "-fx-border-width: 1;" +
                                "-fx-text-fill: red;" +
                                "-fx-cursor: hand;"
                );
            }
        });

        abilityRow.getChildren().addAll(abilityInfo, costLabel, buyButton);
        return abilityRow;
    }

    private void updateButtonStates() {
        // Update all buy buttons based on current coins and cooldowns
        // This would be called after a purchase to refresh the UI
    }

    public void toggleVisibility() {
        isVisible = !isVisible;

        java.lang.System.out.println("toggleVisibility called, isVisible: " + isVisible);

        if (isVisible) {
            // Show the overlay directly by setting visibility
            root.setVisible(true);
            root.toFront(); // Bring to front of the scene graph

            java.lang.System.out.println("Shop overlay shown directly (visible: " + root.isVisible() + ")");
            updateButtonStates();
        } else {
            // Hide the overlay
            root.setVisible(false);
            java.lang.System.out.println("Shop overlay hidden");
        }
    }

    public javafx.scene.layout.Pane getRoot() {
        return root;
    }

    public boolean isVisible() {
        return isVisible;
    }
}

