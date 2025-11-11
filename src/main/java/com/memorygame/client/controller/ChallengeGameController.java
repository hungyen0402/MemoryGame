package com.memorygame.client.controller;

import java.util.Map;

import com.memorygame.client.ClientState;
import com.memorygame.client.NetworkClient;
import com.memorygame.client.SceneManager;
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
    @FXML private Label lblCountdown;
    @FXML private Label lblGameStatus;
    @FXML private Label lblWord;
    @FXML private TextField txtAnswer;
    @FXML private Button btnSend;
    @FXML private Label lblPlayerScore;
    @FXML private Label lblOpponentScore;
    @FXML private Label lblCurrentRound;
    @FXML private Label lblTotalRounds;
    @FXML private Label lblMemorizeTime;

    private SceneManager sceneManager;
    private NetworkClient networkClient;
    private Timeline gameTimer;
    private int remainingTime;
    private String opponentUsername;

    private enum GameState {
        MEMORIZING, // Đang ghi nhớ - KHÓA input
        ANSWERING,  // Đang trả lời - MỞ input
        WAITING,    // Đã gửi câu trả lời, chờ đối thủ - KHÓA input
        ENDED       // Game kết thúc
    }
    private GameState currentState;

    @FXML
    public void initialize() {
        lblCountdown.setText("0s");
        lblWord.setText("Chờ đối thủ...");
        lblGameStatus.setText("Đang kết nối...");
        setUIState(GameState.WAITING);
    }

    public void setupController(SceneManager sceneManager, NetworkClient networkClient) {
        this.sceneManager = sceneManager;
        this.networkClient = networkClient;
    }

    public void setupGameInfo(Map<String, Object> gameInfo) {
        String opponentUsername = (String) gameInfo.get("opponentUsername");
        int totalRounds = (int) gameInfo.get("totalRounds");
        long thinkTime = (long) gameInfo.get("thinkTime");

        Platform.runLater(() -> {
            lblTotalRounds.setText("Tổng Vòng: " + totalRounds);
            lblMemorizeTime.setText("Thời gian nhớ: " + thinkTime + "s");
            lblPlayerScore.setText("0");
            lblOpponentScore.setText("0");
            lblCurrentRound.setText("01");
        });

        this.opponentUsername = opponentUsername;
        System.out.println("VÀO GAME: vs " + opponentUsername);
    }

    @FXML
    private void sendAnswer() {
        if (currentState != GameState.ANSWERING) {
            return;
        }

        if (gameTimer != null) {
            gameTimer.stop();
        }

        String answer = txtAnswer.getText().trim();
        networkClient.sendMessage(new Message("C_SUBMIT_ANSWER", answer));

        // ✅ CHUYỂN SANG TRẠNG THÁI WAITING (Đã gửi, chờ đối thủ)
        setUIState(GameState.WAITING);
        lblGameStatus.setText("Đã gửi! Chờ đối thủ...");
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
                networkClient.sendMessage(new Message("C_LEAVE_GAME", null));
                sceneManager.showMainMenuScene();
            }
        });
    }

    // ✅ SỬA: Khóa input khi đang ghi nhớ
    public void onNewRound(String word, int round, int memorizeTime) {
        Platform.runLater(() -> {
            lblWord.setText(word);
            lblCurrentRound.setText(String.format("%02d", round));
            lblGameStatus.setText("GHI NHỚ!");
            
            // ✅ QUAN TRỌNG: Set state MEMORIZING để khóa input
            setUIState(GameState.MEMORIZING);
            startTimer(memorizeTime);
        });
    }

    // ✅ Mở input khi đến lúc trả lời
    public void onAnswerPhase(int answerTime) {
        Platform.runLater(() -> {
            lblWord.setText("???");
            lblGameStatus.setText("TRẢ LỜI!");
            
            // ✅ Mở input
            setUIState(GameState.ANSWERING);
            startTimer(answerTime);
        });
    }

    // ✅ SỬA: Nhận điểm của cả 2 người (array thay vì int)
    public void onScoreUpdate(int playerScore, int opponentScore) {
        Platform.runLater(() -> {
            lblPlayerScore.setText(String.valueOf(playerScore));
            lblOpponentScore.setText(String.valueOf(opponentScore));
            lblGameStatus.setText("Chờ vòng tiếp theo...");
            
            // ✅ Khóa input khi chờ vòng mới
            setUIState(GameState.WAITING);
        });
    }

    public void onGameOver(String winnerUsername, int finalPlayerScore, int finalOpponentScore) {
        if (gameTimer != null) gameTimer.stop();
        
        lblPlayerScore.setText(String.valueOf(finalPlayerScore));
        lblOpponentScore.setText(String.valueOf(finalOpponentScore));
        
        setUIState(GameState.ENDED);
        lblGameStatus.setText("KẾT THÚC!");

        String myUsername = ClientState.getInstance().getCurrentUsername();
        
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
        
        sceneManager.showMainMenuScene();
    }

    // ✅ QUAN TRỌNG: Xử lý enable/disable input đúng cách
    private void setUIState(GameState state) {
        this.currentState = state;

        switch (state) {
            case MEMORIZING:
                // Đang ghi nhớ: KHÓA input
                txtAnswer.setDisable(true);
                btnSend.setDisable(true);
                break;
                
            case WAITING:
                // Chờ đối thủ hoặc chờ round mới: KHÓA input
                txtAnswer.setDisable(true);
                btnSend.setDisable(true);
                break;
                
            case ENDED:
                // Game kết thúc: KHÓA input
                txtAnswer.setDisable(true);
                btnSend.setDisable(true);
                break;
                
            case ANSWERING:
                // Đang trả lời: MỞ input
                txtAnswer.setDisable(false);
                btnSend.setDisable(false);
                txtAnswer.requestFocus();
                break;
        }
    }

    private void startTimer(int seconds) {
        if (gameTimer != null) {
            gameTimer.stop();
        }
        
        remainingTime = seconds;
        lblCountdown.setText(remainingTime + "s");
        
        gameTimer = new Timeline();
        gameTimer.setCycleCount(seconds + 1);
        
        KeyFrame frame = new KeyFrame(Duration.seconds(1), event -> {
            if (remainingTime > 0) {
                remainingTime--;
            }
            lblCountdown.setText(remainingTime + "s");
        });
        
        gameTimer.getKeyFrames().add(frame);
        
        gameTimer.setOnFinished(event -> {
            if (currentState == GameState.ANSWERING) {
                lblGameStatus.setText("Hết giờ!");
            }
        });
        
        gameTimer.playFromStart();
    }

    public void stopGameTimer() {
        if (gameTimer != null) {
            gameTimer.stop();
        }
    }
}