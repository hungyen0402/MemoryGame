package com.memorygame.common;
import java.util.Map;
import java.util.Set; 

public class GameSession {
    private Player player1; 
    private Player player2; 
    private Set<Integer> useWords; // set chứa các id của đối tượng Vocabulary
    private int totalRounds; 
    private int currentRounds; 
    private long waitTimes; 
    private long displayTimes; 
    private Map<Player, Integer> scores; 
    private boolean isPractice; 

    // Các method 
    public void start() {
        // Logic bắt đầu game
    }   

    public void nextRound() {
        // Logic sang round mới, gửi vocabulary 
    }

    public void submitAnswer(Player player, String answer) {
        // Logic nhận đáp án
    }

    public int calculateScore() {
        // Logic tính điểm 
        return 0; 
    }

    public void endGame() {
        // Logic kết thúc game, so sánh điểm, báo cho PlayerDAO
    }

    // Getter
    public Player getPlayer1() {return player1;}
    public Player getPlayer2() {return player2;}
    public Player getWinner() {
        // bổ sung code 
        return null; 
    }

    public int getTotalRounds() {
        return totalRounds;
    }

    public int getCurrentRounds() {
        return currentRounds;
    }

    public Map<Player, Integer> getScores() {
        return scores;
    }
}
