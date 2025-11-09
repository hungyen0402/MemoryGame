package com.memorygame.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

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
            System.out.println("Client đã ngắt kết nối: " + socket.getInetAddress()); 
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            // Dọn dẹp: ĐÓNG SOCKET, XÓA KHỎI MAP

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
            case "INVITE" -> {
                // lÀM SAU 
                break; 
            }
            case "SUBMIT_ANSWER" -> {
                // làm sau
                break; 
            }
            case "C_SQL_PLAYER" -> {
                List<Player> onlinePlayers = playerDAO.getOnlinePlayersForLobby();
                Message message2 = new Message("S_ONLINE_LIST", onlinePlayers); 
                sendMessage(message2);
            }
            case "C_ONLINE_COUNT" -> {
                int count = playerDAO.countPlayerOnline(); 
                Message message3 = new Message("S_ONLINE_COUNT", count); 
                sendMessage(message3); 
            }
        }
    }
}