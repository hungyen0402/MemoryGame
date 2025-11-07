package com.memorygame.client.controller;

import java.util.List;

import com.memorygame.common.Player;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MainMenuController {
    @FXML
    private Label lblTotalWins; // Cần thêm fx:id="lblTotalWins" trong MainMenuScene.fxml

    @FXML
    private Label lblOnlineCount; // Cần thêm fx:id="lblOnlineCount" trong MainMenuScene.fxml

    @FXML
    public void initialize() {
        lblOnlineCount.setText("0");
        lblTotalWins.setText("0");
    }

    /**Cần truyền NetworkClient + tham chiếu đến đối tượng quản lý chung (SceneManager) vào đây */
    public void setupController() {

    }

    @FXML
    private void openPractice() {
        // sceneManager.showPracticeSettingsScene();
    }

    @FXML
    private void openChallenge() {
        // sceneManager.showLobbyScene();
    }

    @FXML
    private void openLeaderboard() {
        // sceneManager.showLeaderboardScene();
    }

    @FXML
    private void logout() {
        // networkClient.logout();
        // sceneManager.showLoginScene();
    }

    public void updateOnlineList(List<Player> players) {
        int count = (players != null) ? players.size() : 0;
        
        Platform.runLater(() -> {
            lblOnlineCount.setText(String.valueOf(count));
        });
    }

    public void updatePlayerStats(Player player) {
        if (player != null) {
            Platform.runLater(() -> {
                lblTotalWins.setText(String.valueOf(player.getTotalWins()));
            });
        }
    }
}
