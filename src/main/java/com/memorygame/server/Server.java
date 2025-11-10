package com.memorygame.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mindrot.jbcrypt.BCrypt;

import com.memorygame.common.GameSession;
import com.memorygame.common.Message;
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
    // Tạo clientHandler instance, chưa biết player sở hữu 
    public void startServer() {
        System.out.println("SERVER dang khoi dong " + PORT + "..."); 
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while(true) {
                try {
                    Socket clientSocket = serverSocket.accept(); 
                    System.out.println("Mot client moi da ket noi: " + clientSocket.getInetAddress()); 

                    // Tạo 1 clienthandler mới để xứ lý client này 
                    ClientHandler clientHandler = new ClientHandler(clientSocket); 

                    // bắt đầu clientHandler trên 1 thread mới
                    new Thread(clientHandler).start(); 
                    System.out.println("client da ket noi: " + clientSocket.getInetAddress());
                } catch (IOException e) {
                    System.err.println("Loi khi chap nhan ket noi Client: " + e.getMessage()); 
                }
            }
        } catch (IOException e) {
            e.printStackTrace(); 
            System.err.println("LOI NGHIEM TRONG: Khong the khoi dong ServerSocker."); 
        }
    }

    public synchronized boolean handleLogin(String userName, String password, ClientHandler client) {
        Player player = playerDAO.getPlayerByUsername(userName); 
        if (player != null) {
            // Kiểm tra mật khẩu
            if (password.equals(player.getPasswordHash()) || BCrypt.checkpw(password, player.getPasswordHash())) {
                System.out.println("DANG NHAP THANH CONG CHO USER: " + userName);
                
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
                broadcastOnlineList(player); 

                return true; 
            }
            System.out.println("User nhap sai mat khau"); 
        }
        System.out.println("Dang nhap that bai do user: " + userName + " Do nhap sai userName");
        // Gửi message thông báo Login thất bại tới client 
        // client.sendMessage(new Message("LOGIN_FAIL", "SAI NAME HOẶC MẬT KHẨU"));
        return false;  
    }

    public synchronized boolean handleRegister(String username, String password) {
        // Kiểm tra xem tên đã tồn tại chưa
        if (playerDAO.getPlayerByUsername(username) != null) {
            System.out.println("DANG KY THAT BAI: TEN " + username + " DA TON TAI"); 
            return false; // Tên đã tồn tại 
        }

        // Nếu tên chưa tồn tại, gọi DAO để tạo mới
        boolean success = playerDAO.createPlayer(username, password);

        if (success) {
            System.out.println("DANG KY THANH CONG CHO USER: " + username); 
        } else {
            System.out.println("DANG KY THAT BAI: LOI CSDL CHO USER: " + username); 
        }
        return success; 
    }

    public synchronized boolean handleInvite(Player inviter, Map<String, Object> invitePayload) {
        String opponentUsername = (String) invitePayload.get("opponentUsername"); 
        if (opponentUsername == null) {
            System.err.println("LOI INVITE: Payload khong co opponentUsername"); 
            return false;
        }
        // Tìm clienthandler của đối thủ
        ClientHandler opponentHandler = onlinePlayers.get("opponentUsername");
        // Lấy Opponent Player object 
        Player opponent = (opponentHandler != null) ? handlerToPlayer.get(opponentHandler) : null;
        // Tìm clientHandler của người mời 
        ClientHandler inviterHandler = onlinePlayers.get(inviter.getUsername()); 
        if (opponentHandler != null && opponent != null && opponent.getStatus() == PlayerStatus.ONLINE) {
            Map<String, Object> opponentPayload = new HashMap<>(invitePayload);
            opponentPayload.put("inviteUsername", inviter.getUsername()); 
            opponentPayload.remove("opponentUsername"); 
            // Gửi lời mời tới đối thủ
            opponentHandler.sendMessage(new Message("S_RECEIVE_INVITE", opponentPayload));
            // Báo cho người mời là đã gửi thành công
            if (inviterHandler != null) {
                inviterHandler.sendMessage(new Message("S_INVITE_SEND", "Đã gửi lời mời. Đang chờ đối thủ...."));
                return true; 
            }
        } else {
            if (inviterHandler != null) {
                String reason = (opponent == null) ? "Người chơi không tồn tại hoặc đã offline." : "Người chơi đang bận."; 
                inviterHandler.sendMessage(new Message("S_INVITE_FAIL", reason));
            }
            return false;
        }
        return true; 
    }

    public void createGameSession(Player player1, Player player2) {
        
    }
    // Gửi message 
    public void broadcastOnlineList(Player currentPlayer) {
        System.out.println("Dang cap nhap va gui danh sach online ...."); 

        List<Player> onlinePlayerList = playerDAO.getOnlinePlayersForLobby(currentPlayer.getId());
        Message message = new Message("S_ONLINE_LIST", onlinePlayerList); 
        for (ClientHandler handler : handlerToPlayer.keySet()) {
            handler.sendMessage(message);
        }
    }

    public void handleLogout(ClientHandler client) {
        Player player = handlerToPlayer.get(client);
        if (player != null) {
            System.out.println("logout: " + player.getUsername());
            playerDAO.updatePlayerStatus(player.getId(), PlayerStatus.OFFLINE);;
            onlinePlayers.remove(player.getUsername());
            handlerToPlayer.remove(client);
            broadcastOnlineList(player);
        }
        else{
            System.out.println("Client chua dang nhap da ngat ket noi, chi dong socket.");
            handlerToPlayer.remove(client);
        }
    }
    // Get playerDAO
    public PlayerDAO getPlayerDAO() {
        return this.playerDAO; 
    }
}