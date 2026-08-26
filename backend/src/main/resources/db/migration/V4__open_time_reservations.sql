-- Allow reservations without a fixed end time.

ALTER TABLE reservations
    MODIFY COLUMN duration_minutes INT NULL;
