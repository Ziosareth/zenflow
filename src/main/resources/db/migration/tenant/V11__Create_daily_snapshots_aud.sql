-- Audit table for daily_snapshots (Hibernate Envers)
CREATE TABLE IF NOT EXISTS daily_snapshots_aud (
    rev         INTEGER NOT NULL,
    revtype     SMALLINT,
    id          BIGINT  NOT NULL,
    date        DATE,
    sprint_id   BIGINT,
    scope_total INTEGER,
    remaining   INTEGER,
    CONSTRAINT pk_daily_snapshots_aud PRIMARY KEY (rev, id)
);

-- Link to Envers revision table
ALTER TABLE daily_snapshots_aud
    ADD CONSTRAINT fk_daily_snapshots_aud_on_rev FOREIGN KEY (rev) REFERENCES revinfo (id);
