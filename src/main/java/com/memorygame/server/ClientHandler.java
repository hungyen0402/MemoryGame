package com.memorygame.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Map;

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
        this.player = null; // player là null cho đến khi đăng nhập 
        try {
            this.oos = new ObjectOutputStream(socket.getOutputStream()); 
            this.ois = new ObjectInputStream(socket.getInputStream()); 
        } catch (IOException e) {
            e.printStackTrace(); 
        }
    }
    // nhận message rồi gọi hàm processMessage để xử lý message (listerner function)
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
    // Hàm này dùng khi user đăng nhập thành công, 
    //Server sẽ gọi hàm này để gán player cho Clienthandler
    public void setPlayer(Player player) {
        this.player = player; 
    }
    // HÀM XỬ LÝ CÁC MESSAGE NHẬN ĐƯỢC 
    public void processMessage(Message message) {
        String type = message.getType(); 
        
        switch (type) {
            case "C_LOGIN" -> {
                // Lúc này là user đang đăng nhập,
                // Client gửi 1 mảng String[] {username, password}
                try {
                    String[] credentials = (String[]) message.getPayload();
                    String username = credentials[0];
                    String password = credentials[1];
                    
                    boolean login_success = server.handleLogin(username, password, this);

                    Object[] loginResponse = {login_success, this.player};
                    Message message1 = new Message("S_LOGIN_RESPONSE", loginResponse);
                    sendMessage(message1); 
                } catch (Exception e) {
                    sendMessage(new Message("LOGIN_FAIL", "Lỗi dữ liệu đăng nhập (dữ liệu từ user gửi)")); 
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
                    // Chuyển yêu cầu mời cho server
                    boolean success_invite = server.handleInvite(this.player, invitePayload); 
                    if (success_invite) {
                        System.out.println(this.player.getUsername() + " GUI LOI MOI THACH DAU THANH CONG TOI " + invitePayload.get("opponentUsername"));
                    } else {
                        System.out.println(this.player.getUsername() + " GUI LOI MOI THACH DAU KHONG THANH CONG TOI " + invitePayload.get("opponentUsername"));
                    }
                }
                break; 
            }
            case "SUBMIT_ANSWER" -> {
                // làm sau
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
                    sendMessage(new Message("S_REGISTER_FAIL", "Lỗi dữ liệu đăng ký không hợp lệ")); 
                }
            }
        }
    }
}