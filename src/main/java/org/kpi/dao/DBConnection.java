package org.kpi.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static DBConnection instance;
    private Connection connection;

    // !!! ВКАЖИ ТУТ СВІЙ ПАРОЛЬ ВІД MYSQL !!!
    private static final String URL = "jdbc:mysql://localhost:3306/powershell_terminal";
    private static final String USER = "root";
    private static final String PASSWORD = "localhost";

    private DBConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("--- MySQL Connection Successful ---");
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("ПОМИЛКА: Не вдалося підключитися до MySQL. Перевір пароль і запуск сервера.");
            throw new RuntimeException("DB Connection Failed", e);
        }
    }

    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}