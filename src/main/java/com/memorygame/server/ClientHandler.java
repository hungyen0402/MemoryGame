package com.memorygame.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Map;

import com.memorygame.common.GameSession;
import com.memorygame.common.Message;
import com.memorygame.common.Player;

public class ClientHandler implements Runnable {
    private Player player;
    private final Socket socket;
    private ObjectOutputStream oos; 
    private ObjectInputStream ois; 
    private Server server = Server.getInstance(); 
    private PlayerDAO playerDAO = server.getPlayerDAO(); 

    private ClientHandler() {
        this.player = null;
        this.socket = null;
    }

    public ClientHandler(Socket socket) {
        this.socket = socket; 
        this.player = null;
        try {
            this.oos = new ObjectOutputStream(socket.getOutputStream()); 
            this.ois = new ObjectInputStream(socket.getInputStream()); 
        } catch (IOException e) {
            e.printStackTrace(); 
        }
    }

    @Override
    public void run() {
        try {
            Message clientMessage; 
            while ((clientMessage = (Message) ois.readObject()) != null) {
                processMessage(clientMessage); 
            }
        } catch (SocketException e) {
            System.out.println("Client da ngat ket noi: " + socket.getInetAddress()); 
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            server.handleLogout(this);
            try {
                if (ois != null) ois.close();
                if (oos != null) oos.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void sendMessage(Message message) {
        try {
            oos.writeObject(message);
            oos.flush(); 
        } catch (IOException e) {
            e.printStackTrace(); 
        }
    }

    public void setPlayer(Player player) {
        this.player = player; 
    }

    public void processMessage(Message message) {
        String type = message.getType(); 
        
        if (this.player == null && !type.equals("C_LOGIN") && !type.equals("C_REGISTER")) {
             System.err.println("Nhan duoc message " + type + " tu client chua dang nhap.");
             return;
        }
        
        switch (type) {
            case "C_LOGIN" -> {
                try {
                    String[] credentials = (String[]) message.getPayload();
                    String username = credentials[0];
                    String password = credentials[1];
                    
                    boolean login_success = server.handleLogin(username, password, this);

                    Object[] loginResponse = {login_success, this.player};
                    Message message1 = new Message("S_LOGIN_RESPONSE", loginResponse);
                    sendMessage(message1); 
                } catch (Exception e) {
                    sendMessage(new Message("LOGIN_FAIL", "Lỗi dữ liệu đăng nhập")); 
                }
            }
            
            case "C_LOGOUT" -> {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            case "INVITE" -> {
                if (this.player != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> invitePayload = (Map<String, Object>) message.getPayload(); 
                    boolean success_invite = server.handleInvite(this.player, invitePayload); 
                    if (success_invite) {
                        System.out.println(this.player.getUsername() + " GUI LOI MOI THANH CONG");
                    } else {
                        System.out.println(this.player.getUsername() + " GUI LOI MOI THAT BAI");
                    }
                }
                break; 
            }
            
            case "C_SQL_PLAYER" -> {
                List<Player> onlinePlayers = playerDAO.getOnlinePlayersForLobby(this.player.getId());
                Message message2 = new Message("S_ONLINE_LIST", onlinePlayers); 
                sendMessage(message2);
            }
            
            case "C_ONLINE_COUNT" -> {
                int count = playerDAO.countPlayerOnline(); 
                Message message3 = new Message("S_ONLINE_COUNT", count); 
                sendMessage(message3); 
            }

            case "C_WIN_COUNT" -> {
                Player player = playerDAO.getPlayerByUsername(this.player.getUsername());
                Message message5 = new Message("S_WIN_COUNT", player);
                sendMessage(message5);
            }
            
            case "C_GET_LEADERBOARD" -> {
                List<Player> leaderboard = playerDAO.getLeaderBoard();
                Message message4 = new Message("S_LEADERBOARD_DATA", leaderboard);
                sendMessage(message4);
            }
            
            case "C_REGISTER" -> {
                try {
                    String[] credentials = (String[]) message.getPayload(); 
                    String username = credentials[0]; 
                    String password = credentials[1]; 

                    boolean success = server.handleRegister(username, password); 

                    if (success) {
                        sendMessage(new Message("S_REGISTER_SUCCESS", null)); 
                    } else {
                        sendMessage(new Message("S_REGISTER_FAIL", "Tên người dùng đã tồn tại.")); 
                    }
                } catch (Exception e) {
                    sendMessage(new Message("S_REGISTER_FAIL", "Lỗi dữ liệu đăng ký")); 
                }
            }
            
            case "C_START_PRACTICE" -> {
                System.out.println("Nhan duoc C_START_PRACTICE tu " + this.player.getUsername()); 
                @SuppressWarnings("unchecked")
                Map<String, Object> settings = (Map<String, Object>) message.getPayload();  
                server.handleStartPractice(this.player, settings);
                break; 
            }
            
            // ✅ SỬA: Xử lý Submit Answer
            case "C_SUBMIT_ANSWER" -> {
                Map<String, Object> answerPayload = (Map<String, Object>) message.getPayload();
                GameSession session = server.getSessionForPlayer(this.player);
                
                if (session != null) {
                    if (session.isPractice()) {
                        // Practice: 1 người chơi
                        session.submitAnswer(answerPayload);
                    } else {
                        // Challenge: 2 người chơi
                        session.submitAnswerChallenge(this.player, answerPayload);
                    }
                } else {
                    System.err.println("Nhan duoc C_SUBMIT_ANSWER tu " + player.getUsername() + " nhung khong tim thay session!");
                }
                break; 
            }
            
            case "C_LEAVE_GAME" -> {
                System.out.println("Nhan duoc C_LEAVE_GAME tu " + player.getUsername()); 
                server.handleLeaveGame(this.player);
                break; 
            }
            
            case "C_ACCEPT_INVITE" -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) message.getPayload();
                server.handleAcceptInvite(this.player, data);
            }
            
            case "C_DECLINE_INVITE" -> {
                String inviterUsername = (String) message.getPayload();
                server.handleDeclineInvite(this.player, inviterUsername);
            }
        }
    }
}