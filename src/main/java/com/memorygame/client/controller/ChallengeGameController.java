package com.memorygame.client.controller;

import com.memorygame.client.ClientState;
import com.memorygame.client.NetworkClient;
import com.memorygame.client.SceneManager;
import com.memorygame.common.GameSession;
import com.memorygame.common.Message;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;

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
    private Timeline gameTimer;
    private int remainingTime;

    private enum GameState {
        MEMORIZING, // Đang ghi nhớ
        ANSWERING,  // Đang trả lời
        WAITING,     // Đang chờ (đã gửi câu trả lời)
        ENDED       // Game kết thúc
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
        lblMemorizeTime.setText("Thời gian nhớ: 0s");

        setUIState(GameState.WAITING);
    }

    public void setupController(SceneManager sceneManager, NetworkClient networkClient) {
        this.sceneManager = sceneManager;
        this.networkClient = networkClient;
    }

    public void setupGameInfo(GameSession session) {
        int totalRounds = session.getTotalRounds();
        long memorizeTime = session.getDisplayTimes();
        
        Platform.runLater(() -> {
            lblTotalRounds.setText("Tổng Vòng: " + totalRounds);
            lblMemorizeTime.setText("Thời gian nhớ: " + memorizeTime + "s");
        });
    }

    @FXML
    private void sendAnswer() {
        if (currentState != GameState.ANSWERING) {
            return; // Không cho gửi nếu không phải lúc
        }

        if (gameTimer != null) {
            gameTimer.stop(); // Dừng đồng hồ
        }

        String answer = txtAnswer.getText().trim();

        networkClient.sendMessage(new Message("C_SUBMIT_ANSWER", answer));

        setUIState(GameState.WAITING);
        lblGameStatus.setText("Đã gửi! Chờ kết quả...");
        txtAnswer.clear();
    }

    @FXML
    private void backToMenu() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, 
            "Bạn có chắc muốn rời trận? Kết quả sẽ bị hủy và tính là một trận thua.", 
            ButtonType.YES, ButtonType.NO);
        alert.setTitle("Xác nhận rời trận");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                if (gameTimer != null) gameTimer.stop();
                // Gửi tin nhắn thông báo cho server
                networkClient.sendMessage(new Message("C_LEAVE_GAME", null));
                sceneManager.showMainMenuScene();
            }
        });
    }

    /*Server báo hiệu round mới */
    public void onNewRound(String word, int round, int memorizeTime) {
        lblWord.setText(word);
        lblCurrentRound.setText(String.format("%02d", round));
        lblGameStatus.setText("GHI NHỚ!");
        setUIState(GameState.MEMORIZING);
        startTimer(memorizeTime); // Bắt đầu đếm ngược thời gian nhớ
    }

    /*Server báo hiệu đến lúc trả lời */
    public void onAnswerPhase(int answerTime) {
        lblWord.setText("???"); // Ẩn từ
        lblGameStatus.setText("TRẢ LỜI!");
        setUIState(GameState.ANSWERING);
        startTimer(answerTime); // Bắt đầu đếm ngược thời gian trả lời
    }

    /*Cập nhật điểm số sau khi chấm */
    public void onScoreUpdate(int playerScore, int opponentScore) {
        lblPlayerScore.setText(String.valueOf(playerScore));
        lblOpponentScore.setText(String.valueOf(opponentScore));
        lblGameStatus.setText("Chờ vòng tiếp theo...");
        setUIState(GameState.WAITING);
    }

    /*Kết thúc game */
    public void onGameOver(String winnerUsername, int finalPlayerScore, int finalOpponentScore) {
        if (gameTimer != null) gameTimer.stop();
        
        // Cập nhật điểm số cuối cùng
        lblPlayerScore.setText(String.valueOf(finalPlayerScore));
        lblOpponentScore.setText(String.valueOf(finalOpponentScore));
        
        setUIState(GameState.ENDED);
        lblGameStatus.setText("KẾT THÚC!");

        String myUsername = ClientState.getInstance().getCurrentUsername();
        
        // Thông báo kết quả
        String message;
        if (winnerUsername == null) {
            message = "Kết quả: HÒA!";
        } else if (winnerUsername.equals(myUsername)) {
            message = "Chúc mừng! BẠN ĐÃ THẮNG!";
        } else {
            message = "Bạn đã thua! Đối thủ " + winnerUsername + " thắng.";
        }
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setTitle("Kết thúc trận đấu");
        alert.setHeaderText("Trận đấu đã kết thúc");
        alert.showAndWait();
        
        // Tự động quay về Menu
        sceneManager.showMainMenuScene();
    }

    /*Cập nhật trạng thái giao diện (bật tắt input) */
    private void setUIState(GameState state) {
        this.currentState = state;

        switch (state) {
            case MEMORIZING -> {}
            case WAITING -> {}
            case ENDED -> {
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
        if (gameTimer != null) {
            gameTimer.stop();
        }
        
        remainingTime = seconds;
        lblCountdown.setText(remainingTime + "s");
        
        gameTimer = new Timeline();
        gameTimer.setCycleCount(seconds + 1); // Chạy (seconds + 1) frame
        
        KeyFrame frame = new KeyFrame(Duration.seconds(1), event -> {
            if (remainingTime > 0) {
                remainingTime--;
            }
            lblCountdown.setText(remainingTime + "s");
        });
        
        gameTimer.getKeyFrames().add(frame);
        
        // Khi chạy xong server sẽ tự gửi tin nhắn, timer này chỉ để hiển thị
        gameTimer.setOnFinished(event -> {
            if (currentState == GameState.ANSWERING) {
                lblGameStatus.setText("Hết giờ!");
            }
        });
        
        gameTimer.playFromStart();
    }
}
