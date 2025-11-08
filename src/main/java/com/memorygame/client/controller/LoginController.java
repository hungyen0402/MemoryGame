package com.memorygame.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Button btnLogin;

    public void setupController() {

    }

    @FXML
    private void handleLogin() {

    }

    // NetworkClient gọi phương thức này để chuyển qua main menu sau khi đăng nhập thành công
    private void onLoginResult(boolean success, String message) {

    }

    @FXML
    private void handleRegister() {

    }

    // Hiển thị popup thông báo lỗi
    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
