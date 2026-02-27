-- =============================================
-- V2: 로컬 개발용 더미 데이터
-- 모든 계정 비밀번호: test1234
-- INSERT IGNORE: 중복 실행 시 건너뜀
-- =============================================

-- =============================================
-- 사용자 (users)
-- =============================================
INSERT IGNORE INTO users (id, email, password_hash, role, status, created_at, updated_at)
VALUES
    (1, 'consumer1@test.com', '$2a$10$nPWVXWTUxXbrmUKMuM6TCeH5x.BUtscOYJFWg/nHZ9E6SWmLzMjFO', 'CONSUMER', 'ACTIVE', NOW(), NOW()),
    (2, 'consumer2@test.com', '$2a$10$GWBAlSKZ/Orm2jCM5X688.juQwoULFmcdC67tFoGtDZ/p/qurSVt.', 'CONSUMER', 'ACTIVE', NOW(), NOW()),
    (3, 'consumer3@test.com', '$2a$10$tvi1XERBWZ38jy55Rim9A.gEIRLDpYCzmxrgX031TcZphKme9AGCa', 'CONSUMER', 'ACTIVE', NOW(), NOW()),
    (4, 'merchant1@test.com', '$2a$10$qBN7GNq6qnyMsS.RkS2zvegvU9BtETDb6d6deNRsE5Op3fpazukEG', 'MERCHANT', 'ACTIVE', NOW(), NOW()),
    (5, 'merchant2@test.com', '$2a$10$ecdhzLpcB2Ve7Yk39/.NJ.lSRsdBBoDjJCx26lhjYgcxQW3AhOfuC', 'MERCHANT', 'ACTIVE', NOW(), NOW()),
    (6, 'admin@test.com',     '$2a$10$etGuSpWPjwsSWzYfUS7hyu9ug/7c.gXVT5RaVq/LSQ6JHFWiR5ztm', 'ADMIN',    'ACTIVE', NOW(), NOW());

-- =============================================
-- 소비자 프로필 (consumer_profiles)
-- =============================================
INSERT IGNORE INTO consumer_profiles (user_id, nickname, current_lat, current_lng, updated_at)
VALUES
    (1, '강남맛집탐방단', 37.4989, 127.0267, NOW()),
    (2, '홍대버거마니아', 37.5563, 126.9236, NOW()),
    (3, '마포구민A',     37.5630, 126.9390, NOW());

-- =============================================
-- 소상공인 프로필 (merchant_profiles)
-- =============================================
INSERT IGNORE INTO merchant_profiles (user_id, business_name, business_reg_no, shop_lat, shop_lng, shop_address, rating, is_verified, updated_at)
VALUES
    (4, '강남 분식천국', '123-45-67890', 37.4990, 127.0265, '서울시 강남구 테헤란로 123', 4.50, 1, NOW()),
    (5, '홍대 수제샌드위치', '987-65-43210', 37.5555, 126.9240, '서울시 마포구 홍익로 45', 4.20, 1, NOW());

-- =============================================
-- 관리자 프로필 (admin_profiles)
-- =============================================
INSERT IGNORE INTO admin_profiles (user_id, admin_level, permissions)
VALUES
    (6, 'SUPER', '["USER_BAN","PRODUCT_DEACTIVATE","PLATFORM_MONITOR"]');

-- =============================================
-- 상품 (products)
-- =============================================
INSERT IGNORE INTO products (id, merchant_id, title, description, price, product_type, status, stock, available_from, available_until, shop_lat, shop_lng, view_count, created_at, updated_at)
VALUES
    (1, 4, '오늘의 특가 떡볶이 세트',      '로제소스 떡볶이 + 순대 + 어묵. 매일 오후 2~5시 한정 수량 판매!', 6500,  'FLASH_SALE',  'ACTIVE', 30, NOW(), DATE_ADD(NOW(), INTERVAL 3 HOUR), 37.4990, 127.0265, 42, NOW(), NOW()),
    (2, 5, '수제 크로크무슈 런치',           '고다 치즈와 베이컨이 가득한 수제 샌드위치. 11~14시 픽업 예약 가능.', 9800,  'RESERVATION', 'ACTIVE', 10, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 8 DAY), 37.5555, 126.9240, 27, NOW(), NOW()),
    (3, 4, '순대볶음 + 라면',               '매콤한 순대볶음에 라면 추가. 저녁 5시 이후 판매.',            5500,  'FLASH_SALE',  'ACTIVE', 15, NOW(), DATE_ADD(NOW(), INTERVAL 4 HOUR), 37.4990, 127.0265, 18, NOW(), NOW()),
    (4, 5, '아보카도 에그 샌드위치',         '신선한 아보카도와 반숙란의 조합. 주말 한정 메뉴.', 11000, 'RESERVATION', 'ACTIVE', 5,  DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY), 37.5555, 126.9240, 9,  NOW(), NOW()),
    (5, 4, '김치찌개 정식',                 '묵은지 김치찌개 + 공기밥 + 밑반찬 3종.', 8000,  'RESERVATION', 'PAUSED', 20, NULL, NULL, 37.4990, 127.0265, 5,  NOW(), NOW());

-- =============================================
-- 인기도 점수 (popularity_scores)
-- score = view_count × 1 + wishlist × 3 + purchase × 5
-- =============================================
INSERT IGNORE INTO popularity_scores (product_id, score, view_weight, wishlist_weight, purchase_weight, calculated_at)
VALUES
    (1, 67.0000, 42, 3, 2, NOW()),
    (2, 48.0000, 27, 3, 0, NOW()),
    (3, 23.0000, 18, 1, 1, NOW()),
    (4,  9.0000,  9, 0, 0, NOW()),
    (5,  5.0000,  5, 0, 0, NOW());

-- =============================================
-- 찜 목록 (wishlists)
-- =============================================
INSERT IGNORE INTO wishlists (id, user_id, product_id, created_at)
VALUES
    (1, 1, 1, NOW()),
    (2, 1, 2, NOW()),
    (3, 2, 1, NOW()),
    (4, 2, 3, NOW()),
    (5, 3, 2, NOW()),
    (6, 3, 4, NOW());

-- =============================================
-- 예약 (reservations)
-- =============================================
INSERT IGNORE INTO reservations (id, user_id, product_id, status, quantity, memo, visit_scheduled_at, reserved_at, updated_at)
VALUES
    (1, 1, 2, 'CONFIRMED', 2, '창가 자리 부탁드려요',  DATE_ADD(NOW(), INTERVAL 2 DAY), NOW(), NOW()),
    (2, 2, 2, 'PENDING',   1, NULL,                     DATE_ADD(NOW(), INTERVAL 3 DAY), NOW(), NOW()),
    (3, 3, 4, 'PENDING',   1, '알레르기: 없음',          DATE_ADD(NOW(), INTERVAL 2 DAY), NOW(), NOW());

-- =============================================
-- 선착순 구매 (flash_purchases)
-- =============================================
INSERT IGNORE INTO flash_purchases (id, user_id, product_id, status, quantity, purchased_at, updated_at)
VALUES
    (1, 1, 1, 'CONFIRMED', 1, NOW(), NOW()),
    (2, 2, 3, 'PENDING',   2, NOW(), NOW());
