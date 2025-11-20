package org.kpi.dao;

import org.kpi.model.CommandLog;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class CommandLogDAO {

    // SQL запит для вставки
    private static final String INSERT_SQL = "INSERT INTO command_history (command_text, executed_at) VALUES (?, ?)";

    public void save(CommandLog log) {
        // Отримуємо з'єднання через наш Singleton
        Connection conn = DBConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {

            // 1. Підставляємо текст команди
            pstmt.setString(1, log.getCommandText());

            // 2. Підставляємо час виконання
            pstmt.setTimestamp(2, Timestamp.valueOf(log.getExecutedAt()));

            // 3. Виконуємо запит
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Помилка при збереженні історії: " + e.getMessage());
            // Можливо, тут ми б хотіли кинути виняток, але поки просто виводимо помилку
        }
    }
}