package com.memorygame.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mindrot.jbcrypt.BCrypt;

import com.memorygame.common.GameSession;
import com.memorygame.common.Player;
import com.memorygame.common.PlayerStatus; 

public class Server {
    /**Server Singleton */
    private static final Server instance = new Server();

    private Map<String, ClientHandler> onlinePlayers;
    private Map<ClientHandler, Player> handlerToPlayer;
    private Map<Player, GameSession> playerToSession;

    private static final int PORT = 6789; 
    private PlayerDAO playerDAO; 

    private Server() {
        this.onlinePlayers = new ConcurrentHashMap<>();
        this.handlerToPlayer = new ConcurrentHashMap<>();
        this.playerToSession = new ConcurrentHashMap<>();
        this.playerDAO = new PlayerDAO(); 
    }

    public static Server getInstance() {
        return instance;
    }

    public void startServer() {
        System.out.println("SERVER đang khởi động tại cổng " + PORT + "..."); 
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while(true) {
                try {
                    Socket clientSocket = serverSocket.accept(); 
                    System.out.println("Một client mới đã kết nối: " + clientSocket.getInetAddress()); 

                    // Tạo 1 clienthandler mới để xứ lý client này 
                    ClientHandler clientHandler = new ClientHandler(clientSocket); 

                    // bắt đầu clientHandler trên 1 thread mới
                    new Thread(clientHandler).start(); 
                } catch (IOException e) {
                    System.err.println("Lỗi khi chấp nhận kết nối client: " + e.getMessage()); 
                }
            }
        } catch (IOException e) {
            e.printStackTrace(); 
            System.err.println("Lỗi nghiêm trọng: Không thể khởi động ServerSocker."); 
        }
    }

    public synchronized boolean handleLogin(String userName, String password, ClientHandler client) {
        Player player = playerDAO.getPlayerByUsername(userName); 

        if (player != null) {
            // Kiểm tra mật khẩu
            if (BCrypt.checkpw(password, player.getPasswordHash())) {
                System.out.println("Đăng nhập thành công cho user: " + userName);
                
                // Cập nhật status Player trong db 
                playerDAO.updatePlayerStatus(player.getId(), PlayerStatus.ONLINE);
                // Cập nhật status cho object player 
                player.setStatus(PlayerStatus.ONLINE);
                // Lưu trữ thông tin phiên làm việc 
                this.onlinePlayers.put(userName, client);
                this.handlerToPlayer.put(client, player); 
                // Gán player cho ClientHandler
                client.setPlayer(player);
                // Gửi message tới client thông báo đăng nhập thành công 
                // (cái này là việc của ClienHandler)
                // client.sendMessage(new Message("LOGIN_SUCCESS", player)); 
                // Thông báo tới các user khác
                broadcastOnlineList(); 

                return true; 
            }
            System.out.println("User nhập sai mật khẩu"); 
        }
        System.out.println("Đăng nhập thất bại cho user: " + userName);
        // Gửi message thông báo Login thất bại tới client 
        // client.sendMessage(new Message("LOGIN_FAIL", "SAI NAME HOẶC MẬT KHẨU"));
        return false;  
    }

    public void handleInvite(Player inviter, String inviteUsername) {

    }

    public void createGameSession(Player player1, Player player2) {
        
    }

    public void broadcastOnlineList() {
        
    }
    
}