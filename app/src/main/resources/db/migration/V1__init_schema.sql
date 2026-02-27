-- =============================================
-- V1: Initial Schema
-- NearPick 전체 테이블 정의
-- =============================================

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE consumer_profiles (
    user_id     BIGINT         NOT NULL,
    nickname    VARCHAR(50)    NOT NULL,
    current_lat DECIMAL(10, 7),
    current_lng DECIMAL(10, 7),
    updated_at  DATETIME(6)    NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_consumer_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE merchant_profiles (
    user_id          BIGINT         NOT NULL,
    business_name    VARCHAR(100)   NOT NULL,
    business_reg_no  VARCHAR(20)    NOT NULL,
    shop_lat         DECIMAL(10, 7) NOT NULL,
    shop_lng         DECIMAL(10, 7) NOT NULL,
    shop_address     VARCHAR(255),
    rating           DECIMAL(3, 2)  NOT NULL DEFAULT 0.00,
    is_verified      BIT(1)         NOT NULL DEFAULT 0,
    updated_at       DATETIME(6)    NOT NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_merchant_business_reg_no (business_reg_no),
    CONSTRAINT fk_merchant_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE admin_profiles (
    user_id     BIGINT      NOT NULL,
    admin_level VARCHAR(20) NOT NULL DEFAULT 'OPERATOR',
    permissions TEXT        NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_admin_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE products (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    merchant_id     BIGINT         NOT NULL,
    title           VARCHAR(100)   NOT NULL,
    description     TEXT,
    price           INT            NOT NULL,
    product_type    VARCHAR(20)    NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    stock           INT            NOT NULL DEFAULT 0,
    available_from  DATETIME(6),
    available_until DATETIME(6),
    shop_lat        DECIMAL(10, 7) NOT NULL,
    shop_lng        DECIMAL(10, 7) NOT NULL,
    view_count      INT            NOT NULL DEFAULT 0,
    created_at      DATETIME(6)    NOT NULL,
    updated_at      DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_products_location (shop_lat, shop_lng),
    INDEX idx_products_status_type (status, product_type),
    INDEX idx_products_merchant (merchant_id),
    CONSTRAINT fk_product_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE popularity_scores (
    product_id       BIGINT        NOT NULL,
    score            DECIMAL(10, 4) NOT NULL DEFAULT 0.0000,
    view_weight      INT            NOT NULL DEFAULT 0,
    wishlist_weight  INT            NOT NULL DEFAULT 0,
    purchase_weight  INT            NOT NULL DEFAULT 0,
    calculated_at    DATETIME(6)    NOT NULL,
    PRIMARY KEY (product_id),
    INDEX idx_popularity_score (score DESC),
    CONSTRAINT fk_popularity_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE wishlists (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    product_id BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_wishlist_user_product (user_id, product_id),
    INDEX idx_wishlists_user (user_id),
    CONSTRAINT fk_wishlist_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_wishlist_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE reservations (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    user_id             BIGINT      NOT NULL,
    product_id          BIGINT      NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    quantity            INT         NOT NULL DEFAULT 1,
    memo                TEXT,
    visit_scheduled_at  DATETIME(6),
    reserved_at         DATETIME(6) NOT NULL,
    updated_at          DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_reservations_user (user_id),
    CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reservation_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE flash_purchases (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL,
    product_id   BIGINT      NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    quantity     INT         NOT NULL DEFAULT 1,
    purchased_at DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_flash_purchases_user (user_id),
    CONSTRAINT fk_flash_purchase_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_flash_purchase_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
