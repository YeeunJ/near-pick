-- reviews
CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    reservation_id BIGINT NULL UNIQUE,
    flash_purchase_id BIGINT NULL UNIQUE,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ai_checked BOOLEAN NOT NULL DEFAULT FALSE,
    ai_result VARCHAR(20) NULL,
    blinded_reason VARCHAR(100) NULL,
    blind_pending BOOLEAN NOT NULL DEFAULT FALSE,
    report_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- review_images
CREATE TABLE review_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    image_url VARCHAR(1000) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    FOREIGN KEY (review_id) REFERENCES reviews(id)
);

-- review_replies
CREATE TABLE review_replies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL UNIQUE,
    merchant_id BIGINT NOT NULL,
    content VARCHAR(300) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (review_id) REFERENCES reviews(id),
    FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(user_id)
);

-- products 평점 집계 필드 추가
ALTER TABLE products
    ADD COLUMN average_rating DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN review_count INT NOT NULL DEFAULT 0;

-- 인덱스
CREATE INDEX idx_reviews_product_id ON reviews (product_id, status);
CREATE INDEX idx_reviews_user_id ON reviews (user_id);
CREATE INDEX idx_reviews_report_count ON reviews (report_count);
CREATE INDEX idx_review_images_review_id ON review_images (review_id);
