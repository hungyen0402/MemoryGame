package com.memorygame.client;

import java.io.IOException; // Import TẤT CẢ các controller của bạn
import java.util.List;

import com.memorygame.client.controller.ChallengeConfigController;
import com.memorygame.client.controller.ChallengeGameController;
import com.memorygame.client.controller.LeaderboardController;
import com.memorygame.client.controller.LobbyController;
import com.memorygame.client.controller.LoginController;
import com.memorygame.client.controller.MainMenuController;
import com.memorygame.client.controller.PracticeGameController;
import com.memorygame.client.controller.PracticeSettingsController;
import com.memorygame.common.Message;
import com.memorygame.common.Player;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

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
                c.setupController(this, networkClient); 
            } else if (currentController instanceof PracticeGameController c) {
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
        ChallengeConfigController controller = (ChallengeConfigController) loadAndShowScene("/fxml/ChallengeConfigScene.fxml");
        
        if (controller != null) {
            controller.setupController(this, networkClient, opponent);
        }
    }

    public void showPracticeGameScene(int thinkTime, int totalRounds, int waitTime) {
        loadAndShowScene("/fxml/PracticeGameScene.fxml");
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
                    Object[] response = (Object[]) payload;
                    Boolean success = (Boolean) response[0];
                    Player player = (Player) response[1];
                    ClientState.getInstance().setCurrentPlayer(player);

                    if (Boolean.TRUE.equals(success)) {
                        c.onLoginSuccess();
                    } else {
                        c.onLoginFail("Sai tài khoản hoặc mật khẩu!");
                    }
                }
            }
        }
        
        else if (currentController instanceof MainMenuController c) {
            if (type.equals("S_ONLINE_COUNT")) {
                c.updateOnlineCount((int) payload); // phải sửa lại 
            }
        }
        
        else if (currentController instanceof LobbyController c) {
            if (type.equals("S_ONLINE_LIST")) {
                c.updateOnlineList((List<Player>) payload);
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