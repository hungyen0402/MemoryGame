// src/main/java/com/memorygame/client/controller/MainMenuController.java
package com.memorygame.client.controller;

import com.memorygame.client.NetworkClient;
import com.memorygame.common.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

public class MainMenuController {

    @FXML private Label lblWins;
    @FXML private Label lblOnlinePlayers;

    private NetworkClient networkClient;

    @FXML
    private void initialize() {
        // Giả lập dữ liệu (sau này lấy từ DB)
        lblWins.setText("58");
        lblOnlinePlayers.setText("12");

        // Kết nối lại nếu chưa có
        networkClient = NetworkClient.getInstance();
        if (!networkClient.isConnected()) {
            networkClient.connect();
        }

        // Lắng nghe cập nhật online (từ server)
        networkClient.setMessageListener(msg -> {
            if ("S_ONLINE_COUNT".equals(msg.getType())) {
                Platform.runLater(() -> lblOnlinePlayers.setText(msg.getPayload().toString()));
            }
        });
    }

    @FXML private void openPractice()     { show("Luyện tập: 20 từ, 3 mức độ!"); }
    @FXML private void openChallenge()   { show("Thách đấu: Tìm đối thủ 1vs1!"); }
    @FXML private void openLeaderboard() { show("Top 100 cao thủ toàn server!"); }

    @FXML
    private void logout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Đăng xuất ngay?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                networkClient.sendMessage(new Message("C_LOGOUT", null));
                System.exit(0);
            }
        });
    }

    private void show(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg + "\n\nSắp ra mắt trong 24h!", ButtonType.OK);
        a.setHeaderText("Tính năng HOT!");
        a.setTitle("MindFlow Arena");
        a.show();
    }
}