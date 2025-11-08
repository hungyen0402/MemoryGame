package com.memorygame.server;

import java.net.Socket;

import com.memorygame.common.Message;
import com.memorygame.common.Player;

public class ClientHandler implements Runnable {
    private final Player player;
    private final Socket socket;

    private ClientHandler() {
        this.player = null;
        this.socket = null;
    }

    @Override
    public void run() {

    }

    public synchronized void sendMessage(Message message) {

    }

    public void processMessage(Message message) {
        
    }
}