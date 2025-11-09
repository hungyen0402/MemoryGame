package com.memorygame.client.controller;

import com.memorygame.client.NetworkClient;
import com.memorygame.client.SceneManager;
import com.memorygame.common.GameSession;
import com.memorygame.common.Message;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class PracticeGameController {
    @FXML
    private Label lblRound; 

    @FXML
    private Label lblPoints;

    @FXML
    private Label lblTimer;

    @FXML
    private Label lblWord;

    @FXML
    private Label lblHint;

    @FXML
    private TextField txtAnswer;

    @FXML
    private Button btnSubmit;

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
        setUIState(GameState.WAITING); 
    }

    public void setupController(SceneManager sceneManager, NetworkClient networkClient) {
        this.sceneManager = sceneManager;
        this.networkClient = networkClient;
    }

    public void setupGameInfo(GameSession session) {
        // Hàm này được gọi khi SceneManager nhận S_PRACTICE_START
        lblHint.setText("Game bắt đầu!");
    }

    public void onNewRound(String word, int round, int memorizeTime) {
        lblWord.setText(word);
        lblRound.setText(String.format("%02d", round));
        lblHint.setText("Ghi nhớ!");
        setUIState(GameState.MEMORIZING);
        startTimer(memorizeTime); // Bắt đầu đếm ngược (chỉ để hiển thị)
    }

    public void onAnswerPhase(int answerTime) {
        lblWord.setText("???");
        lblHint.setText("Trả lời!");
        setUIState(GameState.ANSWERING);
        startTimer(answerTime); // Bắt đầu đếm ngược
    }

    public void onScoreUpdate(int newScore) {
        lblPoints.setText(String.valueOf(newScore));
        lblHint.setText("Chờ vòng tiếp theo...");
        setUIState(GameState.WAITING);
    }

    public void onGameOver(int finalScore) {
        if (gameTimer != null) gameTimer.stop();
        setUIState(GameState.ENDED);
        lblWord.setText("Kết thúc!");
        lblHint.setText("Hoàn thành luyện tập!");
        // (Hiển thị Alert...)
        sceneManager.showMainMenuScene();
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