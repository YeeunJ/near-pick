package com.nearpick.domain.transaction

enum class FlashPurchaseStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED,
    FAILED,       // Consumer 처리 실패 (재고 부족, 상품 없음 등)
}
