// src/main/java/com/memorygame/client/controller/LoginController.java
package com.memorygame.client.controller;

import com.memorygame.client.NetworkClient;
import com.memorygame.common.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;

    private NetworkClient networkClient;
    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        networkClient = new NetworkClient();
        networkClient.setMessageListener(this::handleServerMessage);
    }
    @FXML
    private void handleRegister() {
        showAlert("Đăng ký tài khoản mới\n\nTính năng sẽ có trong bản 2.0!\nEmail: support@mindflow.vn");
    }
    @FXML
    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Vui lòng nhập đầy đủ!");
            return;
        }

        if (!networkClient.isConnected() && !networkClient.connect()) {
            showAlert("Không thể kết nối server!");
            return;
        }

        // Gửi message: type = "C_LOGIN", payload = [username, password]
        String[] credentials = { username, password };
        networkClient.sendMessage(new Message("C_LOGIN", credentials));

        btnLogin.setDisable(true);
        btnLogin.setText("Đang đăng nhập...");
    }

    private void handleServerMessage(Message msg) {
        Platform.runLater(() -> {
            if ("S_LOGIN_RESPONSE".equals(msg.getType())) {
                Boolean success = (Boolean) msg.getPayload();
                if (Boolean.TRUE.equals(success)) {
                    goToMainMenu();
                } else {
                    resetLoginButton();
                    showAlert("Sai tài khoản hoặc mật khẩu!");
                }
            }
        });
    }

    private void goToMainMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainMenuScene.fxml"));
            Scene scene = new Scene(loader.load(), 1000, 700);
            stage.setScene(scene);
            stage.setTitle("MindFlow Arena");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi tải menu!");
        }
    }

    private void resetLoginButton() {
        btnLogin.setDisable(false);
        btnLogin.setText("Đăng nhập");
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.show();
    }

    public void shutdown() {
        if (networkClient != null) networkClient.disconnect();
    }
}