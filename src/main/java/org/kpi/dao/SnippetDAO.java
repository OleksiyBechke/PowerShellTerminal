package org.kpi.dao;

import org.kpi.model.Snippet;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SnippetDAO {

    private static final String SELECT_ALL = "SELECT id, title, command_body, description FROM snippets ORDER BY title";
    private static final String INSERT_SQL = "INSERT INTO snippets (title, command_body, description) VALUES (?, ?, ?)";
    private static final String DELETE_SQL = "DELETE FROM snippets WHERE id = ?";

    public List<Snippet> findAll() {
        List<Snippet> snippets = new ArrayList<>();
        Connection conn = DBConnection.getInstance().getConnection();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL)) {

            while (rs.next()) {
                Snippet snippet = new Snippet();
                snippet.setId(rs.getInt("id"));
                snippet.setTitle(rs.getString("title"));
                snippet.setCommandBody(rs.getString("command_body"));
                snippet.setDescription(rs.getString("description"));
                snippets.add(snippet);
            }
        } catch (SQLException e) {
            System.err.println("Помилка читання сніпетів: " + e.getMessage());
        }
        return snippets;
    }

    public void save(Snippet snippet) {
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {
            pstmt.setString(1, snippet.getTitle());
            pstmt.setString(2, snippet.getCommandBody());
            pstmt.setString(3, snippet.getDescription());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Помилка збереження сніпета: " + e.getMessage());
        }
    }

    public void delete(int id) {
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(DELETE_SQL)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            System.out.println("Сніпет ID " + id + " видалено.");
        } catch (SQLException e) {
            System.err.println("Помилка видалення сніпета: " + e.getMessage());
        }
    }
}