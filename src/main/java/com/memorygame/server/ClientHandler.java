package com.memorygame.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.memorygame.common.Message;
import com.memorygame.common.Player;

public class ClientHandler implements Runnable {
    private static final String C_LOGIN = "C_LOGIN";
    private static final String S_LOGIN_RESPONSE = "S_LOGIN_RESPONSE";

    private Player player;
    private final Socket socket;
    private final Server server;

    private ObjectOutputStream oos;
    private ObjectInputStream ois;

    public ClientHandler(Socket socket,Server server) {
        this.socket = socket;
        this.server = server;
        try{
            this.oos = new ObjectOutputStream(socket.getOutputStream());
            this.ois = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try{
            Message clientMessage;
            while((clientMessage = (Message) ois.readObject()) != null){
                processMessage(clientMessage);
            }
        } catch(IOException | ClassNotFoundException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    public synchronized void sendMessage(Message message) {
        try{
            if(oos != null){
                oos.writeObject(message);
                oos.flush();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void processMessage(Message message) {
        String messageType = message.getType();

        if(C_LOGIN.equals(messageType)) {
            Object[] credentials = (Object[]) message.getPayload();
            String username = (String) credentials[0];
            String password = (String) credentials[1];

            boolean isSuccess = server.handleLogin(username, password, this);

            Message response;
            if(isSuccess){
                response = new Message(S_LOGIN_RESPONSE, this.player);
            } else{
                response = new Message(S_LOGIN_RESPONSE, null);
            }

            sendMessage(response);
        }
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    private void closeConnection() {
        try {
            if (ois != null) ois.close();
            if (oos != null) oos.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
