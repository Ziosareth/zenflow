-- Create table for daily sprint snapshots (scope and remaining)
CREATE TABLE IF NOT EXISTS daily_snapshots (
    id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL,
    sprint_id BIGINT NOT NULL,
    scope_total INTEGER,
    remaining INTEGER,
    CONSTRAINT uq_daily_snapshots_date_sprint UNIQUE (date, sprint_id),
    CONSTRAINT fk_daily_snapshots_sprint FOREIGN KEY (sprint_id)
        REFERENCES sprints(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_daily_snapshots_sprint_date ON daily_snapshots(sprint_id, date);
