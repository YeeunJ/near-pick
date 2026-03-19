-- reviews.rating: TINYINT → INT
-- Hibernate maps Kotlin Int to INTEGER (Types#INTEGER), not TINYINT
ALTER TABLE reviews MODIFY COLUMN rating INT NOT NULL;
