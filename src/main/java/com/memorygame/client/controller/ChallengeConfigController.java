package com.memorygame.client.controller;

import java.util.HashMap;
import java.util.Map;

import com.memorygame.client.NetworkClient;
import com.memorygame.client.SceneManager;
import com.memorygame.common.Message;
import com.memorygame.common.Player;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.util.StringConverter;

public class ChallengeConfigController {
    @FXML
    private Slider sldThinkTime;

    @FXML
    private Label lblThinkTimeValue;
    
    @FXML
    private Slider sldRounds;

    @FXML
    private Label lblRoundsValue;
    
    @FXML
    private Slider sldWaitTime;

    @FXML
    private Label lblWaitTimeValue;

    private SceneManager sceneManager;
    private NetworkClient networkClient;
    private Player opponent;

    @FXML
    public void initialize() {
        
        // Liên kết Thời gian nhớ với Slider 1
        bindLabelToSlider(lblThinkTimeValue, sldThinkTime, "s");
        
        // Liên kết Số vòng với Slider 2
        bindLabelToSlider(lblRoundsValue, sldRounds, " Lượt");
        
        // Liên kết Thời gian trả lời với Slider 3
        bindLabelToSlider(lblWaitTimeValue, sldWaitTime, "s");
    }

    public void setupController(SceneManager sceneManager, NetworkClient networkClient, Player opponent) {
        this.sceneManager = sceneManager;
        this.networkClient = networkClient;
        this.opponent = opponent;
    }

    /*Tự động cập nhật Label khi Slider thay đổi */
    private void bindLabelToSlider(Label label, Slider slider, String suffix) {
        StringConverter<Number> converter = new StringConverter<Number>() {
            @Override
            public String toString(Number n) {
                return String.format("%.0f", n.doubleValue()) + suffix;
            }
            @Override
            public Number fromString(String string) {
                return Integer.parseInt(string.replace(suffix, ""));
            }
        };
        label.textProperty().bindBidirectional(slider.valueProperty(), converter);
    }

    @FXML
    private void startChallenge() {
        if (opponent == null) {
            showAlert("Lỗi", "Không tìm thấy đối thủ để thách đấu.");
            return;
        }
        long thinkTime = (long) sldThinkTime.getValue();
        int totalRounds = (int) sldRounds.getValue();
        long waitTime = (long) sldWaitTime.getValue();

        Map<String, Object> invitePayload = new HashMap<>();
        invitePayload.put("opponentUsername", opponent.getUsername());
        invitePayload.put("thinkTime", thinkTime);
        invitePayload.put("totalRounds", totalRounds);
        invitePayload.put("waitTime", waitTime);

        networkClient.sendMessage(new Message("INVITE", invitePayload));
        System.out.println("DA GUI MESSAGE INVITE TOI CLIENTHANDLER"); 
        sceneManager.showLobbyScene();
    }

    @FXML
    private void backToMenu() {
        sceneManager.showMainMenuScene();
    }

    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR, content, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.show();
    }
}