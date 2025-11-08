package com.memorygame.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.memorygame.common.Message;
import com.memorygame.common.Player;

public class ClientHandler implements Runnable {
    private Player player;
    private final Socket socket;
    private ObjectOutputStream oos; 
    private ObjectInputStream ois; 

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

    @Override
    public void run() {
        
    }

    public synchronized void sendMessage(Message message) {

    }
    // Hàm này dùng khi user đăng nhập thành công, 
    //Server sẽ gọi hàm này để gán player cho Clienthandler
    public void setPlayer(Player player) {
        this.player = player; 
    }
    public void processMessage(Message message) {
        
    }
}
