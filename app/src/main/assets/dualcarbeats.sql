-- Dual Carbeats database (SQLite version of the Data Modeler design)

CREATE TABLE game_level (
    level_id   INTEGER PRIMARY KEY,
    nama_lagu  TEXT NOT NULL,
    kesulitan  TEXT NOT NULL,
    file_musik TEXT NOT NULL,
    file_map   TEXT NOT NULL
);

CREATE TABLE high_score (
    score_id            INTEGER PRIMARY KEY AUTOINCREMENT,
    skor_tertinggi      INTEGER NOT NULL,
    perfect_count       INTEGER NOT NULL,
    great_count         INTEGER NOT NULL,
    miss_count          INTEGER NOT NULL,
    max_combo           INTEGER NOT NULL,
    game_level_level_id INTEGER NOT NULL,
    CONSTRAINT high_score__un UNIQUE ( game_level_level_id ),
    CONSTRAINT fk_highscore_level FOREIGN KEY ( game_level_level_id )
        REFERENCES game_level ( level_id )
);

CREATE TABLE pengaturan (
    setting_id   INTEGER PRIMARY KEY,
    volume_musik INTEGER NOT NULL,
    volume_sfx   INTEGER NOT NULL,
    getar_aktif  INTEGER NOT NULL
);

-- Starting data
INSERT INTO game_level VALUES (1, 'On & On - NCS', 'Easy', 'music_onandon_2', 'map_easy.json');
INSERT INTO game_level VALUES (2, 'Safe & Sound - NCS', 'Medium', 'music_safeandsound_1', 'map_medium.json');
INSERT INTO game_level VALUES (3, 'Monkeys Spinning Monkeys', 'Hard', 'music_monkeys_3', 'map_hard.json');
INSERT INTO pengaturan VALUES (1, 60, 75, 1);