-- V6: Purchase Lifecycle — visit_code, completed_at, pickup_code, picked_up_at

-- reservations 테이블
ALTER TABLE reservations
    ADD COLUMN visit_code   VARCHAR(6)  NULL COMMENT '방문 확인 코드 (confirm 시 생성)',
    ADD COLUMN completed_at DATETIME    NULL COMMENT '방문 완료 시각';

-- flash_purchases 테이블
ALTER TABLE flash_purchases
    ADD COLUMN pickup_code  VARCHAR(6)  NULL COMMENT '픽업 확인 코드 (CONFIRMED 시 생성)',
    ADD COLUMN picked_up_at DATETIME    NULL COMMENT '픽업 완료 시각';

-- 방문 코드 조회용 인덱스 (소상공인 코드 입력 → 예약 검색)
CREATE INDEX idx_reservations_visit_code ON reservations (visit_code);

-- 픽업 코드 조회용 인덱스
CREATE INDEX idx_flash_purchases_pickup_code ON flash_purchases (pickup_code);

-- 스케줄러용 복합 인덱스 (status + visit_scheduled_at)
CREATE INDEX idx_reservations_status_scheduled ON reservations (status, visit_scheduled_at);
