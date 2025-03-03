CREATE TABLE teams
(
    id         int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name       varchar(255) NOT NULL UNIQUE,
--     player1    varchar(255) NOT NULL,
--     player2    varchar(255) NOT NULL,
--     player3    varchar(255) NOT NULL,
--     player4    varchar(255) NOT NULL,
--     player5    varchar(255) NOT NULL,
--     player6    varchar(255) NOT NULL,
--     player7    varchar(255) NOT NULL,
--     player8    varchar(255) NOT NULL,
--     player9    varchar(255) NOT NULL,
--     player10   varchar(255) NOT NULL,
--     player11   varchar(255) NOT NULL,
    player1_id int(11) NOT NULL,
    player2_id int(11) NOT NULL,
    player3_id int(11) NOT NULL,
    player4_id int(11) NOT NULL,
    player5_id int(11) NOT NULL,
    player6_id int(11) NOT NULL,
    player7_id int(11) NOT NULL,
    player8_id int(11) NOT NULL,
    player9_id int(11) NOT NULL,
    player10_id int(11) NOT NULL,
    player11_id int(11) NOT NULL,

    created_at datetime     NOT NULL DEFAULT current_timestamp(),
);

CREATE TABLE matches
(
    id         int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    team1_id   int(11) NOT NULL,
    team2_id   int(11) NOT NULL,
    created_at datetime NOT NULL DEFAULT current_timestamp(),
    FOREIGN KEY (team1_id) REFERENCES teams (id) ON DELETE CASCADE,
    FOREIGN KEY (team2_id) REFERENCES teams (id) ON DELETE CASCADE
);

-- CREATE TABLE match_stats
-- (
--     id              int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
--     match_id        int(11) NOT NULL,
--     team1_stats_id  int(11) NOT NULL,
--     team2_stats_id  int(11) NOT NULL,
--     is_completed    enum('true','false') NOT NULL DEFAULT 'false',
--     winner          enum('team1','team2','none','tie') NOT NULL DEFAULT 'none',
--     current_batting enum('team1','team2') NOT NULL DEFAULT 'team1',
--     created_at      datetime NOT NULL DEFAULT current_timestamp(),
--     FOREIGN KEY (match_id) REFERENCES matches (id) ON DELETE CASCADE,
--     FOREIGN KEY (team1_stats_id) REFERENCES team_stats (id) ON DELETE CASCADE,
--     FOREIGN KEY (team2_stats_id) REFERENCES team_stats (id) ON DELETE CASCADE
-- );
--
-- CREATE TABLE team_stats
-- (
--     id               int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
--     team_id          int(11) NOT NULL,
--     player1_runs     int(11) DEFAULT 0,
--     player2_runs     int(11) DEFAULT 0,
--     player3_runs     int(11) DEFAULT 0,
--     player4_runs     int(11) DEFAULT 0,
--     player5_runs     int(11) DEFAULT 0,
--     player6_runs     int(11) DEFAULT 0,
--     player7_runs     int(11) DEFAULT 0,
--     player8_runs     int(11) DEFAULT 0,
--     player9_runs     int(11) DEFAULT 0,
--     player10_runs    int(11) DEFAULT 0,
--     player11_runs    int(11) DEFAULT 0,
--     player1_wickets  int(11) DEFAULT 0,
--     player2_wickets  int(11) DEFAULT 0,
--     player3_wickets  int(11) DEFAULT 0,
--     player4_wickets  int(11) DEFAULT 0,
--     player5_wickets  int(11) DEFAULT 0,
--     player6_wickets  int(11) DEFAULT 0,
--     player7_wickets  int(11) DEFAULT 0,
--     player8_wickets  int(11) DEFAULT 0,
--     player9_wickets  int(11) DEFAULT 0,
--     player10_wickets int(11) DEFAULT 0,
--     player11_wickets int(11) DEFAULT 0,
--     balls            int(11) NOT NULL DEFAULT 0,
--     total_score      int(11) GENERATED ALWAYS AS (player1_runs + player2_runs + player3_runs + player4_runs + player5_runs + player6_runs + player7_runs + player8_runs + player9_runs + player10_runs + player11_runs) STORED,
--     total_wickets    int(11) GENERATED ALWAYS AS (player1_wickets + player2_wickets + player3_wickets + player4_wickets + player5_wickets + player6_wickets + player7_wickets + player8_wickets + player9_wickets + player10_wickets + player11_wickets) STORED,
--     FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE
-- );
