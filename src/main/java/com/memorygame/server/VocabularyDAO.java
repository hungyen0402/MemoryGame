package com.memorygame.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Set;
import com.memorygame.common.Vocabulary;

public class VocabularyDAO {
    public Vocabulary getRandomPhrase(Set<Integer> excludedIds) {
        StringBuilder sql = new StringBuilder("SELECT * FROM Vocabulary");

        if (excludedIds != null && !excludedIds.isEmpty()) {
            sql.append("WHERE id NOT IN ("); 
            String placeholders = String.join(",", Collections.nCopies(excludedIds.size(), "?")); 
            sql.append(placeholders); 
            sql.append(")"); 
        }
        sql.append(" ORDER BY RAND() LIMIT 1"); 
        System.out.println("Truy vấn: " + sql.toString());

        // Thực hiện truy vấn
        // Đọc document PreparedStatement để hiểu rõ 
        try (Connection conn = DatabaseConnector.getConnection();
            PreparedStatement pstm = conn.prepareStatement(sql.toString())) {
            // Gán parameter cho prepared statement 
            if (excludedIds != null && !excludedIds.isEmpty()) {
                int paramIndex = 1; 
                for (Integer id : excludedIds) {
                    pstm.setInt(paramIndex++, id); 
                }
            }
            // Lấy kết quả 
            try (ResultSet rs = pstm.executeQuery()) {
                if (rs.next()) {
                    return new Vocabulary(
                        rs.getInt("id"),
                        rs.getString("phrase"),
                        rs.getInt("length")
                    );
                }
            } 
        } catch (Exception e) {
            e.printStackTrace(); 
            System.err.println("Lỗi khi lấy vocabulary ngẫu nhiên: " + e.getMessage());
        }
        return null; 
    }

}
