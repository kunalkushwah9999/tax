CREATE TABLE IF NOT EXISTS workspace_meta (
    id TINYINT UNSIGNED NOT NULL PRIMARY KEY,
    schema_version INT NOT NULL,
    key_nonce VARBINARY(12) NOT NULL,
    key_probe VARBINARY(128) NOT NULL,
    payload_bytes BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT singleton_meta CHECK (id = 1),
    CONSTRAINT schema_v1 CHECK (schema_version = 1),
    CONSTRAINT bounded_payload CHECK (payload_bytes >= 0 AND payload_bytes <= 16777216)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS workspace_records (
    ordinal BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    namespace VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    record_id VARCHAR(180) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    db_revision BIGINT NOT NULL,
    lookup_tag VARBINARY(32) NULL,
    nonce VARBINARY(12) NOT NULL,
    ciphertext MEDIUMBLOB NOT NULL,
    UNIQUE KEY record_identity (namespace, record_id),
    UNIQUE KEY unique_lookup (namespace, lookup_tag),
    CONSTRAINT known_namespace CHECK (namespace IN ('users','cases','grants','tickets','prices','quotes','receipts','audit')),
    CONSTRAINT positive_revision CHECK (db_revision > 0),
    CONSTRAINT nonce_length CHECK (OCTET_LENGTH(nonce) = 12),
    CONSTRAINT record_size CHECK (OCTET_LENGTH(ciphertext) <= 262160)
) ENGINE=InnoDB;
