package com.memorygame.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
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
    private VocabularyDAO vocabularyDAO;
    private Server() {
        this.onlinePlayers = new ConcurrentHashMap<>();
        this.handlerToPlayer = new ConcurrentHashMap<>();
        this.playerToSession = new ConcurrentHashMap<>();
        this.playerDAO = new PlayerDAO(); 
        this.vocabularyDAO = new VocabularyDAO();
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
                broadcastOnlineCount(); 

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

    private Map<Player, Map<String, Object>> pendingInvites = new ConcurrentHashMap<>();

    public synchronized boolean handleInvite(Player inviter, Map<String, Object> invitePayload) {
        String opponentUsername = (String) invitePayload.get("opponentUsername");
        ClientHandler opponentHandler = onlinePlayers.get(opponentUsername);
        Player opponent = (opponentHandler != null) ? handlerToPlayer.get(opponentHandler) : null;
        ClientHandler inviterHandler = onlinePlayers.get(inviter.getUsername());

        if (opponent == null || opponentHandler == null || opponent.getStatus() != PlayerStatus.ONLINE) {
            if (inviterHandler != null) {
                inviterHandler.sendMessage(new Message("S_INVITE_FAIL", "Đối thủ không online hoặc đang bận."));
            }
            return false;
        }

        // Lưu lời mời tạm
        pendingInvites.put(opponent, invitePayload);

        // Gửi popup cho người nhận
        Map<String, Object> receivePayload = new HashMap<>(invitePayload);
        receivePayload.put("inviterUsername", inviter.getUsername());
        receivePayload.put("inviterWins", inviter.getTotalWins());
        receivePayload.remove("opponentUsername");

        opponentHandler.sendMessage(new Message("S_RECEIVE_INVITE", receivePayload));

        // Báo cho người gửi
        if (inviterHandler != null) {
            inviterHandler.sendMessage(new Message("S_INVITE_SEND", "Đã gửi lời mời đến " + opponentUsername + "..."));
        }
        return true;
    }

    // Xử lý khi người nhận ĐỒNG Ý
    // Server.java
    public synchronized void handleAcceptInvite(Player acceptor, Map<String, Object> inviteData) {
        String inviterUsername = (String) inviteData.get("inviterUsername");
        
        // Tìm người mời
        ClientHandler inviterHandler = onlinePlayers.get(inviterUsername);
        Player inviter = (inviterHandler != null) ? handlerToPlayer.get(inviterHandler) : null;

        if (inviter == null) {
            // Người mời đã thoát
            acceptor.setStatus(PlayerStatus.ONLINE);
            playerDAO.updatePlayerStatus(acceptor.getId(), PlayerStatus.ONLINE);
            sendMessageToPlayer(acceptor, new Message("S_INVITE_FAIL", "Người mời đã thoát."));
            return;
        }

        // XÓA lời mời pending
        pendingInvites.remove(acceptor);

        // ✅ CẬP NHẬT TRẠNG THÁI CẢ 2 NGƯỜI
        inviter.setStatus(PlayerStatus.BUSY);
        acceptor.setStatus(PlayerStatus.BUSY);
        playerDAO.updatePlayerStatus(inviter.getId(), PlayerStatus.BUSY);
        playerDAO.updatePlayerStatus(acceptor.getId(), PlayerStatus.BUSY);

        // ✅ TẠO GAME SESSION
        GameSession session = new GameSession(inviter, acceptor, inviteData, this, false);
        session.start();
        playerToSession.put(inviter, session);
        playerToSession.put(acceptor, session);

        // ✅ GỬI S_CHALLENGE_START CHO CẢ 2 NGƯỜI
        // Map<String, Object> gameInfoForInviter = new HashMap<>(inviteData);
        // gameInfoForInviter.put("opponentUsername", acceptor.getUsername());
        // sendMessageToPlayer(inviter, new Message("S_CHALLENGE_START", gameInfoForInviter));

        // Map<String, Object> gameInfoForAcceptor = new HashMap<>(inviteData);
        // gameInfoForAcceptor.put("opponentUsername", inviter.getUsername());
        // sendMessageToPlayer(acceptor, new Message("S_CHALLENGE_START", gameInfoForAcceptor));

        System.out.println("✅ TRAN DAU BAT DAU: " + inviter.getUsername() + " vs " + acceptor.getUsername());

        // Cập nhật lobby cho người khác
        broadcastOnlineCount();
    }
    // Xử lý khi người nhận TỪ CHỐI
    public synchronized void handleDeclineInvite(Player decliner, String inviterUsername) {
        Player inviter = handlerToPlayer.values().stream()
                .filter(p -> p.getUsername().equals(inviterUsername))
                .findFirst().orElse(null);

        pendingInvites.remove(decliner);

        if (inviter != null) {
            ClientHandler handler = onlinePlayers.get(inviter.getUsername());
            if (handler != null) {
                handler.sendMessage(new Message("S_INVITE_DECLINED", decliner.getUsername() + " đã từ chối lời mời."));
            }
        }
    }

    public void createGameSession(Player player1, Player player2) {
        
    }
    // Gửi message 
    public void broadcastOnlineCount() {
        System.out.println("Dang cap nhap va gui danh sach online ...."); 

        int count = playerDAO.countPlayerOnline();
        Message countMessage = new Message("S_ONLINE_COUNT", count);

        for (ClientHandler handler : handlerToPlayer.keySet()) {
            handler.sendMessage(countMessage);
        }
    }
// --- CÁC HÀM MỚI CHO LOGIC GAME ---

    /**
     * Bắt đầu game luyện tập cho 1 người chơi
     */
    public synchronized void handleStartPractice(Player player, Map<String, Object> settings) {
        if (player == null) return;
        
        // Cập nhật trạng thái người chơi
        player.setStatus(PlayerStatus.BUSY);
        playerDAO.updatePlayerStatus(player.getId(), PlayerStatus.BUSY);
        
        // Tạo game session mới
        GameSession newSession = new GameSession(player,null, settings, this, true);
        playerToSession.put(player, newSession);
        
        System.out.println(player.getUsername() + " bat dau luyen tap.");
        
        // Bắt đầu game (GameSession sẽ tự gửi message S_PRACTICE_START)
        newSession.start();
        
        // Cập nhật sảnh chờ cho người khác
        broadcastOnlineCount();
    }
    public synchronized void handleLeaveGame(Player player) {
        if (player == null) return;
        
        GameSession session = playerToSession.get(player);
        if (session != null) {
            if (session.isPractice()) {
                session.leaveGame(); // Sẽ tự gọi removePracticeSession
            } else {
                // TODO: Xử lý rời game thách đấu (báo cho người kia)
            }
        }
    }

    /**
     * Dọn dẹp session luyện tập (được gọi bởi GameSession)
     */
    public synchronized void removePracticeSession(Player player) {
        playerToSession.remove(player);
        
        // Cập nhật trạng thái người chơi
        player.setStatus(PlayerStatus.ONLINE);
        playerDAO.updatePlayerStatus(player.getId(), PlayerStatus.ONLINE);
        
        // Cập nhật sảnh chờ
        broadcastOnlineCount();
    }

    /**
     * Gửi tin nhắn đến một người chơi cụ thể
     */
    public void sendMessageToPlayer(Player player, Message message) {
        if (player != null) {
            ClientHandler handler = onlinePlayers.get(player.getUsername());
            if (handler != null) {
                handler.sendMessage(message);
            }
        }
    }
    public void handleLogout(ClientHandler client) {
        Player player = handlerToPlayer.get(client);
        if (player != null) {
            System.out.println("logout: " + player.getUsername());
            playerDAO.updatePlayerStatus(player.getId(), PlayerStatus.OFFLINE);;
            onlinePlayers.remove(player.getUsername());
            handlerToPlayer.remove(client);
            broadcastOnlineCount();

            Message removeMessage = new Message("S_PLAYER_LOGGED_OUT", player);
            for (ClientHandler handler : handlerToPlayer.keySet()) {
                handler.sendMessage(removeMessage);
            }
        }
        else{
            System.out.println("Client chua dang nhap da ngat ket noi, chi dong socket.");
            handlerToPlayer.remove(client);
        }
    }
    public synchronized void removeChallengeSession(Player player1, Player player2) {
        playerToSession.remove(player1);
        playerToSession.remove(player2);
        
        // Cập nhật trạng thái người chơi về ONLINE
        player1.setStatus(PlayerStatus.ONLINE);
        player2.setStatus(PlayerStatus.ONLINE);
        playerDAO.updatePlayerStatus(player1.getId(), PlayerStatus.ONLINE);
        playerDAO.updatePlayerStatus(player2.getId(), PlayerStatus.ONLINE);
        
        // Cập nhật sảnh chờ
        broadcastOnlineCount();
        
        System.out.println("Đã dọn dẹp session thách đấu: " + player1.getUsername() + " vs " + player2.getUsername());
    }
    // Get playerDAO
    public PlayerDAO getPlayerDAO() {
        return this.playerDAO; 
    }
    public VocabularyDAO getVocabularyDAO() {
        return this.vocabularyDAO;
    }

    public GameSession getSessionForPlayer(Player player) {
        return playerToSession.get(player);
    }
}