package com.memorygame.client;

import javafx.application.Application;
import javafx.stage.Stage;

public class ClientApp extends Application {

    private SceneManager sceneManager;
    private NetworkClient networkClient;

    @Override
    public void start(Stage primaryStage) {
        try {

            this.networkClient = NetworkClient.getInstance();
            this.sceneManager = new SceneManager(primaryStage, networkClient);
 
            sceneManager.showLoginScene();

            primaryStage.setOnCloseRequest(event -> {
                networkClient.disconnect();
                System.exit(0);
            });

        } catch (Exception e) {
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
        launch(args);
    }
}