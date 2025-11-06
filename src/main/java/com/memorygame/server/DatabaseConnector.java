package com.memorygame.server;
import java.io.InputStream; 
import java.sql.Connection;
import java.sql.DriverManager; 
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnector {
    private static Properties properties = new Properties();
    
    static {
        try (InputStream input = DatabaseConnector.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.err.println("Lỗi nghiêm trọng: Không tìm thấy file 'config.properties' trong src/main/resources"); 
            } else {
                properties.load(input); 
            }
        } catch (Exception ex) {
            ex.printStackTrace(); 
        }
    }

    private static String getProperty(String key) {
        return properties.getProperty(key); 
    }

    public static Connection getConnection() throws SQLException {
        String url = getProperty("db.url"); 
        String user = getProperty("db.user"); 
        String password = getProperty("db.password"); 
        if (url == null || user == null || password == null) {
            throw new SQLException("Thiếu thông tin cấu hình SQL trong config.properties");
        }
        return DriverManager.getConnection(url, user, password); 
    }
}
