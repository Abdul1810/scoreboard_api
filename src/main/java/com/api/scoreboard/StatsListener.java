package com.api.scoreboard;

import com.api.util.Database;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class StatsListener {
    private static final Map<String, Session> sessions = new HashMap<>();
    private static final Map<String, CopyOnWriteArrayList<String>> matchSessions = new HashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void addSession(String matchId, Session session) {
        System.out.println("Adding session: " + session.getId());
        System.out.println("For match: " + matchId);
        sessions.put(session.getId(), session);
        matchSessions.putIfAbsent(matchId, new CopyOnWriteArrayList<>());
        matchSessions.get(matchId).add(session.getId());

        String matchStats = fetchMatchStatsFromDatabase(matchId);
        System.out.println("Sending stats: " + matchStats);
        try {
            session.getBasicRemote().sendText(matchStats);
        } catch (IOException e) {
            System.out.println("Error sending stats: " + e.getMessage());
        }
    }

    public static void removeSession(String matchId, Session session) {
        sessions.remove(session.getId());
        matchSessions.getOrDefault(matchId, new CopyOnWriteArrayList<>()).remove(session.getId());
    }

    public static void fireStatsUpdate(String matchId, String matchData) {
        sendStatsToAllSessions(matchId, matchData);
    }

    public static void fireStatsRemove(String matchId) {
        System.out.println("Match completed: " + matchId);
        System.out.println(sessions);
        System.out.println(matchSessions);
        if (matchSessions.get(matchId) != null) matchSessions.get(matchId).forEach(System.out::println);
        disconnectAllSessions(matchId);
    }

    private static void sendStatsToAllSessions(String matchId, String matchData) {
        List<String> sendingSessions = new ArrayList<>(matchSessions.getOrDefault(matchId, new CopyOnWriteArrayList<>()));

        for (String sessionId : sendingSessions) {
            try {
                Session session = sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    session.getBasicRemote().sendText(matchData);
                } else {
                    matchSessions.get(matchId).remove(sessionId);
                }
            } catch (Exception e) {
                System.out.println("Error sending content to session: " + sessionId);
            }
        }
    }

    private static void disconnectAllSessions(String matchId) {
        CopyOnWriteArrayList<String> sendingSessions = matchSessions.get(matchId);
        if (sendingSessions == null) {
            return;
        }

        for (String sessionId : new ArrayList<>(sendingSessions)) {
            try {
                Session session = sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    session.close();
                }
                sendingSessions.remove(sessionId);
            } catch (IOException e) {
                System.out.println("Error closing session: " + sessionId);
            }
        }

        matchSessions.put(matchId, new CopyOnWriteArrayList<>());
    }

    private static String fetchMatchStatsFromDatabase(String matchId) {
        /*
                team1_score: int,
                team2_score: int,
                team1_wickets: int,
                team2_wickets: int,
                team1_balls: int,
                team2_balls: int,
                team1_runs: List<int>,
                team2_rums: List<int>,
                current_batting: string,
                is_completed: boolean,
                winner: string
             */
        Map<String, Object> matchStats = new HashMap<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        /*
                CREATE TABLE match_stats (
                     id INT AUTO_INCREMENT PRIMARY KEY,
                     match_id INT NOT NULL,
                     team1_player1_runs INT DEFAULT NULL,
                     team1_player2_runs INT DEFAULT 0,
                     team1_player3_runs INT DEFAULT 0,
                     team1_player4_runs INT DEFAULT 0,
                     team1_player5_runs INT DEFAULT 0,
                     team1_player6_runs INT DEFAULT 0,
                     team1_player7_runs INT DEFAULT 0,
                     team1_player8_runs INT DEFAULT 0,
                     team1_player9_runs INT DEFAULT 0,
                     team1_player10_runs INT DEFAULT 0,
                     team1_player11_runs INT DEFAULT 0,
                     team2_player1_runs INT DEFAULT 0,
                     team2_player2_runs INT DEFAULT 0,
                     team2_player3_runs INT DEFAULT 0,
                     team2_player4_runs INT DEFAULT 0,
                     team2_player5_runs INT DEFAULT 0,
                     team2_player6_runs INT DEFAULT 0,
                     team2_player7_runs INT DEFAULT 0,
                     team2_player8_runs INT DEFAULT 0,
                     team2_player9_runs INT DEFAULT 0,
                     team2_player10_runs INT DEFAULT 0,
                     team2_player11_runs INT DEFAULT 0,
                     team1_player1_wickets INT DEFAULT 0,
                     team1_player2_wickets INT DEFAULT 0,
                     team1_player3_wickets INT DEFAULT 0,
                     team1_player4_wickets INT DEFAULT 0,
                     team1_player5_wickets INT DEFAULT 0,
                     team1_player6_wickets INT DEFAULT 0,
                     team1_player7_wickets INT DEFAULT 0,
                     team1_player8_wickets INT DEFAULT 0,
                     team1_player9_wickets INT DEFAULT 0,
                     team1_player10_wickets INT DEFAULT 0,
                     team1_player11_wickets INT DEFAULT 0,
                     team2_player1_wickets INT DEFAULT 0,
                     team2_player2_wickets INT DEFAULT 0,
                     team2_player3_wickets INT DEFAULT 0,
                     team2_player4_wickets INT DEFAULT 0,
                     team2_player5_wickets INT DEFAULT 0,
                     team2_player6_wickets INT DEFAULT 0,
                     team2_player7_wickets INT DEFAULT 0,
                     team2_player8_wickets INT DEFAULT 0,
                     team2_player9_wickets INT DEFAULT 0,
                     team2_player10_wickets INT DEFAULT 0,
                     team2_player11_wickets INT DEFAULT 0,
                     team1_balls INT DEFAULT 0 NOT NULL,
                     team2_balls INT DEFAULT 0 NOT NULL,
                     current_batting ENUM('team1', 'team2') NOT NULL DEFAULT 'team1',
                     is_completed ENUM('true', 'false') NOT NULL DEFAULT 'false',
                     winner ENUM('team1', 'team2', 'none', 'tie') NOT NULL DEFAULT 'none',
                     team1_wickets INT GENERATED ALWAYS AS (
                         team2_player1_wickets + team2_player2_wickets + team2_player3_wickets +
                         team2_player4_wickets + team2_player5_wickets + team2_player6_wickets +
                         team2_player7_wickets + team2_player8_wickets + team2_player9_wickets +
                         team2_player10_wickets + team2_player11_wickets
                         ) VIRTUAL,
                     team2_wickets INT GENERATED ALWAYS AS (
                         team1_player1_wickets + team1_player2_wickets + team1_player3_wickets +
                         team1_player4_wickets + team1_player5_wickets + team1_player6_wickets +
                         team1_player7_wickets + team1_player8_wickets + team1_player9_wickets +
                         team1_player10_wickets + team1_player11_wickets
                         ) VIRTUAL,
                     FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE
                );
             */
        try {
            conn = Database.getConnection();
            stmt = conn.prepareStatement("SELECT * FROM match_stats WHERE match_id = ?");
            stmt.setString(1, matchId);
            rs = stmt.executeQuery();

            List<Integer> team1Runs = new ArrayList<>();
            List<Integer> team2Runs = new ArrayList<>();

//
//            int current_player = rs.getInt("team1_wickets") + 1;
//            if (rs.getString("current_batting").equals("team2")) {
//                current_player = rs.getInt("team2_wickets") + 1;
//            }

            if (rs.next()) {
                for (int i = 1; i <= 11; i++) {
                    team1Runs.add(rs.getInt("team1_player" + i + "_runs"));
                    team2Runs.add(rs.getInt("team2_player" + i + "_runs"));
                }
                matchStats.put("team1_score", team1Runs.stream().mapToInt(Integer::intValue).sum());
                matchStats.put("team2_score", team2Runs.stream().mapToInt(Integer::intValue).sum());
                matchStats.put("team1_wickets", rs.getInt("team1_wickets"));
                matchStats.put("team2_wickets", rs.getInt("team2_wickets"));
                matchStats.put("team1_balls", rs.getInt("team1_balls"));
                matchStats.put("team2_balls", rs.getInt("team2_balls"));
                matchStats.put("team1_runs", new ArrayList<>(Arrays.asList(rs.getInt("team1_player1_runs"), rs.getInt("team1_player2_runs"), rs.getInt("team1_player3_runs"), rs.getInt("team1_player4_runs"), rs.getInt("team1_player5_runs"), rs.getInt("team1_player6_runs"), rs.getInt("team1_player7_runs"), rs.getInt("team1_player8_runs"), rs.getInt("team1_player9_runs"), rs.getInt("team1_player10_runs"), rs.getInt("team1_player11_runs"))));
                matchStats.put("team2_runs", new ArrayList<>(Arrays.asList(rs.getInt("team2_player1_runs"), rs.getInt("team2_player2_runs"), rs.getInt("team2_player3_runs"), rs.getInt("team2_player4_runs"), rs.getInt("team2_player5_runs"), rs.getInt("team2_player6_runs"), rs.getInt("team2_player7_runs"), rs.getInt("team2_player8_runs"), rs.getInt("team2_player9_runs"), rs.getInt("team2_player10_runs"), rs.getInt("team2_player11_runs"))));
                matchStats.put("team1_outs", new ArrayList<>(Arrays.asList(rs.getInt("team1_player1_wickets"), rs.getInt("team1_player2_wickets"), rs.getInt("team1_player3_wickets"), rs.getInt("team1_player4_wickets"), rs.getInt("team1_player5_wickets"), rs.getInt("team1_player6_wickets"), rs.getInt("team1_player7_wickets"), rs.getInt("team1_player8_wickets"), rs.getInt("team1_player9_wickets"), rs.getInt("team1_player10_wickets"), rs.getInt("team1_player11_wickets"))));
                matchStats.put("team2_outs", new ArrayList<>(Arrays.asList(rs.getInt("team2_player1_wickets"), rs.getInt("team2_player2_wickets"), rs.getInt("team2_player3_wickets"), rs.getInt("team2_player4_wickets"), rs.getInt("team2_player5_wickets"), rs.getInt("team2_player6_wickets"), rs.getInt("team2_player7_wickets"), rs.getInt("team2_player8_wickets"), rs.getInt("team2_player9_wickets"), rs.getInt("team2_player10_wickets"), rs.getInt("team2_player11_wickets"))));
                matchStats.put("current_batting", rs.getString("current_batting"));
                matchStats.put("is_completed", rs.getString("is_completed"));
                matchStats.put("winner", rs.getString("winner"));
            }
        } catch (Exception e) {
            System.out.println("Error fetching match stats: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                System.out.println("Error" + e.getMessage());
            }
        }
        try {
            return objectMapper.writeValueAsString(matchStats);
        } catch (JsonProcessingException e) {
            System.out.println("Error converting to JSON: " + e.getMessage());
            return "{}";
        }
    }
}
