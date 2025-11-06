package com.memorygame.server;

import java.util.Map;
import java.util.Set;

import com.memorygame.common.Player;

public class GameSession {
    private final Player player1;
    private final Player player2;
    private final Set<Integer> usedWords;
    private final int totalRounds;
    private final int currentRound;
    private final long waitTimeMs;
    private final long displayTimeMs;
    private final Map<Player, Integer> scores;
    private final boolean isPractice;

    public GameSession() {
        this.player1 = null;
        this.player2 = null;
        this.usedWords = null;
        this.totalRounds = 0;
        this.currentRound = 0;
        this.waitTimeMs = 0;
        this.displayTimeMs = 0;
        this.scores = null;
        this.isPractice = false;
    }

    public void start() {

    }

    public synchronized void nextRound() {

    }

    public synchronized void submitAnswer(Player player, String answer) {

    }

    public void calculateScore() {

    }

    public void endGame() {
        
    }
}
