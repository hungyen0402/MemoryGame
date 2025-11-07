package com.memorygame.common;
import java.sql.Timestamp; 

public class MatchHistory {
    private int matchId; 
    private Player player1;
    private Player player2; 
    private int player1Score;
    private int player2Score; 
    private Player winner; 
    private Timestamp playedAt; 
    
    public MatchHistory(int matchId, Timestamp playedAt, Player player1, int player1Score, Player player2, int player2Score, Player winner) {
        this.matchId = matchId;
        this.playedAt = playedAt;
        this.player1 = player1;
        this.player1Score = player1Score;
        this.player2 = player2;
        this.player2Score = player2Score;
        this.winner = winner;
    }

    public MatchHistory() {
    }

    public int getMatchId() {
        return matchId;
    }

    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }

    public Player getPlayer1() {
        return player1;
    }

    public void setPlayer1(Player player1) {
        this.player1 = player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public void setPlayer2(Player player2) {
        this.player2 = player2;
    }

    public int getPlayer1Score() {
        return player1Score;
    }

    public void setPlayer1Score(int player1Score) {
        this.player1Score = player1Score;
    }

    public int getPlayer2Score() {
        return player2Score;
    }

    public void setPlayer2Score(int player2Score) {
        this.player2Score = player2Score;
    }

    public Player getWinner() {
        return winner;
    }

    public void setWinner(Player winner) {
        this.winner = winner;
    }

        
}
