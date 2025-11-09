package com.memorygame.client.controller;

import com.memorygame.client.NetworkClient;
import com.memorygame.client.SceneManager;
import com.memorygame.common.Message;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;

    private NetworkClient networkClient;
    private SceneManager sceneManager;
    
    public void setupController(SceneManager sceneManager, NetworkClient networkClient) {
        this.sceneManager = sceneManager;
        this.networkClient = networkClient;

        // Kết nối với server ngay khi vào màn hình
        if (!networkClient.isConnected()) {
            networkClient.connect();
        }
    }
    
    @FXML
    private void handleRegister() {
        sceneManager.showRegisterScene();
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Vui lòng nhập đầy đủ!");
            return;
        }

        // Kiểm tra kết nối
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

    public void onLoginSuccess() {
        sceneManager.showMainMenuScene();
    }

    public void onLoginFail(String reason) {
        resetLoginButton();
        showAlert(reason); 
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
}