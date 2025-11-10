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
    
    // (Bỏ qua serialVersionUID nếu không cần)

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
    private transient Server server; // Dùng 'transient' vì Server không cần Serializable
    private transient VocabularyDAO vocabularyDAO;

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
    }

    // TODO: Bổ sung Constructor cho game Thách đấu (2 người chơi) sau

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
            settings.put("opponentUsername", player2.getUsername()); // Player1 thấy Player2
            server.sendMessageToPlayer(player1, new Message("S_CHALLENGE_START", settings));
            settings.put("opponentUsername", player1.getUsername()); // Player2 thấy Player1
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

        // 1. Lấy từ vựng mới
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

    /**
     * Xử lý khi người chơi gửi câu trả lời (được gọi từ ClientHandler)
     */
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
            // Trả lời đúng
            // Thêm player2 cho chế độ Challenge
            System.out.println("DA CHAM DIEM, KET QUA DUNG"); 
            int currentScore = scores.get(player1);
            int newScore = currentScore + 10; // (Bạn có thể tính điểm phức tạp hơn)
            scores.put(player1, newScore);
            
            // Gửi điểm mới cho client
            server.sendMessageToPlayer(player1, new Message("S_SCORE_UPDATE", newScore));
        } else {
            // Trả lời sai
            // Không gửi gì cả, hoặc gửi điểm cũ
            System.out.println("DA CHAM DIEM, KET QUA SAI"); 
             server.sendMessageToPlayer(player1, new Message("S_SCORE_UPDATE", scores.get(player1)));
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
        if (isPractice) {
            server.sendMessageToPlayer(player1, new Message("S_SCORE_UPDATE_PRACTICE", scores.get(player1)));
        } else {
            Map<String, Object> score = new HashMap<>();
            score.put("score1", scores.get(player1)); 
            score.put("score2", scores.get(player2)); 
            server.sendMessageToPlayer(player1, new Message("S_SCORE_UPDATE_CHALLENGE", score));
            server.sendMessageToPlayer(player2, new Message("S_SCORE_UPDATE_CHALLENGE", score));
        }
        
        
        // Chờ 2 giây rồi bắt đầu vòng mới
        scheduleNextRound(2000);
    }

    /**
     * Kết thúc game
     */
    private void endGame() {
        gameTimer.cancel(); // Hủy mọi hẹn giờ
        int finalScore = scores.get(player1);

        // Gửi tin nhắn kết thúc game
        server.sendMessageToPlayer(player1, new Message("S_GAME_OVER", finalScore));
        
        // Báo cho Server dọn dẹp session này
        server.removePracticeSession(player1);
        System.out.println("Game luyen tap cho " + player1.getUsername() + " da ket thuc.");
    }

    /**
     * Người chơi chủ động rời game
     */
    public void leaveGame() {
        gameTimer.cancel();
        // Báo cho Server dọn dẹp
        server.removePracticeSession(player1);
        System.out.println(player1.getUsername() + " da roi game luyen tap.");
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

    public Player getWinner() {
        if (isPractice) return null;
        // (Bổ sung logic 2 người chơi sau)
        return null;
    }
}