// src/main/java/com/memorygame/client/network/NetworkClient.java
package com.memorygame.client;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

import com.memorygame.common.Message;

import javafx.application.Platform;

public class NetworkClient {
    private static final String HOST = "localhost";
    private static final int PORT = 6789;
    private NetworkClient() {}
    // Thêm ngay dưới class
    private static NetworkClient instance;
    public static NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread receiveThread;
    private volatile boolean running = false;
    
    public interface MessageListener {
        void onMessageReceived(Message msg);
    }
    private MessageListener listener;

    public void setMessageListener(MessageListener listener) {
        this.listener = listener;
    }

    public boolean connect() {
        try {
            socket = new Socket(HOST, PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            running = true;
            startReceiveThread();
            return true;
        } catch (IOException e) {
            System.err.println("Không thể kết nối server: " + e.getMessage());
            return false;
        }
    }

    private void startReceiveThread() {
        receiveThread = new Thread(() -> {
            while (running) {
                try {
                    Object obj = in.readObject();
                    if (obj instanceof Message msg) {
                        if (listener != null) {
                            Platform.runLater(() -> listener.onMessageReceived(msg));
                        }
                    }
                } catch (EOFException | SocketException e) {
                    if (running) System.out.println("Mất kết nối với server.");
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    public void sendMessage(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.err.println("Gửi message thất bại: " + e.getMessage());
        }
    }

    public void disconnect() {
        running = false;
        try { if (socket != null) socket.close(); }
        catch (IOException e) { e.printStackTrace(); }
    }

    public void logout() {
        sendMessage(new Message("C_LOGOUT", null));
        disconnect();
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}