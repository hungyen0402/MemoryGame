package com.memorygame.common;

public class Player {
    private int id; 
    private String username; 
    private String passwordHash; 
    private int totalWins; 
    private PlayerStatus status; 

    public Player(int id, String passwordHash, PlayerStatus status, int totalWins, String username) {
        this.id = id;
        this.passwordHash = passwordHash;
        this.status = status;
        this.totalWins = totalWins;
        this.username = username;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public int getTotalWins() {
        return totalWins;
    }

    public void setTotalWins(int totalWins) {
        this.totalWins = totalWins;
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public void setStatus(PlayerStatus status) {
        this.status = status;
    }

    
}
