package com.memorygame.common;

public class Player {
    private final int id;
    private final String userName;
    private final String passwordHash;
    private final int totalWins;
    private final PlayerStatus status;

    public Player() {
        this.id = 0;
        this.userName = null;
        this.passwordHash = null;
        this.totalWins = 0;
        this.status = PlayerStatus.OFFLINE;
    }

    public boolean equals(Object o) {
        return true;
    }

    public int hashCode() {
        return 0;
    }
}
