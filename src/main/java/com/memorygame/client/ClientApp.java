// src/main/java/com/memorygame/client/ClientApp.java
package com.memorygame.client;

import com.memorygame.client.controller.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // 1. Load LoginScene.fxml
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/LoginScene.fxml")
            );

            Scene scene = new Scene(loader.load(), 500, 720);
            
            // 3. Cấu hình Stage
            primaryStage.setScene(scene);
            primaryStage.setTitle("MindFlow Arena - Đăng nhập");
            primaryStage.setResizable(false);
            primaryStage.centerOnScreen();

            // 5. Lấy controller và truyền Stage
            LoginController controller = loader.getController();
            controller.setStage(primaryStage);

            // 6. Đóng kết nối mạng khi thoát ứng dụng
            primaryStage.setOnCloseRequest(event -> {
                controller.shutdown();
                System.exit(0);
            });

            // 7. Hiển thị
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showErrorAndExit("Không tải được giao diện đăng nhập!\n" + e.getMessage());
        }
    }

    private void showErrorAndExit(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle("Lỗi khởi động");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        System.exit(1);
    }

    public static void main(String[] args) {
        launch();
    }
}