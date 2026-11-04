CREATE TABLE IF NOT EXISTS workspace_concurrency (
    namespace VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    shard TINYINT UNSIGNED NOT NULL,
    epoch BIGINT NOT NULL DEFAULT 0,
    record_count BIGINT NOT NULL DEFAULT 0,
    payload_bytes BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (namespace,shard),
    CONSTRAINT concurrency_shard CHECK (shard < 32),
    CONSTRAINT concurrency_counters CHECK (epoch >= 0 AND record_count >= 0 AND payload_bytes >= 0)
) ENGINE=InnoDB;
