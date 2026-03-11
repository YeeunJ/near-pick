CREATE TABLE saved_locations
(
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    consumer_id BIGINT        NOT NULL,
    label       VARCHAR(50)   NOT NULL,
    lat         DECIMAL(10, 7) NOT NULL,
    lng         DECIMAL(10, 7) NOT NULL,
    is_default  BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_saved_location_consumer
        FOREIGN KEY (consumer_id) REFERENCES consumer_profiles (user_id)
            ON DELETE CASCADE
);

CREATE INDEX idx_saved_location_consumer_id ON saved_locations (consumer_id);
