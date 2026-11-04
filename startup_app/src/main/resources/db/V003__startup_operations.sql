ALTER TABLE workspace_records
    DROP CHECK known_namespace,
    ADD CONSTRAINT known_namespace CHECK (namespace IN ('users','cases','grants','tickets','prices','quotes','receipts','operations','audit'));
