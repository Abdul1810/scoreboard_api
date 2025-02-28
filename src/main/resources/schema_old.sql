CREATE TABLE teams (
       id INT AUTO_INCREMENT PRIMARY KEY,
       name VARCHAR(255) UNIQUE NOT NULL,
       player1 VARCHAR(255) NOT NULL,
       player2 VARCHAR(255) NOT NULL,
       player3 VARCHAR(255) NOT NULL,
       player4 VARCHAR(255) NOT NULL,
       player5 VARCHAR(255) NOT NULL,
       player6 VARCHAR(255) NOT NULL,
       player7 VARCHAR(255) NOT NULL,
       player8 VARCHAR(255) NOT NULL,
       player9 VARCHAR(255) NOT NULL,
       player10 VARCHAR(255) NOT NULL,
       player11 VARCHAR(255) NOT NULL,
       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE matches (
     id INT AUTO_INCREMENT PRIMARY KEY,
     team1_id INT NOT NULL,
     team2_id INT NOT NULL,
     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
     FOREIGN KEY (team1_id) REFERENCES teams(id) ON DELETE CASCADE,
     FOREIGN KEY (team2_id) REFERENCES teams(id) ON DELETE CASCADE
);

CREATE TABLE match_stats (
     id INT AUTO_INCREMENT PRIMARY KEY,
     match_id INT NOT NULL,
     team1_player1_runs INT DEFAULT 0,team1_player2_runs INT DEFAULT 0,team1_player3_runs INT DEFAULT 0,team1_player4_runs INT DEFAULT 0,team1_player5_runs INT DEFAULT 0,
     team1_player6_runs INT DEFAULT 0,team1_player7_runs INT DEFAULT 0,team1_player8_runs INT DEFAULT 0,team1_player9_runs INT DEFAULT 0,team1_player10_runs INT DEFAULT 0,
     team1_player11_runs INT DEFAULT 0,
     team2_player1_runs INT DEFAULT 0,team2_player2_runs INT DEFAULT 0,team2_player3_runs INT DEFAULT 0,team2_player4_runs INT DEFAULT 0,team2_player5_runs INT DEFAULT 0,
     team2_player6_runs INT DEFAULT 0,team2_player7_runs INT DEFAULT 0,team2_player8_runs INT DEFAULT 0,team2_player9_runs INT DEFAULT 0,team2_player10_runs INT DEFAULT 0,
     team2_player11_runs INT DEFAULT 0,
     team1_player1_wickets INT DEFAULT 0,team1_player2_wickets INT DEFAULT 0,team1_player3_wickets INT DEFAULT 0,team1_player4_wickets INT DEFAULT 0,team1_player5_wickets INT DEFAULT 0,
     team1_player6_wickets INT DEFAULT 0,team1_player7_wickets INT DEFAULT 0,team1_player8_wickets INT DEFAULT 0,team1_player9_wickets INT DEFAULT 0,team1_player10_wickets INT DEFAULT 0,
     team1_player11_wickets INT DEFAULT 0,
     team2_player1_wickets INT DEFAULT 0,team2_player2_wickets INT DEFAULT 0,team2_player3_wickets INT DEFAULT 0,team2_player4_wickets INT DEFAULT 0,team2_player5_wickets INT DEFAULT 0,
     team2_player6_wickets INT DEFAULT 0,team2_player7_wickets INT DEFAULT 0,team2_player8_wickets INT DEFAULT 0,team2_player9_wickets INT DEFAULT 0,team2_player10_wickets INT DEFAULT 0,
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
