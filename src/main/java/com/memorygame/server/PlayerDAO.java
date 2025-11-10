package com.memorygame.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

import com.memorygame.common.GameSession;
import com.memorygame.common.Player;
import com.memorygame.common.PlayerStatus;

public class PlayerDAO {
    // Method 1: Lấy thông tin Player bằng username (dùng cho login)
    // Lưu ý: Hàm check pass sẽ để ở lớp logic ClientHandler
    public Player getPlayerByUsername(String username) {
        String sql = "SELECT * FROM Player WHERE username = ?"; 
        
        try (Connection conn = DatabaseConnector.getConnection();
            PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, username);

            try (ResultSet rs = pstm.executeQuery()) {
                if (rs.next()) {
                    return new Player(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("passwordHash"),
                        rs.getInt("totalWins"),
                        PlayerStatus.valueOf(rs.getString("status"))
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); 
        }
        return null;
    }
    // Method 2: Lấy bảng xếp hạng 
    public List<Player> getLeaderBoard() {
        List<Player> leaderboard = new ArrayList<>();
        String sql = "SELECT username, totalWins, status FROM Player ORDER BY totalWins DESC LIMIT 20"; 

        try (Connection conn = DatabaseConnector.getConnection();
            PreparedStatement pstm = conn.prepareStatement(sql);
            ResultSet rs = pstm.executeQuery()) {
            while (rs.next()) {
                Player player = new Player(
                    0,
                    rs.getString("username"),
                    null,
                    rs.getInt("totalWins"),
                    PlayerStatus.valueOf(rs.getString("status"))
                );
                leaderboard.add(player);
            }
        } catch (Exception e) {
            e.printStackTrace(); 
        }
        return leaderboard; 
    }
    // Method 3: Lưu kết quả trận đấu 
    public void saveMatchResult(GameSession session) {
        Player winner = session.getWinner(); 
        Player player1 = session.getPlayer1(); 
        Player player2 = session.getPlayer2();
        int score1 = session.getScores().getOrDefault(player1, 0);
        int score2 = session.getScores().getOrDefault(player2, 0); 
        String insertMatchsql = "INSERT INTO MatchHistory (player1_id, player2_id, player1_score, player2_score, winner_id) VALUES (?, ?, ?, ?, ?)"; 
        String updateWinnersql = "UPDATE Player SET totalWins = totalWins + 1 WHERE id = ?"; 

        Connection conn = null; 
        try {
            conn = DatabaseConnector.getConnection(); 
            conn.setAutoCommit(false); 
            
            // Ghi vào MatchHistory
            try (PreparedStatement pstm = conn.prepareStatement(insertMatchsql)) {
                pstm.setInt(1, player1.getId()); 
                pstm.setInt(2, player2.getId()); 
                pstm.setInt(3, score1); 
                pstm.setInt(4, score2);
                if (winner != null) {
                    pstm.setInt(5, winner.getId()); 
                } else {
                    pstm.setNull(5, java.sql.Types.INTEGER); 
                }
                pstm.executeUpdate(); 
            }
            // Cập nhật totalWins cho người thắng 
            if (winner != null) {
                try (PreparedStatement pstm2 = conn.prepareStatement(updateWinnersql)) {
                    pstm2.setInt(1, winner.getId());
                    int rows = pstm2.executeUpdate();
                    if (rows == 0) {
                        System.err.println("Canh bao: Khong tim thay Player ID " + winner.getId() + " de tang so tran win"); 
                    }
                }
            }

            conn.commit();
            System.out.println("Luu tran dau thanh cong: " + player1.getUsername() + " vs " + player2.getUsername());
        } catch (SQLException e) {
            System.err.println("Loi khi luu ket qua tran dau! Dang rollback...");
            e.printStackTrace();
            // Rollback nếu có lỗi
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("Da rollback giao dich.");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            // Khôi phục AutoCommit và đóng kết nối
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    // Method 3: Update player - cập nhật trạng thái 
    public void updatePlayerStatus(int PlayerId, PlayerStatus status) {
        String sql = "UPDATE Player SET status = ? WHERE id = ?"; 
        try (Connection conn = DatabaseConnector.getConnection();
            PreparedStatement pstm = conn.prepareStatement(sql)) {
            
            pstm.setString(1, status.name()); 
            pstm.setInt(2, PlayerId); 
            pstm.executeUpdate(); 
        } catch (Exception e) {
            e.printStackTrace(); 
        }
    }
    // Method 4: Truy vấn player - "Đổi lại" tại LobbyScene 
    public List<Player> getOnlinePlayersForLobby(int currentUserId) {
        List<Player> onlinePlayers = new ArrayList<>();
        String sql = "SELECT id, username, status, totalWins FROM Player WHERE status = ? AND id != ? LIMIT 50"; 
        
        try (Connection conn = DatabaseConnector.getConnection();
            PreparedStatement pstm = conn.prepareStatement(sql)) {
            
            pstm.setString(1, PlayerStatus.ONLINE.name()); 
            pstm.setInt(2, currentUserId); 
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    Player player = new Player(
                        rs.getInt("id"),
                        rs.getString("username"),
                        null,  // không cần password
                        rs.getInt("totalWins"),
                        PlayerStatus.valueOf(rs.getString("status"))
                    );
                    onlinePlayers.add(player);
                }
            }
            System.out.println("📋 Tim thay " + onlinePlayers.size() + " players ONLINE");
        } catch (Exception e) {
            e.printStackTrace(); 
        }
        return onlinePlayers;
    }
    // Method 5: Truy vấn Đếm số Player đang ONLINE
    public int countPlayerOnline() {
        String sql = "SELECT COUNT(*) AS online_count FROM Player WHERE status = ?";
        int count = 0;

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {

            pstm.setString(1, PlayerStatus.ONLINE.name());

            try (ResultSet rs = pstm.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt("online_count"); // hoặc rs.getInt(1)
                }
            }
        } catch (SQLException e) {
            System.err.println("Loi khi dem nguoi choi ONLINE:");
            e.printStackTrace();
        }

        return count;
    }
    // Method 6: Tạo 1 user mới trong db 
    public boolean createPlayer(String username, String plainTextPassword) {
        String hashedPassword = BCrypt.hashpw(plainTextPassword, BCrypt.gensalt()); 

        String sql  ="INSERT INTO Player (username, passwordHash, status) VALUES (?, ?, 'OFFLINE')"; 

        try(Connection conn = DatabaseConnector.getConnection();
            PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, username); 
            pstm.setString(2, hashedPassword); 

            int rowAffected = pstm.executeUpdate(); 
            return rowAffected > 0; // Trả về true nếu có 1 hàng bị ảnh hưởng (thêm thành công)
        } catch (SQLException e) {
            System.err.println("Loi khi tao Player: " + e.getMessage());
            return false;
        }
    }
}
