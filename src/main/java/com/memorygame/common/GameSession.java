package com.memorygame.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.memorygame.server.Server;
import com.memorygame.server.VocabularyDAO;

/**
 * Lớp này chỉ tồn tại ở phía Server.
 * Nó quản lý trạng thái, vòng chơi, tính điểm và hẹn giờ cho một game.
 */
public class GameSession implements Serializable {
    // Cài đặt game
    private final Player player1; // Người chơi (cho chế độ luyện tập)
    private final Player player2; // Sẽ là null nếu là luyện tập
    private final boolean isPractice;
    private final int totalRounds;
    private final long displayTimes; // Thời gian hiển thị từ (giây)
    private final long waitTimes;    // Thời gian trả lời (giây)
    
    // Trạng thái game
    private int currentRound = 0;
    private Map<Player, Integer> scores;
    private Set<Integer> usedWords; // Chứa ID của từ vựng đã dùng
    private Vocabulary currentWord; // Từ vựng của vòng hiện tại
    private Timer gameTimer;        // Bộ hẹn giờ cho các vòng
    
    // Tham chiếu đến các thành phần của Server
    private transient Server server; 
    private transient VocabularyDAO vocabularyDAO;

    // Sửa COMMIT 7
    private Map<Player, String> playerAnswers; 
    private Set<Player> submittedPlayers; 

    // Trạng thái của vòng chơi
    private enum RoundState { WAITING_FOR_ANSWER, WAITING_FOR_NEXT_ROUND }
    private RoundState roundState;

    /**
     * Constructor cho Game Luyện tập
     */
    // GameSession.java – Thêm constructor cho 2 người
    public GameSession(Player player1, Player player2, Map<String, Object> settings, Server server, boolean isPractice) {
        this.player1 = player1;
        this.player2 = player2;
        this.isPractice = isPractice;

        this.displayTimes = (long) settings.get("thinkTime");
        this.totalRounds = (int) settings.get("totalRounds");
        this.waitTimes = (long) settings.get("waitTime");

        this.server = server;
        this.vocabularyDAO = server.getVocabularyDAO();

        this.scores = new HashMap<>();
        this.scores.put(player1, 0);
        if (player2 != null) {
            this.scores.put(player2, 0);
        }
        this.usedWords = new HashSet<>();
        this.gameTimer = new Timer();

        // Sửa COMMIT 7
        this.playerAnswers = new HashMap<>();
        this.submittedPlayers = new HashSet<>();  
    }

    public void start() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("thinkTime", displayTimes);
        settings.put("totalRounds", totalRounds);
        settings.put("waitTime", waitTimes);
        if(isPractice){
            server.sendMessageToPlayer(player1, new Message("S_PRACTICE_START", settings));
        }else{
            settings.put("opponentUsername", player2.getUsername()); // Player1 thấy Player2
            server.sendMessageToPlayer(player1, new Message("S_CHALLENGE_START", settings));
            settings.put("opponentUsername", player1.getUsername()); // Player2 thấy Player1
            server.sendMessageToPlayer(player2, new Message("S_CHALLENGE_START", settings));
        }
        // 2. Bắt đầu vòng đầu tiên (sau 1 giây)
        scheduleNextRound(1000);
    }

    private void nextRound() {
        currentRound++;
        if (currentRound > totalRounds) {
            endGame();
            return;
        }
        // Sửa COMMIT 7 
        playerAnswers.clear();
        submittedPlayers.clear();

        currentWord = vocabularyDAO.getRandomPhrase(usedWords);
        if (currentWord == null) {
            System.err.println("HET TU VUNG! Ket thuc game som.");
            endGame();
            return;
        }
        usedWords.add(currentWord.getId());

        // 2. Gửi từ vựng cho client (Giai đoạn ghi nhớ)
        // Payload: Object[] {word, round, memorizeTime}
        Object[] roundData = {currentWord.getPhrase(), currentRound, (int) displayTimes};
        if (isPractice) {
            server.sendMessageToPlayer(player1, new Message("S_NEW_ROUND_PRACTICE", roundData));
        } else {
            server.sendMessageToPlayer(player1, new Message("S_NEW_ROUND_CHALLENGE", roundData));
            server.sendMessageToPlayer(player2, new Message("S_NEW_ROUND_CHALLENGE", roundData));
        }
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
        System.out.println("PLAYER TRA LOI CAU HOI"); 
        if (isPractice) {
            server.sendMessageToPlayer(player1, new Message("S_ANSWER_PHASE_PRACTICE", (int) waitTimes));
        } else {
            server.sendMessageToPlayer(player1, new Message("S_ANSWER_PHASE_CHALLENGE", (int) waitTimes));
            server.sendMessageToPlayer(player2, new Message("S_ANSWER_PHASE_CHALLENGE", (int) waitTimes));
        }
        // 2. Hẹn giờ "hết giờ trả lời"
        gameTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                timesUp();
            }
        }, waitTimes * 1000);
    }

    // Sửa COMMIT 7. Tính điểm cho 1 player - chế độ Practice 
    public synchronized void submitAnswer(String answer) {
        // Chỉ xử lý nếu đang ở trạng thái chờ trả lời
        if (this.roundState != RoundState.WAITING_FOR_ANSWER) {
            return; 
        }
        
        this.roundState = RoundState.WAITING_FOR_NEXT_ROUND;
        gameTimer.cancel(); // Hủy tất cả các hẹn giờ (quan trọng là hủy cái timesUp())
        gameTimer = new Timer(); // Tạo lại timer mới cho vòng sau

        // Tính điểm
        if (answer != null && currentWord != null && answer.equalsIgnoreCase(currentWord.getPhrase())) {
            System.out.println("DA CHAM DIEM, KET QUA DUNG"); 
            int currentScore = scores.get(player1);
            int newScore = currentScore + 10;
            scores.put(player1, newScore);
            server.sendMessageToPlayer(player1, new Message("S_SCORE_UPDATE_PRACTICE", newScore));
        } else {
            System.out.println("DA CHAM DIEM, KET QUA SAI"); 
            server.sendMessageToPlayer(player1, new Message("S_SCORE_UPDATE_PRACTICE", scores.get(player1)));
        }

        // Chờ 2 giây rồi bắt đầu vòng mới
        scheduleNextRound(2000);
    }
    // Sửa COMMIT 7 
    public synchronized void submitAnswerChallenge(Player player, String answer) {
        if (this.roundState != RoundState.WAITING_FOR_ANSWER) {
            System.out.println("[WARN] " + player.getUsername() + " gui answer khong phai luc.");
            return; 
        }

        // Kiểm tra người này đã gửi chưa
        if (submittedPlayers.contains(player)) {
            System.out.println("[WARN] " + player.getUsername() + " da gui answer roi!");
            return;
        }

        // Lưu câu trả lời
        playerAnswers.put(player, answer);
        submittedPlayers.add(player);
        
        System.out.println(player.getUsername() + " da gui answer: " + answer);

        // CHỈ CHẤM ĐIỂM KHI CẢ 2 ĐÃ GỬI
        if (submittedPlayers.size() == 2) {
            gameTimer.cancel(); // Dừng timer hết giờ
            gameTimer = new Timer();
            
            processAnswers();
        }
    }

    // COMMIT 7
    private void processAnswers() {
        this.roundState = RoundState.WAITING_FOR_NEXT_ROUND;

        String answer1 = playerAnswers.getOrDefault(player1, "");
        String answer2 = playerAnswers.getOrDefault(player2, "");

        // Chấm điểm cho Player 1
        if (answer1 != null && currentWord != null && answer1.equalsIgnoreCase(currentWord.getPhrase())) {
            int newScore = scores.get(player1) + 10;
            scores.put(player1, newScore);
            System.out.println("Player1 TRA LOI DUNG: " + newScore);
        } else {
            System.out.println("Player1 TRA LOI SAI");
        }

        // Chấm điểm cho Player 2
        if (answer2 != null && currentWord != null && answer2.equalsIgnoreCase(currentWord.getPhrase())) {
            int newScore = scores.get(player2) + 10;
            scores.put(player2, newScore);
            System.out.println("Player2 TRA LOI DUNG: " + newScore);
        } else {
            System.out.println("Player2 TRA LOI SAI");
        }

        // Gửi điểm cho cả 2
        Object[] scoreData = {scores.get(player1), scores.get(player2)};
        server.sendMessageToPlayer(player1, new Message("S_SCORE_UPDATE_CHALLENGE", scoreData));
        server.sendMessageToPlayer(player2, new Message("S_SCORE_UPDATE_CHALLENGE", scoreData));

        System.out.println("DIEM ROUND " + currentRound + ": " + scores.get(player1) + " - " + scores.get(player2));

        scheduleNextRound(2000);
    }

    private synchronized void timesUp() {
        if (this.roundState != RoundState.WAITING_FOR_ANSWER) {
            return;
        }
        
        System.out.println("HET GIO TRA LOI - SO NGUOI DA GUI: " + submittedPlayers.size());

        gameTimer = new Timer(); 
        
        if (isPractice) {
            this.roundState = RoundState.WAITING_FOR_NEXT_ROUND;
            server.sendMessageToPlayer(player1, new Message("S_SCORE_UPDATE_PRACTICE", scores.get(player1)));
            scheduleNextRound(2000);
        } else {
            // CHẤM ĐIỂM NGAY CẢ KHI CHỈ 1 HOẶC 0 NGƯỜI GỬI
            processAnswers();
        }
    }

    private void endGame() {
        gameTimer.cancel();
        
        if (isPractice) {
            int finalScore = scores.get(player1);
            server.sendMessageToPlayer(player1, new Message("S_GAME_OVER", finalScore));
            server.removePracticeSession(player1);
            System.out.println("Game luyen tap cho " + player1.getUsername() + " da ket thuc.");
        } else {
            // TODO: Xử lý kết thúc Challenge
            int score1 = scores.get(player1);
            int score2 = scores.get(player2);
            
            String winnerUsername = null;
            if (score1 > score2) {
                winnerUsername = player1.getUsername();
            } else if (score2 > score1) {
                winnerUsername = player2.getUsername();
            }
            
            System.out.println("KẾT THÚC CHALLENGE: " + player1.getUsername() + "(" + score1 + ") vs " + player2.getUsername() + "(" + score2 + ")");
        }
    }

    // user chủ động rời game 
    public void leaveGame() {
        gameTimer.cancel();
        server.removePracticeSession(player1);
        System.out.println(player1.getUsername() + " da roi game luyen tap.");
    }
    
    private void scheduleNextRound(long delayMs) {
         gameTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                nextRound();
            }
        }, delayMs);
    }

    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public boolean isPractice() { return isPractice; }
    public Map<Player, Integer> getScores() { return scores; }

    public Player getWinner() {
        if (isPractice) return null;
        int score1 = scores.get(player1);
        int score2 = scores.get(player2);
        if (score1 > score2) return player1;
        if (score2 > score1) return player2;
        return null;
    }
}