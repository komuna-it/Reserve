ALTER TABLE reservations
    RENAME COLUMN reserved_by_user_id TO user_id;

ALTER TABLE reservations
    ADD COLUMN actor_id BIGINT;

UPDATE reservations
SET actor_id = -1;

ALTER TABLE reservations
    ALTER COLUMN actor_id SET NOT NULL;

ALTER TABLE reservations
    ADD CONSTRAINT fk_reservations_actor
        FOREIGN KEY (actor_id)
        REFERENCES users (id);

CREATE INDEX idx_reservations_actor_id
    ON reservations (actor_id);