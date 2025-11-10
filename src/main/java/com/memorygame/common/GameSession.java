package com.memorygame.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.memorygame.server.PlayerDAO;
import com.memorygame.server.Server;
import com.memorygame.server.VocabularyDAO;

/**
 * Lớp này chỉ tồn tại ở phía Server.
 * Nó quản lý trạng thái, vòng chơi, tính điểm và hẹn giờ cho một game.
 */
public class GameSession implements Serializable {
    
    // (Bỏ qua serialVersionUID nếu không cần)

    // Cài đặt game
    private final Player player1; // Người chơi (cho chế độ luyện tập)
    private final Player player2; // Sẽ là null nếu là luyện tập
    private final boolean isPractice;
    private final int totalRounds;
    private final long displayTimes;// Thời gian hiển thị từ (giây)
    private final long waitTimes;    // Thời gian trả lời (giây)

    // Trạng thái game
    private int currentRound = 0;
    private Map<Player, Integer> scores;
    private Set<Integer> usedWords; // Chứa ID của từ vựng đã dùng
    private Vocabulary currentWord; // Từ vựng của vòng hiện tại
    private Timer gameTimer;        // Bộ hẹn giờ cho các vòng

    private String player1Answer;
    private String player2Answer;
    
    // Tham chiếu đến các thành phần của Server
    private transient Server server; // Dùng 'transient' vì Server không cần Serializable
    private transient VocabularyDAO vocabularyDAO;
    private transient PlayerDAO playerDAO;

    // Trạng thái của vòng chơi
    private enum RoundState { WAITING_FOR_MEMORIZING,WAITING_FOR_ANSWER, WAITING_FOR_NEXT_ROUND }
    private RoundState roundState;

    /**
     * Constructor cho Game Luyện tập
     */
    public GameSession(Player player, Map<String, Object> settings, Server server) {
        this.player1 = player;
        this.player2 = null;
        this.isPractice = true;
        
        // Lấy cài đặt từ settings map (với giá trị mặc định)
        this.displayTimes = (long) settings.getOrDefault("thinkTime", 3L);
        this.totalRounds = (int) settings.getOrDefault("totalRounds", 5);
        this.waitTimes = (long) settings.getOrDefault("waitTime", 10L);
        
        this.server = server;
        this.vocabularyDAO = server.getVocabularyDAO();
        
        this.scores = new HashMap<>();
        this.scores.put(player1, 0);
        this.usedWords = new HashSet<>();
        this.gameTimer = new Timer();
    }

// game challenge
    public GameSession(Player player1,Player player2, Map<String, Object> settings, Server server) {
        this.player1 = player1;
        this.player2 = player2;
        this.isPractice = false;

        // Lấy cài đặt từ settings map (với giá trị mặc định)
        this.displayTimes = (long) settings.getOrDefault("thinkTime", 3L);
        this.totalRounds = (int) settings.getOrDefault("totalRounds", 5);
        this.waitTimes = (long) settings.getOrDefault("waitTime", 10L);

        this.server = server;
        this.vocabularyDAO = server.getVocabularyDAO();
        this.playerDAO = server.getPlayerDAO();

        this.scores = new HashMap<>();
        this.scores.put(player1, 0);
        this.scores.put(player2, 0);
        this.usedWords = new HashSet<>();
        this.gameTimer = new Timer();
    }

    private void broadcastMessage(Message message) {
        server.sendMessageToPlayer(player1, message);
        if (player2 != null) {
            server.sendMessageToPlayer(player2, message);
        }
    }

    /**
     * Bắt đầu phiên chơi game (được gọi bởi Server.java)
     */
    public void start() {
        // 1. Gửi tin nhắn cho client biết game đã bắt đầu (để client chuyển scene)
        // Gửi lại cài đặt để client hiển thị
        Map<String, Object> settings = new HashMap<>();
        settings.put("thinkTime", displayTimes);
        settings.put("totalRounds", totalRounds);
        settings.put("waitTime", waitTimes);
        
        if(isPractice){
            server.sendMessageToPlayer(player1, new Message("S_PRACTICE_START", settings));
        }else{
            settings.put("opponent", player2); // Player1 thấy Player2
            server.sendMessageToPlayer(player1, new Message("S_CHALLENGE_START", settings));
            settings.put("opponent", player1); // Player2 thấy Player1
            server.sendMessageToPlayer(player2, new Message("S_CHALLENGE_START", settings));
        }

        // 2. Bắt đầu vòng đầu tiên (sau 1 giây)
        scheduleNextRound(1000);
    }

    /**
     * Chuẩn bị và bắt đầu vòng chơi mới
     */
    private void nextRound() {
        currentRound++;
        if (currentRound > totalRounds) {
            endGame();
            return;
        }
        this.player1Answer = null;
        this.player2Answer = null;
        // 1. Lấy từ vựng mới
        currentWord = vocabularyDAO.getRandomPhrase(usedWords);
        if (currentWord == null) {
            System.err.println("HET TU VUNG! Ket thuc game som.");
            endGame();
            return;
        }
        usedWords.add(currentWord.getId());
        this.roundState = RoundState.WAITING_FOR_MEMORIZING;
        // 2. Gửi từ vựng cho client (Giai đoạn ghi nhớ)
        // Payload: Object[] {word, round, memorizeTime}
        Object[] roundData = {currentWord.getPhrase(), currentRound, (int) displayTimes};
        broadcastMessage(new Message("S_NEW_ROUND", roundData));
        
        // 3. Hẹn giờ để bắt đầu giai đoạn trả lời
        gameTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                startAnsweringPhase();
            }
        }, displayTimes * 1000); // nhân 1000 để đổi sang mili-giây
    }

    /**
     * Bắt đầu giai đoạn trả lời (sau khi hết giờ ghi nhớ)
     */
    private void startAnsweringPhase() {
        this.roundState = RoundState.WAITING_FOR_ANSWER;
        
        // 1. Báo cho client biết để bật ô nhập liệu
        // Payload: Integer answerTime
        broadcastMessage(new Message("S_ANSWER_PHASE", (int) waitTimes));

        // 2. Hẹn giờ "hết giờ trả lời"
        gameTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                timesUp();
            }
        }, waitTimes * 1000);
    }

    /**
     * Xử lý khi người chơi gửi câu trả lời (được gọi từ ClientHandler)
     */
    public synchronized void submitAnswer(Player player,String answer) {
        // Chỉ xử lý nếu đang ở trạng thái chờ trả lời
        if (this.roundState != RoundState.WAITING_FOR_ANSWER) {
            return; 
        }


        if (player.getId() == player1.getId()) {
            this.player1Answer = answer;
        } else if (player2 != null && player.getId() == player2.getId()) {
            this.player2Answer = answer;
        }

        if (isPractice) {
            this.roundState = RoundState.WAITING_FOR_NEXT_ROUND;
            gameTimer.cancel();
            gameTimer = new Timer();
            scoreRound();
        }
        else if (player1Answer != null && player2Answer != null) {
            this.roundState = RoundState.WAITING_FOR_NEXT_ROUND;
            gameTimer.cancel(); // Hủy hẹn giờ timesUp()
            gameTimer = new Timer();
            scoreRound();
        }
    }

    private void scoreRound() {
        int score1 = scores.get(player1);

        // Chấm điểm Player 1
        if (player1Answer != null && currentWord != null && player1Answer.equalsIgnoreCase(currentWord.getPhrase())) {
            score1 += 10;
            scores.put(player1, score1);
        }

        // Gửi kết quả
        if (isPractice) {
            // Chế độ 1 người: Gửi "S_SCORE_UPDATE" với 1 Integer
            server.sendMessageToPlayer(player1, new Message("S_SCORE_UPDATE", score1));
        } else {
            // Chế độ 2 người
            int score2 = scores.get(player2);
            if (player2Answer != null && currentWord != null && player2Answer.equalsIgnoreCase(currentWord.getPhrase())) {
                score2 += 10;
                scores.put(player2, score2);
            }

            // Gửi "S_SCORE_UPDATE" (payload là 2 điểm)
            // Khớp với: onScoreUpdate(int playerScore, int opponentScore)
            Object[] scorePayload = {score1, score2};
            broadcastMessage(new Message("S_SCORE_UPDATE", scorePayload));
        }

        // Chờ 2 giây rồi bắt đầu vòng mới
        scheduleNextRound(2000);
    }

    /**
     * Xử lý khi hết giờ trả lời
     */
    private synchronized void timesUp() {
        if (this.roundState != RoundState.WAITING_FOR_ANSWER) {
            return;
        }
        
        System.out.println("Nguoi choi " + player1.getUsername() + " da het gio tra loi.");
        this.roundState = RoundState.WAITING_FOR_NEXT_ROUND;
        // gameTimer đã tự hủy khi chạy xong task này, ta tạo lại timer
        gameTimer = new Timer(); 
        
        // Không cộng điểm
        server.sendMessageToPlayer(player1, new Message("S_SCORE_UPDATE", scores.get(player1)));
        
        // Chờ 2 giây rồi bắt đầu vòng mới
        scheduleNextRound(2000);
    }

    /**
     * Kết thúc game
     */
    private void endGame() {
        gameTimer.cancel(); // Hủy mọi hẹn giờ
        if (isPractice) {
            int finalScore = scores.get(player1);
            server.sendMessageToPlayer(player1, new Message("S_GAME_OVER", finalScore));
            server.removePracticeSession(player1);
        } else {
            // Chế độ 2 người
            int finalScore1 = scores.get(player1);
            int finalScore2 = scores.get(player2);

            // Xác định người thắng
            Player winner = getWinner();

            // Lưu kết quả (PlayerDAO đã hỗ trợ)
            if (playerDAO != null) {
                playerDAO.saveMatchResult(this);
            }

            // Gửi "S_GAME_OVER" (payload là 3 giá trị)
            // Khớp với: onGameOver(String winnerUsername, int finalPlayerScore, int finalOpponentScore)
            String winnerUsername = (winner != null) ? winner.getUsername() : null;
            Object[] gameOverPayload = {winnerUsername, finalScore1, finalScore2};
            broadcastMessage(new Message("S_GAME_OVER", gameOverPayload));

            // Dọn dẹp session
            server.removeChallengeSession(this);
        }
    }

    /**
     * Người chơi chủ động rời game
     */
    public void leaveGame(Player leaver) {
        gameTimer.cancel();
        if (isPractice) {
            server.removePracticeSession(player1);
        } else {
            // (Logic 2 người chơi) Xác định người thắng
            Player winner = (leaver.getId() == player1.getId()) ? player2 : player1;
            scores.put(winner, 999); // Thắng tuyệt đối
            scores.put(leaver, 0);

            String winnerUsername = winner.getUsername();
            Object[] gameOverPayload = {winnerUsername, scores.get(player1), scores.get(player2)};
            broadcastMessage(new Message("S_GAME_OVER", gameOverPayload));

            // Lưu kết quả
            if (playerDAO != null) {
                playerDAO.saveMatchResult(this);
            }

            // Dọn dẹp
            server.removeChallengeSession(this);
        }
    }
    
    /**
     * Hẹn giờ cho vòng tiếp theo (tránh lỗi Timer)
     */
    private void scheduleNextRound(long delayMs) {
        gameTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    nextRound();
                }
            }, delayMs);
    }


    // --- Các Getter (nếu cần) ---
        public Player getPlayer1() { return player1; }
        public Player getPlayer2() { return player2; }
        public boolean isPractice() { return isPractice; }
        public Map<Player, Integer> getScores() { return scores; }
        public int getTotalRounds() { return totalRounds; }
        public long getDisplayTimes() { return displayTimes; }

    public Player getWinner() {
        if (isPractice || scores.isEmpty() || player2 == null) return null;

        int score1 = scores.getOrDefault(player1, 0);
        int score2 = scores.getOrDefault(player2, 0);

        if (score1 > score2) return player1;
        if (score2 > score1) return player2;
        return null; // Hòa
    }
}

