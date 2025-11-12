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

public class GameSession implements Serializable {
    
    private final Player player1;
    private final Player player2;
    private final boolean isPractice;
    private final int totalRounds;
    private final long displayTimes;
    private final long waitTimes;
    
    private int currentRound = 0;
    private Map<Player, Integer> scores;
    private Set<Integer> usedWords;
    private Vocabulary currentWord;
    private Timer gameTimer;
    
    private transient Server server;
    private transient VocabularyDAO vocabularyDAO;

    private enum RoundState { WAITING_FOR_ANSWER, WAITING_FOR_NEXT_ROUND, ENDED }
    private RoundState roundState;

    // ✅ THÊM: Lưu câu trả lời của 2 người chơi
    private Map<Player, Map<String, Object>> playerAnswerDetails;
    private Set<Player> submittedPlayers;

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

        // ✅ KHỞI TẠO cho Challenge
        this.playerAnswerDetails = new HashMap<>();
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
            settings.put("opponentUsername", player2.getUsername());
            server.sendMessageToPlayer(player1, new Message("S_CHALLENGE_START", settings));
            
            settings.put("opponentUsername", player1.getUsername());
            server.sendMessageToPlayer(player2, new Message("S_CHALLENGE_START", settings));
        }
        
        scheduleNextRound(1000);
    }

    private void nextRound() {
        currentRound++;
        if (currentRound > totalRounds) {
            endGame();
            return;
        }

        // ✅ RESET trạng thái cho round mới
        playerAnswerDetails.clear();
        submittedPlayers.clear();

        currentWord = vocabularyDAO.getRandomPhrase(usedWords);
        if (currentWord == null) {
            System.err.println("HET TU VUNG! Ket thuc game som.");
            endGame();
            return;
        }
        usedWords.add(currentWord.getId());

        Object[] roundData = {currentWord.getPhrase(), currentRound, (int) displayTimes};
        
        if (isPractice) {
            server.sendMessageToPlayer(player1, new Message("S_NEW_ROUND_PRACTICE", roundData));
        } else {
            server.sendMessageToPlayer(player1, new Message("S_NEW_ROUND_CHALLENGE", roundData));
            server.sendMessageToPlayer(player2, new Message("S_NEW_ROUND_CHALLENGE", roundData));
        }
        
        gameTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                startAnsweringPhase();
            }
        }, displayTimes * 1000);
    }

    private void startAnsweringPhase() {
        this.roundState = RoundState.WAITING_FOR_ANSWER;
        
        System.out.println("PLAYER TRA LOI CAU HOI"); 
        
        if (isPractice) {
            server.sendMessageToPlayer(player1, new Message("S_ANSWER_PHASE_PRACTICE", (int) waitTimes));
        } else {
            server.sendMessageToPlayer(player1, new Message("S_ANSWER_PHASE_CHALLENGE", (int) waitTimes));
            server.sendMessageToPlayer(player2, new Message("S_ANSWER_PHASE_CHALLENGE", (int) waitTimes));
        }
        
        gameTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                timesUp();
            }
        }, waitTimes * 1000);
    }

    // ✅ SỬA: Submit Answer cho Practice (1 người)
    public synchronized void submitAnswer(Map<String, Object> payload) {
        if (this.roundState != RoundState.WAITING_FOR_ANSWER) {
            return; 
        }
        
        this.roundState = RoundState.WAITING_FOR_NEXT_ROUND;
        gameTimer.cancel();
        gameTimer = new Timer();

        String answer = (String) payload.getOrDefault("answer", "");
        Integer remainingTime = (Integer) payload.getOrDefault("remainingTime", 0);

        if (answer != null && currentWord != null && answer.equalsIgnoreCase(currentWord.getPhrase())) {
            int wordLength = currentWord.getLength();
            int roundScore = remainingTime * wordLength;
            int currentScore = scores.get(player1);
            int newScore = currentScore + roundScore;
            scores.put(player1, newScore);
            System.out.println("DA CHAM DIEM, KET QUA DUNG"); 
            server.sendMessageToPlayer(player1, new Message("S_SCORE_UPDATE_PRACTICE", newScore));
        } else {
            System.out.println("DA CHAM DIEM, KET QUA SAI"); 
            server.sendMessageToPlayer(player1, new Message("S_SCORE_UPDATE_PRACTICE", scores.get(player1)));
        }

        scheduleNextRound(2000);
    }

    // ✅ MỚI: Submit Answer cho Challenge (2 người)
    public synchronized void submitAnswerChallenge(Player player, Map<String, Object> payload) {
        String answer = (String) payload.get("answer");
        Integer remainingTime = (Integer) payload.get("remainingTime");

        if (this.roundState != RoundState.WAITING_FOR_ANSWER) {
            System.out.println("[WARN] " + player.getUsername() + " gửi answer khi không phải lúc.");
            return; 
        }

        // Kiểm tra người này đã gửi chưa
        if (submittedPlayers.contains(player)) {
            System.out.println("[WARN] " + player.getUsername() + " đã gửi answer rồi!");
            return;
        }

        // Lưu câu trả lời
        Map<String, Object> details = new HashMap<>();
        details.put("answer", answer);
        details.put("remainingTime", remainingTime != null ? remainingTime : 0);
        playerAnswerDetails.put(player, details);
        submittedPlayers.add(player);
        
        System.out.println(player.getUsername() + " đã gửi answer: " + answer);

        // ✅ CHỈ CHẤM ĐIỂM KHI CẢ 2 ĐÃ GỬI
        if (submittedPlayers.size() == 2) {
            gameTimer.cancel(); // Dừng timer hết giờ
            gameTimer = new Timer();
            
            processAnswers();
        }
    }

    // ✅ MỚI: Xử lý câu trả lời của cả 2 người
    private void processAnswers() {
        this.roundState = RoundState.WAITING_FOR_NEXT_ROUND;

        Map<String, Object> details1 = playerAnswerDetails.getOrDefault(player1, new HashMap<>());
        Map<String, Object> details2 = playerAnswerDetails.getOrDefault(player2, new HashMap<>());

        String answer1 = (String) details1.getOrDefault("answer", "");
        int time1 = (Integer) details1.getOrDefault("remainingTime", 0);
    
        String answer2 = (String) details2.getOrDefault("answer", "");
        int time2 = (Integer) details2.getOrDefault("remainingTime", 0);

        int wordLength = currentWord.getLength();

        // Chấm điểm cho Player 1
        if (answer1 != null && currentWord != null && answer1.equalsIgnoreCase(currentWord.getPhrase())) {
            int roundScore = time1 * wordLength; 
            int newScore = scores.get(player1) + roundScore;
            scores.put(player1, newScore);
            System.out.println("Player1 TRẢ LỜI ĐÚNG: " + newScore);
        } else {
            System.out.println("Player1 TRẢ LỜI SAI");
        }

        // Chấm điểm cho Player 2
        if (answer2 != null && currentWord != null && answer2.equalsIgnoreCase(currentWord.getPhrase())) {
            int roundScore = time2 * wordLength;
            int newScore = scores.get(player2) + roundScore;
            scores.put(player2, newScore);
            System.out.println("Player2 TRẢ LỜI ĐÚNG: " + newScore);
        } else {
            System.out.println("Player2 TRẢ LỜI SAI");
        }

        // Gửi điểm cho cả 2
        Object[] scoreDataP1 = {scores.get(player1), scores.get(player2)};
        server.sendMessageToPlayer(player1, new Message("S_SCORE_UPDATE_CHALLENGE", scoreDataP1));

        Object[] scoreDataP2 = {scores.get(player2), scores.get(player1)};
        server.sendMessageToPlayer(player2, new Message("S_SCORE_UPDATE_CHALLENGE", scoreDataP2));

        System.out.println("ĐIỂM ROUND " + currentRound + ": " + scores.get(player1) + " - " + scores.get(player2));

        scheduleNextRound(2000);
    }

    private synchronized void timesUp() {
        if (this.roundState != RoundState.WAITING_FOR_ANSWER) {
            return;
        }
        
        System.out.println("HẾT GIỜ TRẢ LỜI - Số người đã gửi: " + submittedPlayers.size());
        
        gameTimer = new Timer();
        
        if (isPractice) {
            this.roundState = RoundState.WAITING_FOR_NEXT_ROUND;
            server.sendMessageToPlayer(player1, new Message("S_SCORE_UPDATE_PRACTICE", scores.get(player1)));
            scheduleNextRound(2000);
        } else {
            // ✅ CHẤM ĐIỂM NGAY CẢ KHI CHỈ 1 HOẶC 0 NGƯỜI GỬI
            processAnswers();
        }
    }

    // Được gọi khi hết round hoặc hết từ vựng
    private void endGame() {
        if (gameTimer != null) gameTimer.cancel();
        
        if (isPractice) {
            int finalScore = scores.get(player1);
            server.sendMessageToPlayer(player1, new Message("S_GAME_OVER", finalScore));
            server.removePracticeSession(player1);
            System.out.println("Game luyen tap cho " + player1.getUsername() + " da ket thuc.");
        } else {
            String winnerUsername = getWinner() != null ? getWinner().getUsername() : null;
            endChallengeSession(winnerUsername); // Gửi kết quả và dọn dẹp
        }
    }

    // Mất kết nối đột ngột
    public synchronized void handleOpponentForfeit(Player leaver) {
        if (isPractice) return; // Chỉ xử lý Challenge

        if (gameTimer != null) gameTimer.cancel();
        this.roundState = RoundState.ENDED;
        
        // Gán 0 điểm cho người rời trận (Forfeit)
        scores.put(leaver, 0); 

        // Xác định người còn lại
        Player winner = (leaver.equals(player1)) ? player2 : player1;
        // Nếu điểm bằng 0 thì tăng lên 1
        scores.computeIfPresent(winner, (player, currentScore) -> {
            if (currentScore == 0) {
                return 1;
            }
            return currentScore;
        });
        String winnerUsername = winner.getUsername();

        // Gửi kết quả và dọn dẹp
        endChallengeSession(winnerUsername);
    }

    // Gửi kết quả và dọn dẹp
    private void endChallengeSession(String winnerUsername) {
        Player p1 = player1;
        Player p2 = player2;
        int score1 = scores.get(p1);
        int score2 = scores.get(p2);
        
        System.out.println("KẾT THÚC CHALLENGE: " + p1.getUsername() + "(" + score1 + ") vs " + p2.getUsername() + "(" + score2 + ")");
        
        // Gửi kết quả cho Player 1
        Map<String, Object> resultForP1 = new HashMap<>();
        resultForP1.put("winnerUsername", winnerUsername);
        resultForP1.put("yourScore", score1);
        resultForP1.put("opponentScore", score2);
        resultForP1.put("opponentPlayer", p2);
        server.sendMessageToPlayer(p1, new Message("S_CHALLENGE_END", resultForP1));
        
        // Gửi kết quả cho Player 2
        Map<String, Object> resultForP2 = new HashMap<>();
        resultForP2.put("winnerUsername", winnerUsername);
        resultForP2.put("yourScore", score2);
        resultForP2.put("opponentScore", score1);
        resultForP2.put("opponentPlayer", p1);
        server.sendMessageToPlayer(p2, new Message("S_CHALLENGE_END", resultForP2));

        // Ghi lịch sử và dọn dẹp
        server.getPlayerDAO().saveMatchResult(this); 
        server.removeChallengeSession(p1, p2);
    }

    // Thoát trận luyện tập
    public void leavePractice() {
        if (gameTimer != null) gameTimer.cancel();
        server.removePracticeSession(player1);
        System.out.println(player1.getUsername() + " da roi game luyen tap.");
    }

    // Đối phương chủ động bấm thoát trận thách đấu
    public synchronized void handleOpponentLeave(Player leaver) {
        gameTimer.cancel(); 
        this.roundState = RoundState.ENDED;
        
        // Xác định người còn lại
        Player winner = (leaver.equals(player1)) ? player2 : player1;
        // Nếu điểm bằng 0 thì tăng lên 1
        scores.computeIfPresent(winner, (player, currentScore) -> {
            if (currentScore == 0) {
                return 1;
            }
            return currentScore;
        });
        String winnerUsername = winner.getUsername();

        // Gửi kết quả và dọn dẹp
        endChallengeSession(winnerUsername);
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