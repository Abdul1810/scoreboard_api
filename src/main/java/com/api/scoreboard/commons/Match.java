package com.api.scoreboard.commons;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Match {
    public static int create(Connection conn, int team1Id, int team2Id) throws Exception {
        return create(conn, team1Id, team2Id, -1);
    }

    public static int create(Connection conn, int team1Id, int team2Id, int tournamentId) throws Exception {
        String insertMatchQuery;
        PreparedStatement insertMatchStmt;
        ResultSet rs;
        int matchId = -1;
        if (tournamentId != -1) {
            insertMatchQuery = "INSERT INTO matches (team1_id, team2_id, tournament_id, is_completed, winner, current_batting, active_batsman_index, passive_batsman_index, active_bowler_index) VALUES (?, ?, ?, 'false', 'none', 'team1', -1, -1, -1)";
            insertMatchStmt = conn.prepareStatement(insertMatchQuery, Statement.RETURN_GENERATED_KEYS);
            insertMatchStmt.setInt(1, team1Id);
            insertMatchStmt.setInt(2, team2Id);
            insertMatchStmt.setInt(3, tournamentId);
        } else {
            insertMatchQuery = "INSERT INTO matches (team1_id, team2_id, is_completed, winner, current_batting, active_batsman_index, passive_batsman_index, active_bowler_index) VALUES (?, ?, 'false', 'none', 'team1', -1, -1, -1)";
            insertMatchStmt = conn.prepareStatement(insertMatchQuery, Statement.RETURN_GENERATED_KEYS);
            insertMatchStmt.setInt(1, team1Id);
            insertMatchStmt.setInt(2, team2Id);
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
}
