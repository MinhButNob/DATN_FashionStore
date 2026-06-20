package com.example.backend_fashionstore.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestConnection {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/test";
        String user = "postgres";
        String password = "1234$";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Kết nối thành công!");
        } catch (SQLException e) {
            System.out.println(" Lỗi: " + e.getMessage());
        }
    }
}