CREATE TABLE players
(
    id         int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name       varchar(255) NOT NULL,
    created_at datetime     NOT NULL DEFAULT current_timestamp()
);

CREATE TABLE teams
(
    id          int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        varchar(255) NOT NULL UNIQUE,
    player1_id  int(11) NOT NULL,
    player2_id  int(11) NOT NULL,
    player3_id  int(11) NOT NULL,
    player4_id  int(11) NOT NULL,
    player5_id  int(11) NOT NULL,
    player6_id  int(11) NOT NULL,
    player7_id  int(11) NOT NULL,
    player8_id  int(11) NOT NULL,
    player9_id  int(11) NOT NULL,
    player10_id int(11) NOT NULL,
    player11_id int(11) NOT NULL,
    created_at  datetime     NOT NULL DEFAULT current_timestamp(),
    FOREIGN KEY (player1_id) REFERENCES players (id) ON DELETE CASCADE,
    FOREIGN KEY (player2_id) REFERENCES players (id) ON DELETE CASCADE,
    FOREIGN KEY (player3_id) REFERENCES players (id) ON DELETE CASCADE,
    FOREIGN KEY (player4_id) REFERENCES players (id) ON DELETE CASCADE,
    FOREIGN KEY (player5_id) REFERENCES players (id) ON DELETE CASCADE,
    FOREIGN KEY (player6_id) REFERENCES players (id) ON DELETE CASCADE,
    FOREIGN KEY (player7_id) REFERENCES players (id) ON DELETE CASCADE,
    FOREIGN KEY (player8_id) REFERENCES players (id) ON DELETE CASCADE,
    FOREIGN KEY (player9_id) REFERENCES players (id) ON DELETE CASCADE,
    FOREIGN KEY (player10_id) REFERENCES players (id) ON DELETE CASCADE,
    FOREIGN KEY (player11_id) REFERENCES players (id) ON DELETE CASCADE
);

ALTER TABLE players
    ADD COLUMN team_id int(11);
ALTER TABLE players
    ADD FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE;

CREATE TABLE matches
(
    id                    int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    team1_id              int(11) NOT NULL,
    team2_id              int(11) NOT NULL,
    is_completed          ENUM('true','false') NOT NULL DEFAULT 'false',
    winner                ENUM('team1','team2','none','tie') NOT NULL DEFAULT 'none',
    current_batting       ENUM('team1','team2') NOT NULL DEFAULT 'team1',
    active_batsman_index  int(11) NOT NULL DEFAULT 1,
    passive_batsman_index int(11) NOT NULL DEFAULT 2,
    created_at            datetime NOT NULL DEFAULT current_timestamp(),
    FOREIGN KEY (team1_id) REFERENCES teams (id) ON DELETE CASCADE,
    FOREIGN KEY (team2_id) REFERENCES teams (id) ON DELETE CASCADE
);

CREATE TABLE player_stats
(
    id          int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    player_id   int(11) NOT NULL,
    match_id    int(11) NOT NULL,
    team_id     int(11) NOT NULL,
    runs        int(11) DEFAULT 0,
    wickets     int(11) DEFAULT 0,
    balls       int(11) DEFAULT 0,
    wicketer_id int(11),
    created_at  datetime NOT NULL DEFAULT current_timestamp(),
    FOREIGN KEY (player_id) REFERENCES players (id) ON DELETE CASCADE,
    FOREIGN KEY (match_id) REFERENCES matches (id) ON DELETE CASCADE,
    FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
    FOREIGN KEY (wicketer_id) REFERENCES players (id)
);
