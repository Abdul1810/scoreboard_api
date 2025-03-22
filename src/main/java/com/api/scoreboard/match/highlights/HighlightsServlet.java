package com.api.scoreboard.match.highlights;

import com.api.util.Database;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/highlights")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 10,
        maxFileSize = 1024 * 1024 * 100,
        maxRequestSize = 1024 * 1024 * 150
)
public class HighlightsServlet extends HttpServlet {
    private static final String UPLOAD_DIRECTORY = "\\uploads\\highlights";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> jsonResponse = new HashMap<>();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        String matchId = request.getParameter("id");
        if (matchId == null || matchId.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = new Database().getConnection();
            stmt = conn.prepareStatement("SELECT * FROM matches WHERE id = ?");
            stmt.setString(1, matchId);
            rs = stmt.executeQuery();

            if (!rs.next()) {
                jsonResponse.put("status", "error");
                jsonResponse.put("message", "Match not found.");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            } else {
                if (rs.getString("highlights_path") != null) {
                    jsonResponse.put("status", "error");
                    jsonResponse.put("message", "Highlights already uploaded.");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }
            File uploadDir = new File(UPLOAD_DIRECTORY);
            if (!uploadDir.exists()) {
                uploadDir.mkdir();
            }

            Part filePart = request.getPart("file");
            if (filePart != null) {
                String fileName = extractFileName(filePart);
                if (fileName != null && !fileName.isEmpty()) {
                    String filePath = UPLOAD_DIRECTORY + File.separator + fileName;
                    try (InputStream inputStream = filePart.getInputStream();
                         FileOutputStream outputStream = new FileOutputStream(filePath)) {

                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }

                    jsonResponse.put("status", "success");
                    jsonResponse.put("message", "File uploaded successfully.");
                    jsonResponse.put("fileName", fileName);
                    jsonResponse.put("filePath", filePath);
                    response.setStatus(HttpServletResponse.SC_OK);

                    stmt = conn.prepareStatement("UPDATE matches SET highlights_path = ? WHERE id = ?");
                    stmt.setString(1, fileName);
                    stmt.setString(2, matchId);
                    stmt.executeUpdate();
                } else {
                    jsonResponse.put("status", "error");
                    jsonResponse.put("message", "Invalid file.");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
            } else {
                jsonResponse.put("status", "error");
                jsonResponse.put("message", "File part is missing.");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (Exception e) {
            System.out.println("Error uploading file: " + e.getMessage());
            jsonResponse.put("status", "error");
            jsonResponse.put("message", "Error uploading file: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        try {
            objectMapper.writeValue(response.getWriter(), jsonResponse);
        } catch (IOException e) {
            System.out.println("Error writing response: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String matchId = request.getParameter("id");
        if (matchId == null || matchId.isEmpty()) {
            response.setStatus(400);
            jsonResponse.put("message", "Match ID is required");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            return;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        ResultSet rs1 = null;
        try {
            conn = new Database().getConnection();
            String query = "SELECT * FROM matches WHERE id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(matchId));
            rs = stmt.executeQuery();

            if (rs.next()) {
                query = "    SELECT m.id, m.team1_id, m.team2_id, m.is_completed" +
                        "    FROM matches m" +
                        "    WHERE m.tournament_id = ?" +
                        "    AND m.is_completed = 'false'" +
                        "    ORDER BY m.id";
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, rs.getInt("tournament_id"));
                rs1 = stmt.executeQuery();
                List<Integer> tournamentMatches = new ArrayList<>();

                while (rs1.next()) {
                    tournamentMatches.add(rs1.getInt("id"));
                }

                if (!tournamentMatches.isEmpty() && tournamentMatches.contains(rs.getInt("id"))) {
                    if (tournamentMatches.get(0) != rs.getInt("id")) {
                        response.setStatus(403);
                        jsonResponse.put("message", "Match cannot be accessed");
                        response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                        return;
                    }
                }

                Map<String, Integer> matchIds = new HashMap<>();
                matchIds.put("id", rs.getInt("id"));
                matchIds.put("team1_id", rs.getInt("team1_id"));
                matchIds.put("team2_id", rs.getInt("team2_id"));

                Map<String, Object> match = new HashMap<>();
                match.put("id", String.valueOf(matchIds.get("id")));
                match.put("is_completed", rs.getString("is_completed"));
                match.put("highlights_path", rs.getString("highlights_path"));
                match.put("winner", rs.getString("winner"));
                query = "SELECT t.name, p.id as player_id, p.name as player_name, ps.balls FROM teams t " +
                        "JOIN team_players tp ON t.id = tp.team_id " +
                        "JOIN players p ON tp.player_id = p.id " +
                        "LEFT JOIN player_stats ps ON ps.team_id = t.id AND ps.player_id = p.id " +
                        "AND ps.match_id = ? " +
                        "WHERE t.id = ?";
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, matchIds.get("id"));
                stmt.setInt(2, matchIds.get("team1_id"));
                rs = stmt.executeQuery();

                List<Map<String, Object>> player1Map = new ArrayList<>();
                List<Map<String, Object>> player2Map = new ArrayList<>();
                Map<Integer, String> timestamps = new HashMap<>();

                int sum = 0;
                if (rs.next()) {
                    match.put("team1", rs.getString("name"));
                    do {
                        Map<String, Object> player = new HashMap<>();
                        player.put("id", rs.getInt("player_id"));
                        player.put("name", rs.getString("player_name"));
                        player.put("balls", rs.getInt("balls"));
                        player.put("startTime", sum);
                        player1Map.add(player);
                        sum += rs.getInt("balls") * 5;
                    } while (rs.next());
                }

                stmt.setInt(2, matchIds.get("team2_id"));
                rs = stmt.executeQuery();
                if (rs.next()) {
                    match.put("team2", rs.getString("name"));
                    do {
                        Map<String, Object> player = new HashMap<>();
                        player.put("id", rs.getInt("player_id"));
                        player.put("name", rs.getString("player_name"));
                        player.put("balls", rs.getInt("balls"));
                        player.put("startTime", sum);
                        player2Map.add(player);
                        sum += rs.getInt("balls") * 5;
                    } while (rs.next());
                }

                sum = 0;
                for (Map<String, Object> stringObjectMap : player1Map) {
                    sum += (int) stringObjectMap.get("balls");
                    timestamps.put(sum * 5, (String) stringObjectMap.get("name"));
                }
                for (Map<String, Object> stringObjectMap : player2Map) {
                    sum += (int) stringObjectMap.get("balls");
                    timestamps.put(sum * 5, (String) stringObjectMap.get("name"));
                }

                match.put("timestamps", timestamps);
                match.put("team1_players", player1Map);
                match.put("team2_players", player2Map);
                response.getWriter().write(objectMapper.writeValueAsString(match));
            } else {
                response.setStatus(404);
                jsonResponse.put("message", "Match not found");
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
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
                if (rs1 != null) rs1.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    private String extractFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        for (String content : contentDisposition.split(";")) {
            if (content.trim().startsWith("filename")) {
                String originalFile = content.substring(content.indexOf("=") + 2, content.length() - 1);
                String fileExtension = originalFile.substring(originalFile.lastIndexOf("."));
                String baseName = originalFile.substring(0, originalFile.lastIndexOf("."));
                return baseName + "_" + System.currentTimeMillis() + fileExtension;
            }
        }
        return null;
    }
}
