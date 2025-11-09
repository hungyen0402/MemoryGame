package com.memorygame.client;

import com.memorygame.common.Player;

/**
 * Lớp Singleton để lưu trữ trạng thái của client,
 * quan trọng nhất là thông tin Player đang đăng nhập.
 */
public class ClientState {
    
    private static ClientState instance;
    private Player currentPlayer;

    private ClientState() {
        // Private constructor
    }

    public static ClientState getInstance() {
        if (instance == null) {
            instance = new ClientState();
        }
        return instance;
    }

    public void setCurrentPlayer(Player player) {
        this.currentPlayer = player;
    }

    public Player getCurrentPlayer() {
        return this.currentPlayer;
    }
    
    public String getCurrentUsername() {
        if (this.currentPlayer != null) {
            return this.currentPlayer.getUsername();
        }
        return null; // Trả về null nếu chưa đăng nhập
    }
}