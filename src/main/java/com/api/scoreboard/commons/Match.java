package com.api.scoreboard.commons;

import com.api.util.Database;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.*;
import java.util.*;

public class Match {
    public static int create(Connection conn, int userId, int team1Id, int team2Id) throws Exception {
        return create(conn, userId, team1Id, team2Id, -1);
    }

    public static int create(Connection conn, int userId, int team1Id, int team2Id, int tournamentId) throws Exception {
        String insertMatchQuery;
        PreparedStatement insertMatchStmt;
        ResultSet rs;
        int matchId = -1;
        if (tournamentId != -1) {
            insertMatchQuery = "INSERT INTO matches (user_id, team1_id, team2_id, tournament_id, is_completed, winner, current_batting, active_batsman_index, passive_batsman_index, active_bowler_index) VALUES (?, ?, ?, ?, 'false', 'none', 'team1', -1, -1, -1)";
            insertMatchStmt = conn.prepareStatement(insertMatchQuery, Statement.RETURN_GENERATED_KEYS);
            insertMatchStmt.setInt(1, userId);
            insertMatchStmt.setInt(2, team1Id);
            insertMatchStmt.setInt(3, team2Id);
            insertMatchStmt.setInt(4, tournamentId);
        } else {
            insertMatchQuery = "INSERT INTO matches (user_id, team1_id, team2_id, is_completed, winner, current_batting, active_batsman_index, passive_batsman_index, active_bowler_index) VALUES (?, ?, ?, 'false', 'none', 'team1', -1, -1, -1)";
            insertMatchStmt = conn.prepareStatement(insertMatchQuery, Statement.RETURN_GENERATED_KEYS);
            insertMatchStmt.setInt(1, userId);
            insertMatchStmt.setInt(2, team1Id);
            insertMatchStmt.setInt(3, team2Id);
        }
        insertMatchStmt.executeUpdate();

        rs = insertMatchStmt.getGeneratedKeys();
        if (rs.next()) {
            matchId = rs.getInt(1);
            System.out.println("Going to insert player stats for match ID: " + matchId);
            String insertPlayerStatsQuery = "INSERT INTO player_stats (player_id, match_id, team_id) " +
                    "SELECT tp.player_id, ?, ? FROM team_players tp WHERE tp.team_id = ?";

            PreparedStatement insertPlayerStatsStmt = conn.prepareStatement(insertPlayerStatsQuery);
            insertPlayerStatsStmt.setInt(1, matchId);
            insertPlayerStatsStmt.setInt(2, team1Id);
            insertPlayerStatsStmt.setInt(3, team1Id);
            insertPlayerStatsStmt.executeUpdate();

            insertPlayerStatsStmt.setInt(2, team2Id);
            insertPlayerStatsStmt.setInt(3, team2Id);
            insertPlayerStatsStmt.executeUpdate();

            List<Integer> battingOrder = new ArrayList<>();
            List<Integer> bowlingOrder = new ArrayList<>();
            for (int i = 1; i <= 11; i++) {
                battingOrder.add(-1);
            }

            for (int i = 1; i <= 20; i++) {
                bowlingOrder.add(-1);
            }

            String insertTeamOrderQuery = "INSERT INTO team_order (match_id, team_id, batting_order, bowling_order, free_hit_balls) VALUES (?, ?, ?, ?, '[]')";
            PreparedStatement insertTeamOrderStmt = conn.prepareStatement(insertTeamOrderQuery);
            insertTeamOrderStmt.setInt(1, matchId);
            insertTeamOrderStmt.setInt(2, team1Id);
            insertTeamOrderStmt.setString(3, battingOrder.toString());
            insertTeamOrderStmt.setString(4, bowlingOrder.toString());
            insertTeamOrderStmt.executeUpdate();

            insertTeamOrderStmt.setInt(2, team2Id);
            insertTeamOrderStmt.setString(3, battingOrder.toString());
            insertTeamOrderStmt.setString(4, bowlingOrder.toString());
            insertTeamOrderStmt.executeUpdate();
        }
        return matchId;
    }

    public static Map<String, Object> get(Connection conn, int matchId) throws Exception {
        return get(conn, matchId, -1);
    }

    public static Map<String, Object> get(Connection conn, int matchId, int userId) throws Exception {
        PreparedStatement stmt;
        ResultSet rs;
        ResultSet rs1;
        String query;

        if (userId != -1) {
            query = "SELECT * FROM matches WHERE id = ? AND user_id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, matchId);
            stmt.setInt(2, userId);
            rs = stmt.executeQuery();
        } else {
            query = "SELECT * FROM matches WHERE id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, matchId);
            rs = stmt.executeQuery();
        }

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
                System.out.println(tournamentMatches.size());
                tournamentMatches.forEach(System.out::println);
                if (tournamentMatches.get(0) != rs.getInt("id")) {
                    throw new Exception("Match cannot be accessed");
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
            match.put("current_batting", rs.getString("current_batting"));
            match.put("active_batsman_index", rs.getInt("active_batsman_index"));
            match.put("passive_batsman_index", rs.getInt("passive_batsman_index"));

            query = "SELECT t.name, t.logo, p.name as player_name FROM teams t JOIN team_players tp ON t.id = tp.team_id JOIN players p ON tp.player_id = p.id WHERE t.id = ?";
            stmt = conn.prepareStatement(query);

            stmt.setInt(1, matchIds.get("team1_id"));
            rs = stmt.executeQuery();

            if (rs.next()) {
                match.put("team1", rs.getString("name"));
                match.put("team1_logo", "http://localhost:8080/image/teams?name=" + rs.getString("logo") + "&q=low");
                List<String> team1Players = new ArrayList<>();
                do {
                    team1Players.add(rs.getString("player_name"));
                } while (rs.next());
                match.put("team1_players", team1Players);
            }

            stmt.setInt(1, matchIds.get("team2_id"));
            rs = stmt.executeQuery();

            if (rs.next()) {
                match.put("team2", rs.getString("name"));
                match.put("team2_logo", "http://localhost:8080/image/teams?name=" + rs.getString("logo") + "&q=low");
                List<String> team2Players = new ArrayList<>();
                do {
                    team2Players.add(rs.getString("player_name"));
                } while (rs.next());
                match.put("team2_players", team2Players);
            }

            return match;
        } else {
            throw new Exception("Match not found");
        }
    }

    public static String getStats(String matchId) {
        Map<String, Object> matchStats = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = new Database().getConnection();
            String query = "SELECT * FROM matches WHERE id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, matchId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                int team1Id = rs.getInt("team1_id");
                int team2Id = rs.getInt("team2_id");
                matchStats.put("current_batting", rs.getString("current_batting"));
                matchStats.put("is_completed", rs.getString("is_completed"));
                matchStats.put("highlights_path", rs.getString("highlights_path"));
                matchStats.put("winner", rs.getString("winner"));
                matchStats.put("active_batsman_index", rs.getInt("active_batsman_index"));
                matchStats.put("passive_batsman_index", rs.getInt("passive_batsman_index"));
                matchStats.put("active_bowler_index", rs.getInt("active_bowler_index"));

                query = "SELECT ps.player_id, ps.runs, ps.wickets, ps.team_id, ps.wicketer_id, ps.balls, ps.wide_balls, ps.no_balls, " +
                        "w.name AS wicketer_name " +
                        "FROM player_stats ps " +
                        "JOIN team_players tp ON ps.player_id = tp.player_id " +
                        "LEFT JOIN players w ON ps.wicketer_id = w.id " +
                        "WHERE ps.match_id = ? AND ps.team_id IN (?, ?)";
                stmt = conn.prepareStatement(query);
                stmt.setString(1, matchId);
                stmt.setInt(2, team1Id);
                stmt.setInt(3, team2Id);
                rs = stmt.executeQuery();

                Map<Integer, Integer> team1Runs = new HashMap<>();
                Map<Integer, Integer> team2Runs = new HashMap<>();
                Map<Integer, Integer> team1Wickets = new HashMap<>();
                Map<Integer, Integer> team2Wickets = new HashMap<>();
                int team1_balls = 0;
                int team2_balls = 0;
                int team1_wides = 0;
                int team2_wides = 0;
                int team1_no_balls = 0;
                int team2_no_balls = 0;
                List<String> team1WicketsMap = new ArrayList<>();
                List<String> team2WicketsMap = new ArrayList<>();
                List<String> team1BallsMap = new ArrayList<>();
                List<String> team2BallsMap = new ArrayList<>();
                List<Integer> team1Freehits = new ArrayList<>();
                List<Integer> team2Freehits = new ArrayList<>();
                List<Integer> team1BattingOrder = new ArrayList<>();
                List<Integer> team2BattingOrder = new ArrayList<>();
                List<Integer> team1BowlingOrder = new ArrayList<>();
                List<Integer> team2BowlingOrder = new ArrayList<>();

                while (rs.next()) {
                    int playerId = rs.getInt("player_id");
                    int runs = rs.getInt("runs");
                    int balls = rs.getInt("balls");
                    int wickets = rs.getInt("wickets");
                    int teamId = rs.getInt("team_id");
                    String wicketerName = rs.getString("wicketer_name");

                    if (teamId == team1Id) {
                        team1Runs.put(playerId, runs);
                        team1Wickets.put(playerId, wickets);
                        team1BallsMap.add(String.valueOf(balls));
                        team1_balls += balls;
                        team1_wides += rs.getInt("wide_balls");
                        team1_no_balls += rs.getInt("no_balls");
                        team1WicketsMap.add(wicketerName);
                    } else if (teamId == team2Id) {
                        team2Runs.put(playerId, runs);
                        team2Wickets.put(playerId, wickets);
                        team2BallsMap.add(String.valueOf(balls));
                        team2_balls += balls;
                        team2_wides += rs.getInt("wide_balls");
                        team2_no_balls += rs.getInt("no_balls");
                        team2WicketsMap.add(wicketerName);
                    }
                }

                query = "SELECT free_hit_balls, batting_order, bowling_order, team_id FROM team_order WHERE match_id = ?";
                stmt = conn.prepareStatement(query);
                stmt.setString(1, matchId);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    if (team1Id == rs.getInt("team_id")) {
                        team1Freehits = objectMapper.readValue(rs.getString("free_hit_balls"), ArrayList.class);
                        team1BattingOrder = objectMapper.readValue(rs.getString("batting_order"), ArrayList.class);
                        team1BowlingOrder = objectMapper.readValue(rs.getString("bowling_order"), ArrayList.class);
                    } else if (team2Id == rs.getInt("team_id")) {
                        team2Freehits = objectMapper.readValue(rs.getString("free_hit_balls"), ArrayList.class);
                        team2BattingOrder = objectMapper.readValue(rs.getString("batting_order"), ArrayList.class);
                        team2BowlingOrder = objectMapper.readValue(rs.getString("bowling_order"), ArrayList.class);
                    }
                }

                matchStats.put("is_highlights_uploaded", matchStats.get("highlights_path") == null && Objects.equals(matchStats.get("is_completed"), "true") ? "false" : "true");
                matchStats.put("team1_score", team1Runs.values().stream().mapToInt(Integer::intValue).sum() + team2_wides + team2_no_balls);
                matchStats.put("team2_score", team2Runs.values().stream().mapToInt(Integer::intValue).sum() + team1_wides + team1_no_balls);
                matchStats.put("team1_wickets", team1Wickets.values().stream().mapToInt(Integer::intValue).sum());
                matchStats.put("team2_wickets", team2Wickets.values().stream().mapToInt(Integer::intValue).sum());
                matchStats.put("team1_runs", team1Runs);
                matchStats.put("team2_runs", team2Runs);
                matchStats.put("team1_outs", team1Wickets);
                matchStats.put("team2_outs", team2Wickets);
                matchStats.put("team1_balls", team1_balls);
                matchStats.put("team2_balls", team2_balls);
                matchStats.put("team1_wickets_map", team1WicketsMap);
                matchStats.put("team2_wickets_map", team2WicketsMap);
                matchStats.put("team1_balls_map", team1BallsMap);
                matchStats.put("team2_balls_map", team2BallsMap);
                matchStats.put("team1_freehits_map", team1Freehits);
                matchStats.put("team2_freehits_map", team2Freehits);
                matchStats.put("team1_batting_order", team1BattingOrder);
                matchStats.put("team2_batting_order", team2BattingOrder);
                matchStats.put("team1_bowling_order", team1BowlingOrder);
                matchStats.put("team2_bowling_order", team2BowlingOrder);
            }
        } catch (Exception e) {
            System.out.println("Error fetching match stats: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        try {
            System.out.println("Match stats: " + matchStats);
            return objectMapper.writeValueAsString(matchStats);
        } catch (JsonProcessingException e) {
            System.out.println("Error converting to JSON: " + e.getMessage());
            return null;
        }
    }

    public static boolean isOwner(Connection conn, int matchId, int userId) {
        PreparedStatement stmt;
        ResultSet rs;
        try {
            String query = "SELECT * FROM matches WHERE id = ? AND user_id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, matchId);
            stmt.setInt(2, userId);
            rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println("Error checking match owner: " + e.getMessage());
            return false;
        }
    }
}
