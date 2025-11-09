package com.memorygame.client.controller;

import com.memorygame.client.NetworkClient;
import com.memorygame.client.SceneManager;
import com.memorygame.common.Message;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Button btnRegister;

    private NetworkClient networkClient;
    private SceneManager sceneManager;
    
    public void setupController(SceneManager sceneManager, NetworkClient networkClient) {
        this.sceneManager = sceneManager;
        this.networkClient = networkClient;

        // Đảm bảo đã kết nối
        if (!networkClient.isConnected()) {
            networkClient.connect();
        }
    }
    
    @FXML
    private void handleBackToLogin() {
        sceneManager.showLoginScene();
    }

    @FXML
    private void handleRegister() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showAlert(AlertType.WARNING, "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert(AlertType.WARNING, "Mật khẩu xác nhận không khớp!");
            return;
        }

        // TODO: Thêm các kiểm tra độ dài, ký tự đặc biệt...
        
        // Kiểm tra kết nối
        if (!networkClient.isConnected() && !networkClient.connect()) {
            showAlert(AlertType.ERROR, "Không thể kết nối đến server!");
            return;
        }

        // Gửi message đăng ký
        String[] credentials = { username, password }; // Bạn có thể cần gửi password đã hash
        networkClient.sendMessage(new Message("C_REGISTER", credentials));

        btnRegister.setDisable(true);
        btnRegister.setText("Đang xử lý...");
    }

    /**
     * Được gọi bởi SceneManager khi server phản hồi đăng ký thành công
     */
    public void onRegisterSuccess() {
        Platform.runLater(() -> {
            showAlert(AlertType.INFORMATION, "Đăng ký thành công! Vui lòng đăng nhập.");
            sceneManager.showLoginScene();
        });
    }

    /**
     * Được gọi bởi SceneManager khi server phản hồi đăng ký thất bại
     * @param reason Lý do thất bại (ví dụ: Tên đã tồn tại)
     */
    public void onRegisterFail(String reason) {
        Platform.runLater(() -> {
            resetRegisterButton();
            showAlert(AlertType.ERROR, reason);
        });
    }

    private void resetRegisterButton() {
        btnRegister.setDisable(false);
        btnRegister.setText("Đăng ký");
    }

    private void showAlert(AlertType type, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.show();
    }
}