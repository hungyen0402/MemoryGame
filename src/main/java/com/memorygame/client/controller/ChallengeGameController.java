package com.memorygame.client.controller;

import com.memorygame.client.NetworkClient;
import com.memorygame.client.SceneManager;
import com.memorygame.common.GameSession;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ChallengeGameController {
    @FXML
    private Label lblCountdown;

    @FXML
    private Label lblGameStatus;

    @FXML
    private Label lblWord;

    @FXML
    private TextField txtAnswer;

    @FXML
    private Button btnSend;

    @FXML
    private Label lblPlayerScore;

    @FXML
    private Label lblOpponentScore;

    @FXML
    private Label lblCurrentRound;

    @FXML
    private Label lblTotalRounds;

    @FXML
    private Label lblMemorizeTime;

    private SceneManager sceneManager;
    private NetworkClient networkClient;

    private enum GameState {
        MEMORIZING, // Đang ghi nhớ
        ANSWERING,  // Đang trả lời
        WAITING     // Đang chờ (đã gửi câu trả lời)
    }
    private GameState currentState;

    @FXML
    public void initialize() {
        lblPlayerScore.setText("0");
        lblPlayerScore.setText("0");
        lblOpponentScore.setText("0");
        lblCurrentRound.setText("0");
        lblCountdown.setText("0s");
        lblWord.setText("Chờ đối thủ...");
        lblTotalRounds.setText("Tổng Vòng: 0");
        lblMemorizeTime.setText("Tạm dừng: 0s");

        setUIState(GameState.WAITING);
    }

    public void setupController(SceneManager sceneManager, NetworkClient networkClient) {
        this.sceneManager = sceneManager;
        this.networkClient = networkClient;
    }

    public void setupGameInfo(GameSession session) {
        int totalRounds = session.getTotalRounds();
        //long memorizeTime = session.getDisplayTimes();
        
        Platform.runLater(() -> {
            lblTotalRounds.setText("Tổng Vòng: " + totalRounds);
            lblMemorizeTime.setText("Tạm dừng: " ); // + memorizeTime
        });
    }

    @FXML
    private void sendAnswer() {

    }

    @FXML
    private void backToMenu() {
        sceneManager.showMainMenuScene();
    }

    /*Server báo hiệu round mới */
    public void onNewRound() {

    }

    /*Server báo hiệu đến lúc trả lời */
    public void onAnswerPhase() {

    }

    /*Cập nhật điểm số sau khi chấm */
    public void onScoreUpdate() {

    }

    /*Kết thúc game */
    public void onGameOver() {

    }

    /*Cập nhật trạng thái giao diện (bật tắt input) */
    private void setUIState(GameState state) {
        this.currentState = state;

        switch (state) {
            case MEMORIZING -> {}
            case WAITING -> {
                txtAnswer.setDisable(true);
                btnSend.setDisable(true);
            }
            case ANSWERING -> {
                txtAnswer.setDisable(false);
                btnSend.setDisable(false);
                txtAnswer.requestFocus(); // Tự động focus vào ô trả lời
            }
        }
    }

    /*Bắt đầu đồng hồ đếm ngược mới */
    private void startTimer(int seconds) {

    }
}
