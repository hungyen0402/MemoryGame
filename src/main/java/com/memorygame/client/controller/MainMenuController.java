package com.memorygame.client.controller;

import com.memorygame.client.ClientState;
import com.memorygame.client.NetworkClient;
import com.memorygame.client.SceneManager;
import com.memorygame.common.Message;
import com.memorygame.common.Player;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MainMenuController {
    @FXML
    private Label lblTotalWins; // Cần thêm fx:id="lblTotalWins" trong MainMenuScene.fxml

    @FXML
    private Label lblOnlineCount; // Cần thêm fx:id="lblOnlineCount" trong MainMenuScene.fxml

    private SceneManager sceneManager;
    private NetworkClient networkClient;

    @FXML
    public void initialize() {
        lblOnlineCount.setText("0");
        lblTotalWins.setText("0");

    }

    /**Cần truyền NetworkClient + tham chiếu đến đối tượng quản lý chung (SceneManager) vào đây
     * Gọi bởi SceneManager sau khi tải FXML. Tương tự với các controller khác
    */
    public void setupController(SceneManager sceneManager, NetworkClient networkClient) {
        this.sceneManager = sceneManager;
        this.networkClient = networkClient;
        Message message = new Message("C_ONLINE_COUNT", null); 
        networkClient.sendMessage(message);

        Message message2 = new Message("C_WIN_COUNT", null);
        networkClient.sendMessage(message2);

        Player currentPlayer = ClientState.getInstance().getCurrentPlayer();
        updatePlayerStats(currentPlayer);
    }

    @FXML
    private void openPractice() {
        sceneManager.showPracticeSettingsScene();
    }

    @FXML
    private void openChallenge() {
        sceneManager.showLobbyScene();
    }

    @FXML
    private void openLeaderboard() {
        sceneManager.showLeaderboardScene();
    }

    @FXML
    private void logout() {
        networkClient.logout();
        sceneManager.showLoginScene();
    }

    public void updateOnlineCount(int count_players) {
        Platform.runLater(() -> {
            lblOnlineCount.setText(String.valueOf(count_players));
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
