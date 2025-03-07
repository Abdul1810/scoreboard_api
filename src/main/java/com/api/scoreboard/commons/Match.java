package com.api.scoreboard.commons;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class Match {
    public static int create(Connection conn, int team1Id, int team2Id) throws Exception {
        PreparedStatement insertMatchStmt;
        ResultSet rs;
        int matchId = -1;
        String insertMatchQuery = "INSERT INTO matches (team1_id, team2_id, is_completed, winner, current_batting) VALUES (?, ?, 'false', 'none', 'team1')";
        insertMatchStmt = conn.prepareStatement(insertMatchQuery, Statement.RETURN_GENERATED_KEYS);
        insertMatchStmt.setInt(1, team1Id);
        insertMatchStmt.setInt(2, team2Id);
        insertMatchStmt.executeUpdate();

        rs = insertMatchStmt.getGeneratedKeys();
        if (rs.next()) {
            matchId = rs.getInt(1);
            System.out.println("Going to insert player stats for match ID: " + matchId);
            String insertPlayerStatsQuery = "INSERT INTO player_stats (player_id, match_id, team_id, runs, wickets) " +
                    "SELECT tp.player_id, ?, ?, 0, 0 FROM team_players tp WHERE tp.team_id = ?";

            PreparedStatement insertPlayerStatsStmt = conn.prepareStatement(insertPlayerStatsQuery);
            insertPlayerStatsStmt.setInt(1, matchId);
            insertPlayerStatsStmt.setInt(2, team1Id);
            insertPlayerStatsStmt.setInt(3, team1Id);
            insertPlayerStatsStmt.executeUpdate();

            insertPlayerStatsStmt.setInt(2, team2Id);
            insertPlayerStatsStmt.setInt(3, team2Id);
            insertPlayerStatsStmt.executeUpdate();
        }
        return matchId;
    }
}
