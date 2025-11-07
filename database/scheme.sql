USE memory_game_db;

CREATE TABLE Player (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    passwordHash VARCHAR(255) NOT NULL,
    totalWins INT DEFAULT 0,
    status ENUM('ONLINE', 'OFFLINE', 'BUSY') NOT NULL DEFAULT 'OFFLINE'
);

CREATE TABLE Vocabulary (
    id INT AUTO_INCREMENT PRIMARY KEY,
    phrase VARCHAR(255) NOT NULL,
    length INT NOT NULL
);

CREATE TABLE MatchHistory (
    matchId INT AUTO_INCREMENT PRIMARY KEY,
    player1_id INT NOT NULL,
    player2_id INT NOT NULL,
    player1_score INT DEFAULT 0,
    player2_score INT DEFAULT 0,
    -- Id người thắng, có thể là NULL nếu hòa 
    winner_id INT,
    playedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player1_id) REFERENCES Player(id),
    FOREIGN KEY (player2_id) REFERENCES Player(id),
    FOREIGN KEY (winner_id) REFERENCES Player(id)
);

