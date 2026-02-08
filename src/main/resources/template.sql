CREATE TABLE IF NOT EXISTS `player_keys` (
                                             `player` VARCHAR(45) NOT NULL,
                                             `key_name` VARCHAR(45) NOT NULL,
                                             `count` INT NOT NULL DEFAULT 0,
                                             PRIMARY KEY (`player`, `key_name`))
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `prize_history` (
                                               `opener` VARCHAR(45) NULL,
                                               `case_name` VARCHAR(45) NULL,
                                               `luck_name` VARCHAR(45) NULL,
                                               `prize_name` TEXT NULL,
                                               `prize_material` VARCHAR(45) NULL,
                                               `timestamp` BIGINT(16) NULL,
                                               `server_name` VARCHAR(45) NULL)
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `player_luck` (
                                             `player` VARCHAR(45) NOT NULL,
                                             `luck_name` VARCHAR(45) NOT NULL,
                                             `count` INT NOT NULL DEFAULT 0,
                                             PRIMARY KEY (`player`, `luck_name`))
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;