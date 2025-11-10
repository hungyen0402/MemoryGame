package com.memorygame.client.controller;

import com.memorygame.client.NetworkClient;
import com.memorygame.client.SceneManager;
import com.memorygame.common.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.util.Map;

public class ChallengeInviteDialogController {
    @FXML private Label lblInviter, lblThinkTime, lblRounds, lblWaitTime;
    private SceneManager sceneManager;
    private NetworkClient networkClient;
    private Map<String, Object> inviteData;
    private Stage stage;

    public void setup(SceneManager sceneManager, NetworkClient networkClient, Map<String, Object> data, Stage stage) {
        this.sceneManager = sceneManager;
        this.networkClient = networkClient;
        this.inviteData = data;
        this.stage = stage;

        lblInviter.setText("Từ: " + data.get("inviterUsername") + " (Thắng: " + data.get("inviterWins") + ")");
        lblThinkTime.setText("Thời gian nhớ: " + data.get("thinkTime") + "s");
        lblRounds.setText("Số lượt: " + data.get("totalRounds") + " lượt");
        lblWaitTime.setText("Thời gian nhập: " + data.get("waitTime") + "s");
    }

    @FXML
    private void acceptInvite() {
        networkClient.sendMessage(new Message("C_ACCEPT_INVITE", inviteData));
        stage.close();
    }

    @FXML
    private void declineInvite() {
        networkClient.sendMessage(new Message("C_DECLINE_INVITE", inviteData.get("inviterUsername")));
        stage.close();
    }
}