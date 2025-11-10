package com.memorygame.client.controller;

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

public class PracticeGameController {
    @FXML
    private Label lblRound; // Current round 

    @FXML
    private Label lblPoints; // Tổng điểm 

    @FXML
    private Label lblTimer; // Thời gian khả dụng để trả lời 

    @FXML
    private Label lblWord; // Vocabulary 

    @FXML
    private Label lblHint;

    @FXML
    private TextField txtAnswer;

    @FXML
    private Button btnSubmit;

    @FXML
    private Label lblTotalRounds; // Tổng số round 

    @FXML
    private Label lblMemorizeTime; // Thời gian nhớ 

    private SceneManager sceneManager;
    private NetworkClient networkClient;
    private Timeline gameTimer;
    private int remainingTime;
    
    private enum GameState {
        MEMORIZING,
        ANSWERING,
        WAITING,
        ENDED
    }
    private GameState currentState;

    @FXML
    public void initialize() {
        lblRound.setText("0");
        lblPoints.setText("0");
        lblTimer.setText("0s");
        lblWord.setText("Đang tải...");
        lblHint.setText("Chào mừng đến với luyện tập!");
        // (Kiểm tra xem bạn đã thêm 2 ID này vào FXML chưa)
        if (lblTotalRounds != null) lblTotalRounds.setText("Tổng Vòng: 0");
        if (lblMemorizeTime != null) lblMemorizeTime.setText("Thời gian nhớ: 0s");
        setUIState(GameState.WAITING); 
    }

    public void setupController(SceneManager sceneManager, NetworkClient networkClient) {
        this.sceneManager = sceneManager;
        this.networkClient = networkClient;
    }

    public void setupGameInfo(int totalRounds, long memorizeTime) {
        Platform.runLater(() -> {
            if (lblTotalRounds != null) {
                lblTotalRounds.setText("Tổng Vòng: " + totalRounds);
            }
            if (lblMemorizeTime != null) {
                lblMemorizeTime.setText("Thời gian nhớ: " + memorizeTime + "s");
            }
            lblHint.setText("Game bắt đầu!");
        });
    }

    public void onNewRound(String word, int round, int memorizeTime) {
        Platform.runLater(() -> {
            lblWord.setText(word);
            lblRound.setText(String.format("%02d", round));
            lblHint.setText("Ghi nhớ!");
            setUIState(GameState.MEMORIZING);
            startTimer(memorizeTime); // Bắt đầu đếm ngược (chỉ để hiển thị)
        });
    }

    public void onAnswerPhase(int answerTime) {
        Platform.runLater(() -> {
            lblWord.setText("???");
            lblHint.setText("Trả lời!");
            setUIState(GameState.ANSWERING);
            startTimer(answerTime); // Bắt đầu đếm ngược
        });
    }

    public void onScoreUpdate(int newScore) {
        Platform.runLater(() -> {
            lblPoints.setText(String.valueOf(newScore));
            lblHint.setText("Chờ vòng tiếp theo...");
            setUIState(GameState.WAITING);
        });
    }

    public void onGameOver(int finalScore) {
        Platform.runLater(() -> {
            if (gameTimer != null) gameTimer.stop();
            setUIState(GameState.ENDED);
            lblWord.setText("Kết thúc!");
            lblHint.setText("Hoàn thành luyện tập!");

            // Hiển thị Alert thông báo
            Alert alert = new Alert(Alert.AlertType.INFORMATION, 
                "Bạn đã hoàn thành bài luyện tập với số điểm: " + finalScore, 
                ButtonType.OK);
            alert.setTitle("Kết thúc luyện tập");
            alert.setHeaderText(null);
            alert.showAndWait(); // Chờ người dùng nhấn OK
            
            sceneManager.showMainMenuScene(); // Quay về menu
        });
    }

    @FXML
    private void submitAnswer() {
        if (currentState != GameState.ANSWERING) return;
        if (gameTimer != null) gameTimer.stop();

        String answer = txtAnswer.getText().trim();
        networkClient.sendMessage(new Message("C_SUBMIT_ANSWER", answer));

        setUIState(GameState.WAITING);
        lblHint.setText("Đã gửi, đang chờ chấm điểm...");
        txtAnswer.clear();
    }

    @FXML
    private void endPractice() {
        if (gameTimer != null) gameTimer.stop();
        networkClient.sendMessage(new Message("C_LEAVE_GAME", null));
        sceneManager.showMainMenuScene();
    }

    private void setUIState(GameState state) {
        this.currentState = state;
        switch (state) {
            case MEMORIZING:
            case WAITING:
            case ENDED:
                txtAnswer.setDisable(true);
                btnSubmit.setDisable(true);
                break;
            case ANSWERING:
                txtAnswer.setDisable(false);
                btnSubmit.setDisable(false);
                txtAnswer.requestFocus();
                break;
        }
    }

    private void startTimer(int seconds) {
        if (gameTimer != null) gameTimer.stop();
        remainingTime = seconds;
        lblTimer.setText(remainingTime + "s");
        
        gameTimer = new Timeline();
        gameTimer.setCycleCount(seconds + 1); 
        
        KeyFrame frame = new KeyFrame(Duration.seconds(1), event -> {
            if (remainingTime > 0) remainingTime--;
            lblTimer.setText(remainingTime + "s");
        });
        
        gameTimer.getKeyFrames().add(frame);
        
        gameTimer.setOnFinished(event -> {
            if (currentState == GameState.ANSWERING) {
                lblHint.setText("Hết giờ!");
            }
        });
        
        gameTimer.playFromStart();
    }
}