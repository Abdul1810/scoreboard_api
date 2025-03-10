package com.api.scoreboard.tournament;


import com.api.scoreboard.stats.StatsListener;
import com.api.scoreboard.commons.Match;
import com.api.scoreboard.match.MatchListener;
import com.api.util.Database;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@WebServlet("/api/tournaments")
public class TournamentServlet extends HttpServlet {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Object> jsonResponse = new HashMap<>();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Map<String, Object> data = objectMapper.readValue(request.getReader(), Map.class);
        List<Integer> teams = (List<Integer>) data.get("teams");
        String name = (String) data.get("name");
        System.out.println("Teams: " + teams);
        System.out.println("Name: " + name);

        if (name == null || name.isEmpty()) {
            jsonResponse.put("error", "Invalid name");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else if (teams.size() != 8) {
            jsonResponse.put("error", "Invalid number of teams");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else {
//            CREATE TABLE tournaments
//            (
//                    id         INT(11)      NOT NULL AUTO_INCREMENT PRIMARY KEY,
//                    name       VARCHAR(255) NOT NULL UNIQUE,
//                    created_at DATETIME     NOT NULL DEFAULT current_timestamp()
//            );
//
//            CREATE TABLE tournament_teams
//            (
//                    id           INT(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
//                    tournament_id INT(11) NOT NULL,
//                    team_id       INT(11) NOT NULL,
//                    created_at    DATETIME NOT NULL DEFAULT current_timestamp(),
//                    FOREIGN KEY (tournament_id) REFERENCES tournaments (id) ON DELETE CASCADE,
//                    FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
//                    UNIQUE (tournament_id, team_id)
//            );
//
//            CREATE TABLE tournament_matches
//            (
//                    id         INT(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
//                    tournament_id INT(11) NOT NULL,
//                    match_id   INT(11) NOT NULL,
//                    created_at DATETIME NOT NULL DEFAULT current_timestamp(),
//                    FOREIGN KEY (tournament_id) REFERENCES tournaments (id) ON DELETE CASCADE,
//                    FOREIGN KEY (match_id) REFERENCES matches (id) ON DELETE CASCADE,
//                    UNIQUE (tournament_id, match_id)
//            );
//            CREATE TABLE matches
//            (
//                id                    INT(11)                                NOT NULL AUTO_INCREMENT PRIMARY KEY,
//                team1_id              INT(11)                                NOT NULL,
//                team2_id              INT(11)                                NOT NULL,
//                is_completed          ENUM ('true', 'false')                 NOT NULL DEFAULT 'false',
//                winner                ENUM ('team1', 'team2', 'none', 'tie') NOT NULL DEFAULT 'none',
//                current_batting       ENUM ('team1', 'team2')                NOT NULL DEFAULT 'team1',
//                active_batsman_index  INT(11)                                NOT NULL DEFAULT 1,
//                passive_batsman_index INT(11)                                NOT NULL DEFAULT 2,
//                created_at            DATETIME                               NOT NULL DEFAULT current_timestamp(),
//                FOREIGN KEY (team1_id) REFERENCES teams (id) ON DELETE CASCADE,
//                FOREIGN KEY (team2_id) REFERENCES teams (id) ON DELETE CASCADE
//            );
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                conn = Database.getConnection();
                stmt = conn.prepareStatement("SELECT * FROM tournaments WHERE name = ?");
                stmt.setString(1, name);
                rs = stmt.executeQuery();
                if (rs.next()) {
                    jsonResponse.put("error", "Tournament already exists");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                    return;
                }

                stmt = conn.prepareStatement("INSERT INTO tournaments (name) VALUES (?)", PreparedStatement.RETURN_GENERATED_KEYS);
                stmt.setString(1, name);
                stmt.executeUpdate();

                rs = stmt.getGeneratedKeys();
                rs.next();
                int tournamentId = rs.getInt(1);
                stmt = conn.prepareStatement("INSERT INTO tournament_teams (tournament_id, team_id) VALUES (?, ?)");

                for (int teamId : teams) {
                    stmt.setInt(1, tournamentId);
                    stmt.setInt(2, teamId);
                    stmt.addBatch();
                }
                stmt.executeBatch();
                List<Integer> matchIds = new ArrayList<>();
                while (teams.size() > 1) {
                    int team1 = teams.remove(0);
                    int team2 = teams.remove(0);
                    matchIds.add(Match.create(conn, team1, team2, tournamentId));
                }

//                stmt = conn.prepareStatement("INSERT INTO tournament_matches (tournament_id, match_id) VALUES (?, ?)");
//                for (int matchId : matchIds) {
//                    stmt.setInt(1, tournamentId);
//                    stmt.setInt(2, matchId);
//                    stmt.addBatch();
//                }
//                stmt.executeBatch();

                MatchListener.fireMatchesUpdate();
                jsonResponse.put("tournamentId", tournamentId);
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            } catch (Exception e) {
                System.out.println("Database connection error: " + e.getMessage());
            } finally {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (stmt != null) {
                        stmt.close();
                    }
                    if (conn != null) {
                        conn.close();
                    }
                } catch (Exception e) {
                    System.out.println("Closing Error : " + e.getMessage());
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        String tId = request.getParameter("id");
        if (tId == null) {
            try {
                conn = Database.getConnection();
                String sql = "SELECT t.id AS tournament_id, t.name AS tournament_name, t.created_at AS tournament_created_at, " + "m.id AS match_id, team1.name AS team1_name, team2.name AS team2_name " + "FROM tournaments t " + "LEFT JOIN matches m ON t.id = m.tournament_id AND m.is_completed = 'false' " + "LEFT JOIN teams team1 ON m.team1_id = team1.id " + "LEFT JOIN teams team2 ON m.team2_id = team2.id " + "ORDER BY t.id, m.id";

                stmt = conn.prepareStatement(sql);
                rs = stmt.executeQuery();

                Map<Integer, Map<String, Object>> tournamentMap = new LinkedHashMap<>();
                while (rs.next()) {
                    int tournamentId = rs.getInt("tournament_id");
                    tournamentMap.putIfAbsent(tournamentId, new HashMap<>());
                    Map<String, Object> tournament = tournamentMap.get(tournamentId);

                    tournament.put("id", tournamentId);
                    tournament.put("name", rs.getString("tournament_name"));
                    tournament.put("created_at", rs.getString("tournament_created_at"));

                    List<String> matches = (List<String>) tournament.getOrDefault("matches", new ArrayList<>());
                    String team1 = rs.getString("team1_name");
                    String team2 = rs.getString("team2_name");

                    if (team1 != null && team2 != null) {
                        matches.add(team1 + " vs " + team2);
                    }

                    tournament.put("matches", matches);
                }

                List<Map<String, Object>> tournamentsList = new ArrayList<>(tournamentMap.values());
                response.setContentType("application/json");
                response.getWriter().write(objectMapper.writeValueAsString(Collections.singletonMap("tournaments", tournamentsList)));
            } catch (Exception e) {
                System.out.println("Database connection error: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                jsonResponse.put("error", "Internal server error");
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (stmt != null) stmt.close();
                    if (conn != null) conn.close();
                } catch (Exception e) {
                    System.out.println("Closing Error: " + e.getMessage());
                }
            }
        } else {
            try {
                conn = Database.getConnection();
                String query = "SELECT t.id AS tournament_id, " +
                        "t.name AS tournament_name, " +
                        "t.status, " +
                        "t.created_at, " +
                        "tm.name AS winning_team_name " +
                        "FROM tournaments t " +
                        "LEFT JOIN tournament_teams tt ON t.id = tt.tournament_id AND tt.status = 'winner' " +
                        "LEFT JOIN teams tm ON tt.team_id = tm.id " +
                        "WHERE t.id = ?";
                stmt = conn.prepareStatement(query);
                stmt.setString(1, tId);
                rs = stmt.executeQuery();

                if (rs.next()) {
                    Map<String, Object> tournament = new HashMap<>();
                    tournament.put("id", rs.getInt("tournament_id"));
                    tournament.put("name", rs.getString("tournament_name"));
                    tournament.put("status", rs.getString("status"));
                    tournament.put("created_at", rs.getString("created_at"));

                    if (tournament.getOrDefault("status", "ongoing").equals("completed")) {
                        tournament.put("winner", rs.getString("winning_team_name"));
                    }

                    List<Map<String, Object>> matches = new ArrayList<>();
                    stmt = conn.prepareStatement("SELECT m.id, m.winner, m.is_completed, team1.name AS team1_name, team2.name AS team2_name, " + "CASE " + "   WHEN m.winner = 'team1' THEN team1.name " + "   WHEN m.winner = 'team2' THEN team2.name " + "   WHEN m.winner = 'tie' THEN 'Match Tied' " + "   ELSE 'Not decided' " + "END AS winner_name " + "FROM matches m " + "JOIN teams team1 ON m.team1_id = team1.id " + "JOIN teams team2 ON m.team2_id = team2.id " + "WHERE m.tournament_id = ?");
                    stmt.setString(1, tId);
                    rs = stmt.executeQuery();

                    while (rs.next()) {
                        Map<String, Object> match = new HashMap<>();
                        match.put("id", rs.getInt("id"));
                        match.put("name", rs.getString("team1_name") + " vs " + rs.getString("team2_name"));
                        match.put("winner", rs.getString("winner_name"));
                        match.put("is_completed", rs.getString("is_completed"));
                        matches.add(match);
                    }

                    tournament.put("matches", matches);
                    response.getWriter().write(objectMapper.writeValueAsString(tournament));
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    jsonResponse.put("error", "Tournament not found");
                    response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
                }
            } catch (Exception e) {
                System.out.println("Database connection error: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                jsonResponse.put("error", "Internal server error");
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (stmt != null) stmt.close();
                    if (conn != null) conn.close();
                } catch (Exception e) {
                    System.out.println("Closing Error: " + e.getMessage());
                }
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        String tId = request.getParameter("id");
        if (tId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.put("error", "Tournament ID is required");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            return;
        }

        try {
            conn = Database.getConnection();
            stmt = conn.prepareStatement("SELECT * FROM tournaments WHERE id = ?");
            stmt.setString(1, tId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                stmt = conn.prepareStatement("SELECT id FROM matches WHERE tournament_id = ?");
                stmt.setString(1, tId);
                rs = stmt.executeQuery();
                while (rs.next()) {
                    StatsListener.fireStatsRemove(rs.getString("id"));
                }
                stmt = conn.prepareStatement("DELETE FROM matches WHERE tournament_id = ?");
                stmt.setString(1, tId);
                stmt.executeUpdate();

                stmt = conn.prepareStatement("DELETE FROM tournaments WHERE id = ?");
                stmt.setString(1, tId);
                stmt.executeUpdate();

                MatchListener.fireMatchesUpdate();
                response.getWriter().write(objectMapper.writeValueAsString(Collections.singletonMap("message", "Tournament deleted")));
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                jsonResponse.put("error", "Tournament not found");
                response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
            }
        } catch (Exception e) {
            System.out.println("Database connection error: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.put("error", "Internal server error");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                System.out.println("Error while closing : " + e.getMessage());
            }
        }
    }
}
