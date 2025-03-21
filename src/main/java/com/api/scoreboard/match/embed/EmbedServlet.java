package com.api.scoreboard.match.embed;

import com.api.util.Database;
import com.api.util.Validator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.*;
import java.util.*;

@WebServlet("/api/embed")
public class EmbedServlet extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();
//    CREATE TABLE embeds
//    (
//        id         INT(11)      NOT NULL AUTO_INCREMENT PRIMARY KEY,
//        user_id    INT(11)      NOT NULL,
//        match_id   INT(11)      NOT NULL,
//        embed_code TEXT         NOT NULL,
//        created_at DATETIME     NOT NULL DEFAULT current_timestamp(),
//        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
//        FOREIGN KEY (match_id) REFERENCES matches (id) ON DELETE CASCADE
//        UNIQUE (user_id, match_id)
//    );

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Connection conn = null;
        PreparedStatement insertEmbedStmt = null;
        ResultSet rs = null;
        Map<String, String> jsonResponse = new HashMap<>();

        try {
            conn = new Database().getConnection();
            String matchId = request.getParameter("id");
            System.out.println("Match ID: " + matchId);
            int userId = (int) request.getSession().getAttribute("uid");
            System.out.println("User ID: " + userId);

            if (Validator.isEmpty(matchId)) {
                jsonResponse.put("message", "Invalid requestData data");
                response.setStatus(400);
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            String query = "SELECT * FROM matches WHERE id = ?";
            insertEmbedStmt = conn.prepareStatement(query);
            insertEmbedStmt.setInt(1, Integer.parseInt(matchId));
            rs = insertEmbedStmt.executeQuery();

            if (!rs.next()) {
                jsonResponse.put("message", "Match not found");
                response.setStatus(404);
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            query = "SELECT * FROM embeds WHERE user_id = ? AND match_id = ?";
            insertEmbedStmt = conn.prepareStatement(query);
            insertEmbedStmt.setInt(1, userId);
            insertEmbedStmt.setInt(2, Integer.parseInt(matchId));
            rs = insertEmbedStmt.executeQuery();

            if (rs.next()) {
                jsonResponse.put("message", "success");
                jsonResponse.put("embed_code", rs.getString("embed_code"));
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            String embedCode = UUID.randomUUID().toString();
            query = "INSERT INTO embeds (user_id, match_id, embed_code) VALUES (?, ?, ?)";
            insertEmbedStmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            insertEmbedStmt.setInt(1, userId);
            insertEmbedStmt.setInt(2, Integer.parseInt(matchId));
            insertEmbedStmt.setString(3, embedCode);
            insertEmbedStmt.executeUpdate();

            rs = insertEmbedStmt.getGeneratedKeys();
            if (rs.next()) {
                jsonResponse.put("message", "success");
                jsonResponse.put("embed_code", embedCode);
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            } else {
                jsonResponse.put("message", "Error creating requestData");
                response.setStatus(500);
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            }
        } catch (SQLException e) {
            jsonResponse.put("message", "Database error: " + e.getMessage());
            response.setStatus(500);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } catch (NumberFormatException e) {
            jsonResponse.put("message", "Invalid user ID or match ID format");
            response.setStatus(400);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } finally {
            try {
                if (rs != null) rs.close();
                if (insertEmbedStmt != null) insertEmbedStmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String matchId = request.getParameter("id");
        int userId = (int) request.getSession().getAttribute("uid");
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Map<String, String> jsonResponse = new HashMap<>();

        try {
            conn = new Database().getConnection();
            if (matchId != null) {
                String query = "SELECT * FROM embeds WHERE match_id = ? AND user_id = ?";
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, Integer.parseInt(matchId));
                stmt.setInt(2, userId);
                rs = stmt.executeQuery();

                if (rs.next()) {
                    Map<String, String> embed = new HashMap<>();
                    embed.put("id", matchId);
                    embed.put("embed_code", rs.getString("embed_code"));
                    response.getWriter().write(objectMapper.writeValueAsString(embed));
                } else {
                    jsonResponse.put("message", "Embed not found");
                    response.setStatus(404);
                    response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                }
            } else {
                /*
                SELECT e.match_id, e.embed_code, t1.name AS team1, t2.name AS team2
                FROM embeds e
                JOIN matches m ON e.match_id = m.id
                JOIN teams t1 ON m.team1_id = t1.id
                JOIN teams t2 ON m.team2_id = t2.id
                WHERE e.user_id = 6
                 */
                String query = "SELECT e.match_id, e.embed_code, t1.name AS team1, t2.name AS team2" +
                        " FROM embeds e" +
                        " JOIN matches m ON e.match_id = m.id" +
                        " JOIN teams t1 ON m.team1_id = t1.id" +
                        " JOIN teams t2 ON m.team2_id = t2.id" +
                        " WHERE e.user_id = ?";
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, userId);
                rs = stmt.executeQuery();

                List<Map<String, String>> embeds = new ArrayList<>();
                while (rs.next()) {
                    Map<String, String> embed = new HashMap<>();
                    embed.put("id", String.valueOf(rs.getInt("match_id")));
                    embed.put("embed_code", rs.getString("embed_code"));
                    embed.put("team1", rs.getString("team1"));
                    embed.put("team2", rs.getString("team2"));
                    embeds.add(embed);
                }
                response.getWriter().write(objectMapper.writeValueAsString(Collections.singletonMap("embeds", embeds)));
            }
        } catch (SQLException e) {
            jsonResponse.put("message", "Database error: " + e.getMessage());
            response.setStatus(500);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } catch (NumberFormatException e) {
            jsonResponse.put("message", "Invalid match ID format");
            response.setStatus(400);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.out.println("Error closing resource: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String matchId = request.getParameter("id");
        int userId = (int) request.getSession().getAttribute("uid");
        Connection conn = null;
        PreparedStatement stmt = null;
        Map<String, String> jsonResponse = new HashMap<>();

        try {
            conn = new Database().getConnection();
            String query = "SELECT * FROM embeds WHERE match_id = ? AND user_id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(matchId));
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                jsonResponse.put("message", "Embed not found");
                response.setStatus(404);
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            String embedCode = rs.getString("embed_code");
            query = "DELETE FROM embeds WHERE match_id = ? AND user_id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(matchId));
            stmt.setInt(2, userId);
            stmt.executeUpdate();

            EmbedListener.fireEmbedRemove(embedCode);
            jsonResponse.put("message", "success");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } catch (SQLException e) {
            jsonResponse.put("message", "Database error: " + e.getMessage());
            response.setStatus(500);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } catch (NumberFormatException e) {
            jsonResponse.put("message", "Invalid match ID format");
            response.setStatus(400);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
}
