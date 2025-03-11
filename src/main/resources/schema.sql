CREATE TABLE players
(
    id         INT(11)      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT current_timestamp()
);

CREATE TABLE teams
(
    id         INT(11)      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL UNIQUE,
    created_at DATETIME     NOT NULL DEFAULT current_timestamp()
);

CREATE TABLE team_players
(
    id              INT(11)  NOT NULL AUTO_INCREMENT PRIMARY KEY,
    team_id         INT(11)  NOT NULL,
    player_id       INT(11)  NOT NULL,
    player_position INT(11)  NOT NULL CHECK (player_position BETWEEN 1 AND 11),
    created_at      DATETIME NOT NULL DEFAULT current_timestamp(),
    FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
    FOREIGN KEY (player_id) REFERENCES players (id) ON DELETE CASCADE,
    UNIQUE (team_id, player_id)
);

CREATE TABLE tournaments
(
    id         INT(11)      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL UNIQUE,
    status     ENUM ('ongoing', 'completed') NOT NULL DEFAULT 'ongoing',
    winner_id INT(11)      NULL,
    created_at DATETIME     NOT NULL DEFAULT current_timestamp(),
    FOREIGN KEY (winner_id) REFERENCES teams (id) ON DELETE SET NULL
);

CREATE TABLE matches
(
    id                    INT(11)                                NOT NULL AUTO_INCREMENT PRIMARY KEY,
    team1_id              INT(11)                                NOT NULL,
    team2_id              INT(11)                                NOT NULL,
    is_completed          ENUM ('true', 'false')                 NOT NULL DEFAULT 'false',
    highlights_path      VARCHAR(255)                           NULL,
    winner                ENUM ('team1', 'team2', 'none', 'tie') NOT NULL DEFAULT 'none',
    current_batting       ENUM ('team1', 'team2')                NOT NULL DEFAULT 'team1',
    active_batsman_index  INT(11)                                NOT NULL DEFAULT 1,
    passive_batsman_index INT(11)                                NOT NULL DEFAULT 2,
    tournament_id         INT(11)                                NULL,
    created_at            DATETIME                               NOT NULL DEFAULT current_timestamp(),
    FOREIGN KEY (team1_id) REFERENCES teams (id) ON DELETE CASCADE,
    FOREIGN KEY (team2_id) REFERENCES teams (id) ON DELETE CASCADE,
    FOREIGN KEY (tournament_id) REFERENCES tournaments (id) ON DELETE SET NULL
);

CREATE TABLE player_stats
(
    id          INT(11)  NOT NULL AUTO_INCREMENT PRIMARY KEY,
    player_id   INT(11)  NOT NULL,
    match_id    INT(11)  NOT NULL,
    team_id     INT(11)  NOT NULL,
    runs        INT(11)           DEFAULT 0,
    wickets     INT(11)           DEFAULT 0,
    balls       INT(11)           DEFAULT 0,
    wicketer_id INT(11)  NULL,
    created_at  DATETIME NOT NULL DEFAULT current_timestamp(),
    FOREIGN KEY (player_id) REFERENCES players (id) ON DELETE CASCADE,
    FOREIGN KEY (match_id) REFERENCES matches (id) ON DELETE CASCADE,
    FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
    FOREIGN KEY (wicketer_id) REFERENCES players (id) ON DELETE SET NULL
);

CREATE TABLE tournament_winners
(
    id           INT(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tournament_id INT(11) NOT NULL,
    team_id       INT(11) NOT NULL,
    created_at    DATETIME NOT NULL DEFAULT current_timestamp(),
    FOREIGN KEY (tournament_id) REFERENCES tournaments (id) ON DELETE CASCADE,
    FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
    UNIQUE (tournament_id, team_id)
);
