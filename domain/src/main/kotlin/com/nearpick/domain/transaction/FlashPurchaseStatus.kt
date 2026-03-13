package com.nearpick.domain.transaction

enum class FlashPurchaseStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,   // 소상공인 취소 (재고 복원)
    PICKED_UP,   // 픽업 확인 완료
    COMPLETED,   // 기존 유지 (Kafka 처리 성공 레거시)
    FAILED,      // Consumer 처리 실패 (재고 부족, 상품 없음 등)
}
