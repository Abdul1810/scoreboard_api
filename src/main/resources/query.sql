WITH PlayerIndex AS (SELECT id      AS team_id,
                            CASE
                                WHEN player1 = 'Vishnu' THEN 1
                                WHEN player2 = 'Vishnu' THEN 2
                                WHEN player3 = 'Vishnu' THEN 3
                                WHEN player4 = 'Vishnu' THEN 4
                                WHEN player5 = 'Vishnu' THEN 5
                                WHEN player6 = 'Vishnu' THEN 6
                                WHEN player7 = 'Vishnu' THEN 7
                                WHEN player8 = 'Vishnu' THEN 8
                                WHEN player9 = 'Vishnu' THEN 9
                                WHEN player10 = 'Vishnu' THEN 10
                                WHEN player11 = 'Vishnu' THEN 11
                                ELSE NULL
                                END AS player_index
                     FROM teams
                     WHERE id = 1)
SELECT COUNT(DISTINCT m.id) AS matches_played,
       COALESCE(SUM(
                        CASE
                            WHEN m.team1_id = p.team_id THEN
                                CASE p.player_index
                                    WHEN 1 THEN ms.team1_player1_runs
                                    WHEN 2 THEN ms.team1_player2_runs
                                    WHEN 3 THEN ms.team1_player3_runs
                                    WHEN 4 THEN ms.team1_player4_runs
                                    WHEN 5 THEN ms.team1_player5_runs
                                    WHEN 6 THEN ms.team1_player6_runs
                                    WHEN 7 THEN ms.team1_player7_runs
                                    WHEN 8 THEN ms.team1_player8_runs
                                    WHEN 9 THEN ms.team1_player9_runs
                                    WHEN 10 THEN ms.team1_player10_runs
                                    WHEN 11 THEN ms.team1_player11_runs
                                    ELSE 0
                                    END
                            WHEN m.team2_id = p.team_id THEN
                                CASE p.player_index
                                    WHEN 1 THEN ms.team2_player1_runs
                                    WHEN 2 THEN ms.team2_player2_runs
                                    WHEN 3 THEN ms.team2_player3_runs
                                    WHEN 4 THEN ms.team2_player4_runs
                                    WHEN 5 THEN ms.team2_player5_runs
                                    WHEN 6 THEN ms.team2_player6_runs
                                    WHEN 7 THEN ms.team2_player7_runs
                                    WHEN 8 THEN ms.team2_player8_runs
                                    WHEN 9 THEN ms.team2_player9_runs
                                    WHEN 10 THEN ms.team2_player10_runs
                                    WHEN 11 THEN ms.team2_player11_runs
                                    ELSE 0
                                    END
                            ELSE 0
                            END
                ), 0)       AS total_score
FROM matches m
         JOIN match_stats ms ON ms.match_id = m.id
         JOIN PlayerIndex p ON p.team_id = m.team1_id OR p.team_id = m.team2_id
WHERE p.player_index IS NOT NULL;
