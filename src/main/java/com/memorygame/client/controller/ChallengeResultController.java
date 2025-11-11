package com.memorygame.client.controller;

import com.memorygame.client.ClientState;
import com.memorygame.client.NetworkClient;
import com.memorygame.client.SceneManager;
import com.memorygame.common.Player;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class ChallengeResultController {
    @FXML private Label lblResultIcon;
    @FXML private Label lblResultTitle;
    @FXML private Label lblYourScore;
    @FXML private Label lblOpponentName;
    @FXML private Label lblOpponentScore;
    @FXML private Button btnRematch;

    private SceneManager sceneManager;
    private NetworkClient networkClient;
    private String opponentUsername;
    private Player opponent;

    @FXML
    public void initialize() {
        // Mặc định
        lblResultIcon.setText("⏳");
        lblResultTitle.setText("Đang tải kết quả...");
    }

    public void setupController(SceneManager sceneManager, NetworkClient networkClient) {
        this.sceneManager = sceneManager;
        this.networkClient = networkClient;
    }

    /**
     * Hiển thị kết quả trận đấu
     * @param winnerUsername - username người thắng (null nếu hòa)
     * @param yourScore - điểm của bạn
     * @param opponentUsername - username đối thủ
     * @param opponentScore - điểm đối thủ
     */
    public void showResult(String winnerUsername, int yourScore, Player opponent, int opponentScore) {
        Platform.runLater(() -> {
            this.opponent = opponent;
            this.opponentUsername = opponent.getUsername();
            
            String myUsername = ClientState.getInstance().getCurrentUsername();
            
            // Cập nhật điểm số
            lblYourScore.setText(String.valueOf(yourScore));
            lblOpponentName.setText("👤 " + opponentUsername + ":");
            lblOpponentScore.setText(String.valueOf(opponentScore));

            // Xác định kết quả (Thắng/Thua/Hòa)
            if (winnerUsername == null) {
                // HÒA
                lblResultIcon.setText("🤝");
                lblResultTitle.setText("HÒA!");
                lblResultTitle.setStyle("-fx-font-size: 42px; -fx-font-weight: 900; -fx-text-fill: #f59e0b;");
            } else if (winnerUsername.equals(myUsername)) {
                // THẮNG
                lblResultIcon.setText("🏆");
                lblResultTitle.setText("CHIẾN THẮNG!");
                lblResultTitle.setStyle("-fx-font-size: 42px; -fx-font-weight: 900; -fx-text-fill: #10b981;");
            } else {
                // THUA
                lblResultIcon.setText("😔");
                lblResultTitle.setText("THẤT BẠI");
                lblResultTitle.setStyle("-fx-font-size: 42px; -fx-font-weight: 900; -fx-text-fill: #ef4444;");
            }
        });
    }

    @FXML
    private void handleRematch() {
        if (this.opponent != null) {
            sceneManager.showChallengeConfigScene(this.opponent);
        } else {
            sceneManager.showLobbyScene();
        }
    }

    @FXML
    private void backToLobby() {
        sceneManager.showLobbyScene();
    }

    @FXML
    private void backToMenu() {
        sceneManager.showMainMenuScene();
    }
}