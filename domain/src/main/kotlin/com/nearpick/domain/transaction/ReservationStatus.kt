package com.nearpick.domain.transaction

enum class ReservationStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    VISITED,
    COMPLETED,   // VISITED 후 즉시 전환
    NO_SHOW,     // 스케줄러 자동 처리 (CONFIRMED + visitScheduledAt+2h 초과)
}
