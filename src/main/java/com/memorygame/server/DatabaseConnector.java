package com.memorygame.server;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnector {
    private static final Properties properties = new Properties();
    private static boolean configLoaded = false;
    private static boolean driverLoaded = false;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try (InputStream input = DatabaseConnector.class.getClassLoader()
                .getResourceAsStream("config.properties")) {

            if (input == null) {
                System.err.println("LỖI: Không tìm thấy file 'config.properties' trong resources!");
                System.err.println("    → Đảm bảo file nằm ở: src/main/resources/config.properties");
                return;
            }

            properties.load(input);
            configLoaded = true;
            System.out.println("ĐÃ TẢI file config.properties thành công!");

            // In thông tin (ẩn password)
            System.out.println("  • DB URL: " + properties.getProperty("db.url"));
            System.out.println("  • DB User: " + properties.getProperty("db.user"));
            System.out.println("  • DB Pass: " + (properties.getProperty("db.password") != null ? "******" : "null"));

        } catch (Exception ex) {
            System.err.println("LỖI khi đọc config.properties:");
            ex.printStackTrace();
        }
    }

    private static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static Connection getConnection() throws SQLException {
        if (!configLoaded) {
            throw new SQLException("Config chưa được tải! Kiểm tra file config.properties.");
        }

        String url = getProperty("db.url");
        String user = getProperty("db.user");
        String password = getProperty("db.password");

        if (url == null || user == null || password == null) {
            throw new SQLException("Thiếu thông tin cấu hình DB: url, user hoặc password trong config.properties");
        }

        // Load driver (MySQL 8+)
        if (!driverLoaded) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                System.out.println("ĐÃ TẢI MySQL JDBC Driver thành công!");
                driverLoaded = true;
            } catch (ClassNotFoundException e) {
                System.err.println("LỖI: Không tìm thấy MySQL JDBC Driver!");
                System.err.println("    → Hãy thêm mysql-connector-j-*.jar vào classpath!");
                throw new SQLException("MySQL Driver không có!", e);
            }
        }

        System.out.println("ĐANG KẾT NỐI tới MySQL...");
        System.out.println("   → URL: " + url);
        System.out.println("   → User: " + user);

        Connection conn = DriverManager.getConnection(url, user, password);

        System.out.println("KẾT NỐI DATABASE THÀNH CÔNG!");
        return conn;
    }

    // Test method (dùng để kiểm tra nhanh)
    public static void testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("TEST KẾT NỐI: THÀNH CÔNG 100%!");
            System.out.println("   → Database: " + conn.getCatalog());
            System.out.println("   → Driver: " + conn.getMetaData().getDriverName());
        } catch (SQLException e) {
            System.err.println("TEST KẾT NỐI: THẤT BẠI!");
            e.printStackTrace();
        }
    }
}