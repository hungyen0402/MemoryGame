package com.memorygame.client;

import com.memorygame.client.controller.*; // Import TẤT CẢ các controller của bạn
import com.memorygame.common.Message;
import com.memorygame.common.Player;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class SceneManager implements NetworkClient.MessageListener {

    private Stage primaryStage;
    private NetworkClient networkClient;

    // Lưu lại controller đang hiển thị
    private Object currentController;

    public SceneManager(Stage primaryStage, NetworkClient networkClient) {
        this.primaryStage = primaryStage;
        this.networkClient = networkClient;

        // Đăng ký CHÍNH NÓ làm listener duy nhất
        // SceneManager sẽ nhận và xử lý TẤT CẢ tin nhắn từ server gửi sang
        this.networkClient.setMessageListener(this);
    }

    private Object loadAndShowScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            // Lấy controller hiện tại
            this.currentController = loader.getController();
            
            // Truyền 
            if (currentController instanceof LoginController c) {
                c.setupController(this, networkClient);
            } else if (currentController instanceof MainMenuController c) {
                c.setupController(this, networkClient);
            } else if (currentController instanceof LobbyController c) {
                c.setupController(this, networkClient);
            } else if (currentController instanceof LeaderboardController c) {
                c.setupController(this, networkClient);
            } else if (currentController instanceof PracticeSettingsController c) {
                c.setupController(this); 
            } else if (currentController instanceof PracticeGameController c) {
                c.setupController(this);
            } else if (currentController instanceof ChallengeConfigController c) {
                c.setupController(this, networkClient);
            } else if (currentController instanceof ChallengeGameController c) {
                c.setupController(this, networkClient);
            }

            // Hiển thị scene mới
            Scene scene = new Scene(root, 1000, 700);
            primaryStage.setScene(scene);
            primaryStage.setResizable(false); 
            primaryStage.setTitle("MindFlow Arena");
            primaryStage.centerOnScreen();
            primaryStage.show();

            return this.currentController;

        } catch (IOException e) {
            System.err.println("Lỗi nghiêm trọng: Không thể tải file FXML: " + fxmlFile);
            e.printStackTrace();
            return null;
        }
    }

    public void showLoginScene() {
        loadAndShowScene("/fxml/LoginScene.fxml");
    }

    public void showMainMenuScene() {
        loadAndShowScene("/fxml/MainMenuScene.fxml");
    }

    public void showLobbyScene() {
        loadAndShowScene("/fxml/LobbyScene.fxml");
    }

    public void showLeaderboardScene() {
        loadAndShowScene("/fxml/LeaderboardScene.fxml");
    }
    
    public void showPracticeSettingsScene() {
        loadAndShowScene("/fxml/PracticeSettingsScene.fxml");
    }

    public void showChallengeConfigScene(Player opponent) {
        loadAndShowScene("/fxml/ChallengeConfigScene.fxml");
    }

    public void showPracticeGameScene(int thinkTime, int totalRounds, int waitTime) {
        PracticeGameController controller = (PracticeGameController) loadAndShowScene("/fxml/PracticeGameScene.fxml");
        if (controller != null) {
            // (Truyền cài đặt vào controller game)
            controller.setupGame(thinkTime, totalRounds, waitTime);
        }
    }
    
    public void showChallengeGameScene() {
        loadAndShowScene("/fxml/ChallengeGameScene.fxml");
    }
    
    @Override
    public void onMessageReceived(Message msg) {
        String type = msg.getType();
        Object payload = msg.getPayload();

        if (currentController instanceof LoginController c) {
            switch (type) {
                case "S_LOGIN_RESPONSE" -> {
                    Boolean success = (Boolean) payload;

                    if (Boolean.TRUE.equals(success)) {
                        c.onLoginSuccess();
                    } else {
                        c.onLoginFail("Sai tài khoản hoặc mật khẩu!");
                    }
                }
            }
        }
        
        else if (currentController instanceof MainMenuController c) {
            if (type.equals("S_ONLINE_LIST")) {
                c.updateOnlineList((List<Player>) payload);
            }
        }
        
        else if (currentController instanceof LobbyController c) {
            if (type.equals("S_ONLINE_LIST")) {
                //c.updateOnlineList((List<Player>) payload);
            }

        }
        
        else if (currentController instanceof LeaderboardController c) {
            if (type.equals("S_LEADERBOARD_DATA")) {
                c.onLeaderboardDataReceived((List<Player>) payload);
            }
        }
        
        else if (currentController instanceof ChallengeGameController c) {
            switch (type) {

            }
        }
        
        // (Các màn hình Practice không cần nhận tin nhắn)
    }
}