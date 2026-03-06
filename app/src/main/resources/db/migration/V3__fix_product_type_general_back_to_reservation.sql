-- =============================================
-- V3: product_type 'GENERAL' → 'RESERVATION' 복원
-- 이전 세션에서 잘못 변경된 GENERAL 값을 원래 RESERVATION으로 되돌림
-- ProductType: RESERVATION(예약 구매), FLASH_SALE(선착순 구매)
-- =============================================
UPDATE products SET product_type = 'RESERVATION' WHERE product_type = 'GENERAL';
