package com.nearpick.domain.review

enum class ReviewStatus {
    ACTIVE,    // 정상 노출
    BLINDED,   // AI 또는 관리자 블라인드
    DELETED,   // 소비자 삭제 (soft delete)
}
