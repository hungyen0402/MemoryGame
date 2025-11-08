package com.memorygame.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

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
    
    private enum GameState {
        MEMORIZING,
        ANSWERING,
        ENDED
    }
    private GameState currentState;

    @FXML
    public void initialize() {
        lblRound.setText("0");
        lblPoints.setText("0");
        lblTimer.setText("0s");
        lblWord.setText(""); // Chưa nghĩ ra thêm gì vô đây
        lblHint.setText("");
        setUIState(GameState.ENDED); // Bắt đầu ở trạng thái kết thúc
    }
    
    public void setupGame() {

    }

    @FXML
    private void submitAnswer() {
        
    }

    @FXML
    private void endPractice() {
        
    }

    private void nextRound() {
        
    }
    
    private void startAnswerPhase() {
        
    }

    private void endGame() {
        
    }

    private void setUIState(GameState state) {
        this.currentState = state;
        
        switch (state) {
            case MEMORIZING:
            case ENDED:
                txtAnswer.setDisable(true);
                btnSubmit.setDisable(true);
                break;
            case ANSWERING:
                txtAnswer.setDisable(false);
                btnSubmit.setDisable(false);
                txtAnswer.requestFocus(); // Tự động focus
                break;
        }
    }

    private void startTimer() {
        
    }
}