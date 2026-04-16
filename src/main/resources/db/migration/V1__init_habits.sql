-- V1: tabele dla habit trackera

CREATE TABLE habits (
                        id              BIGSERIAL PRIMARY KEY,
                        name            VARCHAR(100) NOT NULL,
                        description     VARCHAR(500),
                        frequency       VARCHAR(20)  NOT NULL DEFAULT 'DAILY',
                        archived        BOOLEAN      NOT NULL DEFAULT FALSE,
                        owner_id        VARCHAR(100),
                        created_on      TIMESTAMP    NOT NULL,
                        updated_on      TIMESTAMP    NOT NULL
);

CREATE INDEX idx_habits_owner_id ON habits(owner_id);
CREATE INDEX idx_habits_archived ON habits(archived);

CREATE TABLE habit_entries (
                               id              BIGSERIAL PRIMARY KEY,
                               habit_id        BIGINT       NOT NULL REFERENCES habits(id) ON DELETE CASCADE,
                               entry_date      DATE         NOT NULL,
                               completed       BOOLEAN      NOT NULL DEFAULT TRUE,
                               note            VARCHAR(500),
                               created_on      TIMESTAMP    NOT NULL,
                               CONSTRAINT uk_habit_entry_day UNIQUE (habit_id, entry_date)
);

CREATE INDEX idx_habit_entries_habit_id ON habit_entries(habit_id);
CREATE INDEX idx_habit_entries_date     ON habit_entries(entry_date);