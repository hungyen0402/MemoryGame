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
                System.err.println("LOI: KHONG TIM THAY FILE 'config.properties' trong resources!");
                System.err.println("    → DAM BAO FILE NAM O: src/main/resources/config.properties");
                return;
            }

            properties.load(input);
            configLoaded = true;
            System.out.println("DA TAI FILE config.properties thanh cong!");

            // In thông tin (ẩn password)
            System.out.println("  • DB URL: " + properties.getProperty("db.url"));
            System.out.println("  • DB User: " + properties.getProperty("db.user"));
            System.out.println("  • DB Pass: " + (properties.getProperty("db.password") != null ? "******" : "null"));

        } catch (Exception ex) {
            System.err.println("LOI KHI DOC config.properties:");
            ex.printStackTrace();
        }
    }

    private static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static Connection getConnection() throws SQLException {
        if (!configLoaded) {
            throw new SQLException("Config chua duoc tai! Kiem tra file config.properties.");
        }

        String url = getProperty("db.url");
        String user = getProperty("db.user");
        String password = getProperty("db.password");

        if (url == null || user == null || password == null) {
            throw new SQLException("Thieu thong tin cau hinh DB: url, user hoac password trong config.properties");
        }

        // Load driver (MySQL 8+)
        if (!driverLoaded) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                System.out.println("DA TAI MySQL THANH CONG!");
                driverLoaded = true;
            } catch (ClassNotFoundException e) {
                System.err.println("LOI: Khong tim thay MySQL JDBC Driver!");
                System.err.println("    → Hay them mysql-connector-j-*.jar vao classpath!");
                throw new SQLException("MySQL Driver khong co!", e);
            }
        }

        System.out.println("DANG KET NOI TOI MySQL...");
        System.out.println("   → URL: " + url);
        System.out.println("   → User: " + user);

        Connection conn = DriverManager.getConnection(url, user, password);

        System.out.println("KET NOI DATABASE THANH CONG!");
        return conn;
    }

    // Test method (dùng để kiểm tra nhanh)
    public static void testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("TEST KET NOI: THANH CONG 100%!");
            System.out.println("   → Database: " + conn.getCatalog());
            System.out.println("   → Driver: " + conn.getMetaData().getDriverName());
        } catch (SQLException e) {
            System.err.println("TEST KET NOI: THAT BAI!");
            e.printStackTrace();
        }
    }
}