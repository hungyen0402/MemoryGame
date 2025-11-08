package com.memorygame.client.controller;

import javafx.fxml.FXML;
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

    @FXML
    public void initialize() {
        
        // Liên kết Thời gian nhớ với Slider 1
        bindLabelToSlider(lblThinkTimeValue, sldThinkTime, "s");
        
        // Liên kết Số vòng với Slider 2
        bindLabelToSlider(lblRoundsValue, sldRounds, " Lượt");
        
        // Liên kết Thời gian trả lời với Slider 3
        bindLabelToSlider(lblWaitTimeValue, sldWaitTime, "s");
    }

    public void setupController() {

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

    }

    @FXML
    private void backToMenu() {
        // sceneManager.showMainMenuScene();
    }
}