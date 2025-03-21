package com.api.scoreboard.team;

import com.api.scoreboard.match.embed.EmbedListener;
import com.api.scoreboard.stats.StatsListener;
import com.api.scoreboard.match.MatchListener;
import com.api.util.Database;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.*;
import java.util.*;

@WebServlet("/api/teams")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 10,
        maxFileSize = 1024 * 1024 * 100,
        maxRequestSize = 1024 * 1024 * 150
)
public class TeamServlet extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> jsonResponse = new HashMap<>();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> jsonResponse = new HashMap<>();

        try {
            if (!request.getContentType().toLowerCase().startsWith("multipart/")) {
                response.setStatus(400);
                jsonResponse.put("message", "Invalid request type");
                response.getWriter().write(new ObjectMapper().writeValueAsString(jsonResponse));
                return;
            }

            String teamName = request.getParameter("name");
            String[] players = request.getParameterValues("players");

            if (teamName == null || teamName.trim().isEmpty()) {
                response.setStatus(400);
                jsonResponse.put("message", "Invalid team name");
                response.getWriter().write(new ObjectMapper().writeValueAsString(jsonResponse));
                return;
            }

            if (players == null || players.length < 11) {
                response.setStatus(400);
                jsonResponse.put("message", "Invalid number of players");
                response.getWriter().write(new ObjectMapper().writeValueAsString(jsonResponse));
                return;
            }

            for (int i = 0; i < 11; i++) {
                if (players[i] == null || players[i].trim().isEmpty()) {
                    response.setStatus(400);
                    jsonResponse.put("message", "Invalid player name at index " + i);
                    response.getWriter().write(new ObjectMapper().writeValueAsString(jsonResponse));
                    return;
                }
            }

            Part teamLogoPart = request.getPart("logo");
            List<Part> playerAvatarParts = new ArrayList<>();
            for (Part part : request.getParts()) {
                if ("avatars".equals(part.getName())) {
                    playerAvatarParts.add(part);
                }
            }

            String teamLogoPath = saveImageFile(teamLogoPart, "teams");
            List<String> playerAvatarPaths = new ArrayList<>();
            for (Part part : playerAvatarParts) {
                playerAvatarPaths.add(saveImageFile(part, "players"));
            }

            try (Connection conn = new Database().getConnection()) {
                if (isTeamExists(conn, teamName)) {
                    jsonResponse.put("message", "Team already exists");
                    response.setStatus(400);
                    response.getWriter().write(new ObjectMapper().writeValueAsString(jsonResponse));
                    return;
                }

                List<Integer> playerIds = insertPlayers(conn, players, playerAvatarPaths);
                int userId = (int) request.getSession().getAttribute("uid");
                int teamId = insertTeam(conn, teamName, teamLogoPath, userId);
                linkPlayersToTeam(conn, teamId, playerIds);

                jsonResponse.put("message", "Team created successfully");
                response.setStatus(201);
                response.getWriter().write(new ObjectMapper().writeValueAsString(jsonResponse));
            }
        } catch (Exception e) {
            jsonResponse.put("message", "Server error: " + e.getMessage());
            response.setStatus(500);
            response.getWriter().write(new ObjectMapper().writeValueAsString(jsonResponse));
        }
    }

    private String saveImageFile(Part part, String type) throws IOException {
        if (part == null || part.getSize() <= 0) {
            return "placeholder.png";
        }

        String uploadDir = "F:\\Code\\JAVA\\zoho_training\\uploads\\" + type + "\\";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileExtension = getFileExtension(part);
        String fileName = System.currentTimeMillis() + fileExtension;
        File file = new File(dir, fileName);

        BufferedImage image = ImageIO.read(part.getInputStream());
        if (image != null) {
            try (FileOutputStream fos = new FileOutputStream(file);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
                ImageOutputStream ios = ImageIO.createImageOutputStream(bos);
                writer.setOutput(ios);

                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(0.5f);
                }

                writer.write(null, new IIOImage(image, null, null), param);
                ios.close();
                writer.dispose();
            }
        }

        return fileName;
    }

    private String getFileExtension(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        for (String content : contentDisp.split(";")) {
            if (content.trim().startsWith("filename")) {
                String fileName = content.substring(content.indexOf("=") + 2, content.length() - 1);
                return fileName.substring(fileName.lastIndexOf("."));
            }
        }
        return "";
    }

    private boolean isTeamExists(Connection conn, String teamName) throws SQLException {
        String checkQuery = "SELECT id FROM teams WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(checkQuery)) {
            stmt.setString(1, teamName);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    private List<Integer> insertPlayers(Connection conn, String[] players, List<String> avatarPaths) throws SQLException {
        String insertPlayerQuery = "INSERT INTO players (name, avatar) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertPlayerQuery, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < players.length; i++) {
                stmt.setString(1, players[i]);
                stmt.setString(2, avatarPaths.size() > i ? avatarPaths.get(i) : "placeholder.png");
                stmt.addBatch();
            }
            stmt.executeBatch();

            List<Integer> playerIds = new ArrayList<>();
            ResultSet keys = stmt.getGeneratedKeys();
            while (keys.next()) {
                playerIds.add(keys.getInt(1));
            }
            return playerIds;
        }
    }

    private int insertTeam(Connection conn, String teamName, String logoPath, int userId) throws SQLException {
        String insertTeamQuery = "INSERT INTO teams (name, logo, user_id) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertTeamQuery, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, teamName);
            stmt.setString(2, logoPath);
            stmt.setInt(3, userId);
            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                return keys.getInt(1);
            } else {
                throw new SQLException("Failed to insert team");
            }
        }
    }

    private void linkPlayersToTeam(Connection conn, int teamId, List<Integer> playerIds) throws SQLException {
        String insertTeamPlayerQuery = "INSERT INTO team_players (team_id, player_id, player_position) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertTeamPlayerQuery)) {
            for (int i = 0; i < playerIds.size(); i++) {
                stmt.setInt(1, teamId);
                stmt.setInt(2, playerIds.get(i));
                stmt.setInt(3, i + 1);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String matchId = request.getParameter("id");
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int userId = (int) request.getSession().getAttribute("uid");

        try {
            conn = new Database().getConnection();

            if (matchId == null) {
                String query = "SELECT t.id, t.name, t.logo FROM teams t WHERE t.user_id = ?";
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, userId);
                rs = stmt.executeQuery();

                List<Map<String, Object>> teams = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> team = new HashMap<>();
                    int teamId = rs.getInt("id");
                    team.put("id", teamId);
                    team.put("name", rs.getString("name"));
                    team.put("logo", "http://localhost:8080/image/teams?name=" + rs.getString("logo") + "&q=low");

                    String playerQuery = "SELECT p.name FROM team_players tp JOIN players p ON tp.player_id = p.id WHERE tp.team_id = ?";
                    try (PreparedStatement playerStmt = conn.prepareStatement(playerQuery)) {
                        playerStmt.setInt(1, teamId);
                        ResultSet playerRs = playerStmt.executeQuery();
                        List<String> players = new ArrayList<>();
                        while (playerRs.next()) {
                            players.add(playerRs.getString("name"));
                        }
                        team.put("players", players);
                    }
                    teams.add(team);
                }
                response.getWriter().write(objectMapper.writeValueAsString(teams));
            } else {
                String query = "SELECT t.id, t.name, t.logo FROM teams t WHERE t.id = ? AND t.user_id = ?";
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, Integer.parseInt(matchId));
                stmt.setInt(2, userId);
                rs = stmt.executeQuery();

                if (rs.next()) {
                    Map<String, Object> team = new HashMap<>();
                    int teamId = rs.getInt("id");
                    team.put("id", teamId);
                    team.put("name", rs.getString("name"));
                    team.put("logo", "http://localhost:8080/image/teams?name=" + rs.getString("logo") + "&q=medium");

                    String playerQuery = "SELECT p.name, p.avatar FROM team_players tp JOIN players p ON tp.player_id = p.id WHERE tp.team_id = ?";
                    try (PreparedStatement playerStmt = conn.prepareStatement(playerQuery)) {
                        playerStmt.setInt(1, teamId);
                        ResultSet playerRs = playerStmt.executeQuery();
                        List<Map<String, String>> players = new ArrayList<>();
                        while (playerRs.next()) {
                            Map<String, String> player = new HashMap<>();
                            player.put("name", playerRs.getString("name"));
                            player.put("avatar", "http://localhost:8080/image/players?name=" + playerRs.getString("avatar") + "&q=low");
                            players.add(player);
                        }
                        team.put("players", players);
                    }

                    response.getWriter().write(objectMapper.writeValueAsString(team));
                } else {
                    jsonResponse.put("message", "Team not found");
                    response.setStatus(404);
                    response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                }
            }
        } catch (Exception e) {
            jsonResponse.put("message", "Database error: " + e.getMessage());
            response.setStatus(500);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String id = request.getParameter("id");
        if (id == null || id.trim().isEmpty()) {
            response.setStatus(400);
            jsonResponse.put("message", "Invalid team ID");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            return;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int userId = (int) request.getSession().getAttribute("uid");

        try {
            conn = new Database().getConnection();
            String checkOwnershipQuery = "SELECT id FROM teams WHERE id = ? AND user_id = ?";
            stmt = conn.prepareStatement(checkOwnershipQuery);
            stmt.setInt(1, Integer.parseInt(id));
            stmt.setInt(2, userId);
            rs = stmt.executeQuery();

            if (!rs.next()) {
                jsonResponse.put("message", "Team not found");
                response.setStatus(404);
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                return;
            }

            String deleteTeamPlayersQuery = "DELETE FROM team_players WHERE team_id = ?";
            stmt = conn.prepareStatement(deleteTeamPlayersQuery);
            stmt.setInt(1, Integer.parseInt(id));
            stmt.executeUpdate();

            String deleteTeamQuery = "DELETE FROM teams WHERE id = ?";
            stmt = conn.prepareStatement(deleteTeamQuery);
            stmt.setInt(1, Integer.parseInt(id));
            stmt.executeUpdate();

            List<Integer> matchIds = new ArrayList<>();
            String fetchMatchesQuery = "SELECT id FROM matches WHERE team1_id = ? OR team2_id = ?";
            stmt = conn.prepareStatement(fetchMatchesQuery);
            stmt.setInt(1, Integer.parseInt(id));
            stmt.setInt(2, Integer.parseInt(id));
            rs = stmt.executeQuery();

            while (rs.next()) {
                matchIds.add(rs.getInt("id"));
            }

            String deleteMatchesQuery = "DELETE FROM matches WHERE team1_id = ? OR team2_id = ?";
            stmt = conn.prepareStatement(deleteMatchesQuery);
            stmt.setInt(1, Integer.parseInt(id));
            stmt.setInt(2, Integer.parseInt(id));
            stmt.executeUpdate();

            String deletePlayerStatsQuery = "DELETE FROM player_stats WHERE match_id = ?";
            for (int matchId : matchIds) {
                stmt = conn.prepareStatement(deletePlayerStatsQuery);
                stmt.setInt(1, matchId);
                stmt.executeUpdate();
            }

            MatchListener.fireMatchesUpdate(userId);
            for (int matchId : matchIds) {
                StatsListener.fireStatsRemove(String.valueOf(matchId));
                EmbedListener.fireEmbedRemove(String.valueOf(matchId));
            }
            jsonResponse.put("message", "Team deleted successfully");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } catch (Exception e) {
            jsonResponse.put("message", "Database error: " + e.getMessage());
            response.setStatus(500);
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
}
