CREATE TABLE IF NOT EXISTS workspace_recovery (
    id TINYINT UNSIGNED NOT NULL PRIMARY KEY,
    state VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    CONSTRAINT singleton_recovery CHECK (id = 1),
    CONSTRAINT known_recovery_state CHECK (state IN ('ACTIVE', 'ISOLATED'))
) ENGINE=InnoDB;
