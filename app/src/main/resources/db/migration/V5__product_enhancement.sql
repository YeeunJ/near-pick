-- =============================================
-- Phase 11: 상품 고도화 (이미지, 카테고리, 메뉴 옵션)
-- =============================================

-- 1. products 테이블에 category, specs 컬럼 추가
ALTER TABLE products
    ADD COLUMN category VARCHAR(20) NULL AFTER product_type,
    ADD COLUMN specs TEXT NULL AFTER description;

CREATE INDEX idx_products_category ON products (category);

-- 2. product_images 테이블
CREATE TABLE product_images
(
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    product_id    BIGINT        NOT NULL,
    s3_key        VARCHAR(500)  NOT NULL,
    url           VARCHAR(1000) NOT NULL,
    display_order INT           NOT NULL DEFAULT 0,
    created_at    DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_image_product
        FOREIGN KEY (product_id) REFERENCES products (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_product_images_product_id ON product_images (product_id);

-- 3. product_menu_option_groups 테이블
CREATE TABLE product_menu_option_groups
(
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    product_id    BIGINT      NOT NULL,
    name          VARCHAR(50) NOT NULL,
    required      BOOLEAN     NOT NULL DEFAULT FALSE,
    max_select    INT         NOT NULL DEFAULT 1,
    display_order INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_menu_option_group_product
        FOREIGN KEY (product_id) REFERENCES products (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_menu_option_groups_product_id ON product_menu_option_groups (product_id);

-- 4. product_menu_choices 테이블
CREATE TABLE product_menu_choices
(
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    group_id         BIGINT      NOT NULL,
    name             VARCHAR(50) NOT NULL,
    additional_price INT         NOT NULL DEFAULT 0,
    display_order    INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_menu_choice_group
        FOREIGN KEY (group_id) REFERENCES product_menu_option_groups (id)
            ON DELETE CASCADE
);
