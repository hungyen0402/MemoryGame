package com.memorygame.client;

import java.io.IOException; // Import TẤT CẢ các controller của bạn
import java.util.List;
import java.util.Map;

import com.memorygame.client.controller.ChallengeConfigController;
import com.memorygame.client.controller.ChallengeGameController;
import com.memorygame.client.controller.ChallengeInviteDialogController;
import com.memorygame.client.controller.ChallengeResultController;
import com.memorygame.client.controller.LeaderboardController;
import com.memorygame.client.controller.LobbyController;
import com.memorygame.client.controller.LoginController;
import com.memorygame.client.controller.MainMenuController;
import com.memorygame.client.controller.PracticeGameController;
import com.memorygame.client.controller.PracticeSettingsController;
import com.memorygame.client.controller.RegisterController; 
import com.memorygame.common.Message;
import com.memorygame.common.Player;

import javafx.application.Platform;
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
    public void showChallengeConfigScene(Player opponent) {
        ChallengeConfigController controller = (ChallengeConfigController) loadAndShowScene("/fxml/ChallengeConfigScene.fxml");
        
        if (controller != null) {
            controller.setupController(this, networkClient, opponent);
        }
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
            primaryStage.show();

            return this.currentController;

        } catch (IOException e) {
            System.err.println("Lỗi nghiêm trọng: Không thể tải file FXML: " + fxmlFile);
            e.printStackTrace();
            return null;
        }
    }
    public void showChallengeResultScene(Map<String, Object> resultData) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ChallengeResultScene.fxml"));
            Parent root = loader.load();
            ChallengeResultController controller = loader.getController();

            this.currentController = controller;
            controller.setupController(this, networkClient);
            
            // Truyền dữ liệu kết quả
            String winnerUsername = (String) resultData.get("winnerUsername");
            int yourScore = (int) resultData.get("yourScore");
            String opponentUsername = (String) resultData.get("opponentUsername");
            int opponentScore = (int) resultData.get("opponentScore");
            Player opponent = (Player) resultData.get("opponentPlayer");
            
            controller.showResult(winnerUsername, yourScore, opponent, opponentScore);

            Scene scene = new Scene(root, 1000, 700);
            primaryStage.setScene(scene);
            primaryStage.setTitle("MindFlow Arena - Kết Quả");
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không thể tải giao diện kết quả!");
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

    public void showChallengeGameScene(Map<String, Object> gameInfo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ChallengeGameScene.fxml"));
            Parent root = loader.load();
            ChallengeGameController controller = loader.getController();

            this.currentController = controller;
            controller.setupController(this, networkClient);
            controller.setupGameInfo(gameInfo);

            Scene scene = new Scene(root, 1000, 700);
            primaryStage.setScene(scene);
            primaryStage.setTitle("MindFlow Arena - Thách Đấu");
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không thể tải giao diện trận đấu!");
        }
    }

    public void showPracticeGameScene(long thinkTime, int totalRounds, long waitTime) {
        PracticeGameController controller = (PracticeGameController) loadAndShowScene("/fxml/PracticeGameScene.fxml");
        if (controller != null) {
            controller.setupGameInfo(totalRounds, thinkTime);
        }
    }
    


    public void showRegisterScene() {
        loadAndShowScene("/fxml/RegisterScene.fxml"); 
    }
    @Override
    public void onMessageReceived(Message msg) {
        String type = msg.getType();
        Object payload = msg.getPayload();
        if (type.equals("S_RECEIVE_INVITE")) {
            // Chỉ hiện nếu KHÔNG đang trong trận
            if (!(currentController instanceof ChallengeGameController)) {
                showInviteDialog((Map<String, Object>) payload);
            }
            return; // Dừng xử lý tiếp
        }else if (type.equals("S_CHALLENGE_START")) {
            showChallengeGameScene((Map<String, Object>) payload);
            return;
        }else if (currentController instanceof LoginController c) {
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
                c.updateOnlineCount((int) payload);
            } else if (type.equals("S_WIN_COUNT")) {
                c.updatePlayerStats((Player) payload);
            }
        }
        
        else if (currentController instanceof LobbyController c) {
            if (type.equals("S_ONLINE_LIST")) {
                c.updateOnlineList((List<Player>) payload);
            }
            else if (type.equals("S_INVITE_SEND")) {
                c.showInviteStatusAlert("Đã Gửi Lời Mời", (String) payload, AlertType.INFORMATION);
            }
            else if (type.equals("S_INVITE_FAIL")) {
                c.showInviteStatusAlert("Gửi Lời Mời Thất Bại", (String) payload, AlertType.ERROR);
            }
            // THÊM DÒNG NÀY:
            else if (type.equals("S_RECEIVE_INVITE")) {
                showInviteDialog((Map<String, Object>) payload);
            } else if (type.equals("S_PLAYER_LOGGED_OUT")) {
                c.removePlayerFromList((Player) payload);
            }
        }
        
        else if (currentController instanceof LeaderboardController c) {
            if (type.equals("S_LEADERBOARD_DATA")) {
                c.onLeaderboardDataReceived((List<Player>) payload);
            }
        }
        
        else if (currentController instanceof ChallengeGameController c) {
            switch (type) {
                case "S_CHALLENGE_START" -> {
                    
                }
                case "S_NEW_ROUND_CHALLENGE" -> {
                    Object[] data = (Object[]) payload;
                    c.onNewRound((String) data[0], (int) data[1], (int) data[2]);
                }
                case "S_ANSWER_PHASE_CHALLENGE" -> {
                    c.onAnswerPhase((int) payload);
                }
                case "S_SCORE_UPDATE_CHALLENGE" -> {
                    // Payload: Integer newScore
                    Object[] data = (Object[]) payload;
                    c.onScoreUpdate((int) data[0], (int) data[1]);
                }
                case "S_CHALLENGE_END" -> {
                    c.stopGameTimer();
                    showChallengeResultScene((Map<String, Object>) payload);
                    System.out.println("KET THUC GAMESESSION");
                    return;
                }
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
                case "S_NEW_ROUND_PRACTICE" -> {
                    // Payload: Object[] {word, round, memorizeTime}
                    Object[] data = (Object[]) payload;
                    c.onNewRound((String) data[0], (int) data[1], (int) data[2]);
                }
                case "S_ANSWER_PHASE_PRACTICE" -> {
                    // Payload: Integer answerTime
                    c.onAnswerPhase((int) payload);
                }
                case "S_SCORE_UPDATE_PRACTICE" -> {
                    // Payload: Integer newScore
                    c.onScoreUpdate((int) payload);
                }
                case "S_GAME_OVER" -> {
                    // Payload: Integer finalScore
                    c.onGameOver((int) payload);
                }
                // case "S_CHALLENGE_END" -> {
                //     showChallengeResultScene((Map<String, Object>) payload);
                //     return;
                // }
            }
        }else if (type.equals("S_RECEIVE_INVITE")) {
            showInviteDialog((Map<String, Object>) payload);
        }
        else if (type.equals("S_INVITE_DECLINED")) {
            if (currentController instanceof LobbyController c) {
                c.showInviteStatusAlert("Từ chối", (String) payload, AlertType.WARNING);
            }
        }
    }
    // Thêm method
    public void showInviteDialog(Map<String, Object> inviteData) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ChallengeInviteDialog.fxml"));
                Parent root = loader.load();
                ChallengeInviteDialogController controller = loader.getController();

                Stage dialogStage = new Stage();
                dialogStage.setTitle("Lời mời thách đấu");
                dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                dialogStage.setResizable(false);
                dialogStage.setScene(new Scene(root));

                controller.setup(this, networkClient, inviteData, dialogStage);
                dialogStage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    
    
}