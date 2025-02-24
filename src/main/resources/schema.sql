CREATE TABLE matches (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    team1 TEXT NOT NULL,
    team2 TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
);

CREATE TABLE match_stats (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    match_id INTEGER NOT NULL,
    team1 INTEGER NOT NULL,
    team2 INTEGER NOT NULL,
    team1_wickets INTEGER NOT NULL,
    team2_wickets INTEGER NOT NULL,
    team1_balls INTEGER NOT NULL,
    team2_balls INTEGER NOT NULL,
    current_batting TEXT NOT NULL CHECK(current_batting IN ('team1', 'team2')),
    is_completed TEXT NOT NULL CHECK(is_completed IN ('true', 'false')),
    winner TEXT NOT NULL CHECK(winner IN ('team1', 'team2', 'none', 'tie')),
    FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE
);