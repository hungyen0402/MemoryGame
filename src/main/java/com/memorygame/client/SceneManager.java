package com.memorygame.client;

import java.io.IOException; // Import TẤT CẢ các controller của bạn
import java.util.List;
import java.util.Map;

import com.memorygame.client.controller.ChallengeConfigController;
import com.memorygame.client.controller.ChallengeGameController;
import com.memorygame.client.controller.LeaderboardController;
import com.memorygame.client.controller.LobbyController;
import com.memorygame.client.controller.LoginController;
import com.memorygame.client.controller.MainMenuController;
import com.memorygame.client.controller.PracticeGameController;
import com.memorygame.client.controller.PracticeSettingsController;
import com.memorygame.client.controller.RegisterController; 
import com.memorygame.common.Message;
import com.memorygame.common.Player;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

public class SceneManager implements NetworkClient.MessageListener {

    private Stage primaryStage;
    private NetworkClient networkClient;
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
            } else if (currentController instanceof RegisterController c) {
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
        PracticeGameController controller = (PracticeGameController) loadAndShowScene("/fxml/PracticeGameScene.fxml");
        if (controller != null) {
            controller.setupGameInfo(totalRounds, thinkTime);
        }
    }
    
    public void showChallengeGameScene() {
        loadAndShowScene("/fxml/ChallengeGameScene.fxml");
    }
    
    public void showRegisterScene() {
        loadAndShowScene("/fxml/RegisterScene.fxml"); 
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
            else if (type.equals("S_INVITE_SEND")) {
                // payload là một String thông báo thành công
                c.showInviteStatusAlert("Đã Gửi Lời Mời", (String) payload, AlertType.INFORMATION);
            }
            else if (type.equals("S_INVITE_FAIL")) {
                // payload là một String lý do thất bại
                c.showInviteStatusAlert("Gửi Lời Mời Thất Bại", (String) payload, AlertType.ERROR);
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
        
        else if (currentController instanceof RegisterController c) {
            switch (type) {
                case "S_REGISTER_SUCCESS" -> {
                    c.onRegisterSuccess();
                }
                case "S_REGISTER_FAIL" -> {
                    c.onRegisterFail((String) (payload));
                }
            }
        }
        else if (currentController instanceof PracticeSettingsController c) {
            switch (type) {
                case "S_PRACTICE_START" -> { // Server báo game bắt đầu
                    @SuppressWarnings("unchecked")
                    Map<String, Object> settings = (Map<String, Object>) payload;
                    long thinkTime = (long) settings.get("thinkTime");
                    int totalRounds = (int) settings.get("totalRounds");
                    long waitTime = (long) settings.get("waitTime");
                    
                    // Chuyển sang màn hình chơi game
                    showPracticeGameScene(thinkTime, totalRounds, waitTime);
                }
            }
        }
        // 8. XỬ LÝ CHO PRACTICE GAME (Màn hình chơi game) <-- PHẦN THIẾU
        else if (currentController instanceof PracticeGameController c) {
             switch (type) {
                case "S_NEW_ROUND" -> {
                    // Payload: Object[] {word, round, memorizeTime}
                    Object[] data = (Object[]) payload;
                    c.onNewRound((String) data[0], (int) data[1], (int) data[2]);
                }
                case "S_ANSWER_PHASE" -> {
                    // Payload: Integer answerTime
                    c.onAnswerPhase((int) payload);
                }
                case "S_SCORE_UPDATE" -> {
                    // Payload: Integer newScore
                    c.onScoreUpdate((int) payload);
                }
                case "S_GAME_OVER" -> {
                    // Payload: Integer finalScore
                    c.onGameOver((int) payload);
                }
            }
        }
    }
}