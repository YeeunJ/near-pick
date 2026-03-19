package com.nearpick.common.exception

enum class ErrorCode(
    val httpStatus: Int,
    val message: String,
) {
    // Common
    INVALID_INPUT(400, "Invalid input value"),
    RESOURCE_NOT_FOUND(404, "Resource not found"),
    INTERNAL_SERVER_ERROR(500, "Internal server error"),

    // Auth
    DUPLICATE_EMAIL(409, "Email already exists"),
    DUPLICATE_BUSINESS_REG_NO(409, "Business registration number already exists"),
    INVALID_CREDENTIALS(401, "Invalid email or password"),
    ACCOUNT_SUSPENDED(403, "Account is suspended"),
    UNAUTHORIZED(401, "Authentication required"),
    FORBIDDEN(403, "Access denied"),

    // Domain
    USER_NOT_FOUND(404, "User not found"),
    PRODUCT_NOT_FOUND(404, "Product not found"),
    RESERVATION_NOT_FOUND(404, "Reservation not found"),
    FLASH_PURCHASE_NOT_FOUND(404, "Flash purchase not found"),

    // Business
    OUT_OF_STOCK(409, "Insufficient stock"),
    ALREADY_WISHLISTED(409, "Already in wishlist"),
    PRODUCT_NOT_ACTIVE(422, "Product is not available"),
    RESERVATION_CANNOT_BE_CANCELLED(422, "Reservation cannot be cancelled in current status"),
    RESERVATION_CANNOT_BE_CONFIRMED(422, "Reservation cannot be confirmed in current status"),

    // Flash Purchase (Phase 9)
    FLASH_PURCHASE_LOCK_FAILED(409, "선착순 처리 중 충돌이 발생했습니다. 다시 시도해주세요."),
    FLASH_PURCHASE_UNAVAILABLE(503, "선착순 구매 서비스가 일시적으로 이용 불가합니다."),

    // Location (Phase 10)
    SAVED_LOCATION_LIMIT_EXCEEDED(400, "저장 위치는 최대 5개까지 등록 가능합니다."),
    SAVED_LOCATION_NOT_FOUND(404, "저장 위치를 찾을 수 없습니다."),
    LOCATION_NOT_SET(400, "현재 위치가 설정되지 않았습니다."),
    EXTERNAL_API_UNAVAILABLE(503, "외부 API를 사용할 수 없습니다."),
    CONSUMER_NOT_FOUND(404, "소비자 프로필을 찾을 수 없습니다."),

    // Product Enhancement (Phase 11)
    PRODUCT_IMAGE_LIMIT_EXCEEDED(400, "상품 이미지는 최대 5장까지 등록 가능합니다."),
    PRODUCT_IMAGE_NOT_FOUND(404, "상품 이미지를 찾을 수 없습니다."),
    INVALID_IMAGE_TYPE(400, "허용되지 않는 이미지 형식입니다. (허용: jpg, jpeg, png, webp)"),
    MENU_OPTION_NOT_ALLOWED(400, "메뉴 옵션은 음식/음료 카테고리 상품에만 등록 가능합니다."),
    MENU_OPTION_GROUP_NOT_FOUND(404, "메뉴 옵션 그룹을 찾을 수 없습니다."),

    // Purchase Lifecycle (Phase 12)
    PRODUCT_NOT_AVAILABLE_YET(422, "아직 판매 시작 전인 상품입니다."),
    PRODUCT_AVAILABILITY_EXPIRED(422, "판매 기간이 종료된 상품입니다."),
    PRODUCT_FORCE_CLOSED(403, "관리자에 의해 강제 종료된 상품입니다."),
    PRODUCT_CANNOT_BE_RESUMED(422, "일시정지 상태인 상품만 재개할 수 있습니다."),
    PRODUCT_CANNOT_BE_PAUSED(422, "활성 상태인 상품만 일시정지할 수 있습니다."),
    RESERVATION_VISIT_CODE_INVALID(404, "유효하지 않은 방문 코드입니다."),
    RESERVATION_ALREADY_COMPLETED(422, "이미 완료된 예약입니다."),
    FLASH_PURCHASE_CANNOT_BE_CANCELLED(422, "취소할 수 없는 선착순 구매입니다."),
    FLASH_PURCHASE_PICKUP_CODE_INVALID(404, "유효하지 않은 픽업 코드입니다."),

    // Review System (Phase 13)
    REVIEW_NOT_FOUND(404, "리뷰를 찾을 수 없습니다."),
    REVIEW_ALREADY_EXISTS(422, "이미 해당 거래에 대한 리뷰가 존재합니다."),
    REVIEW_NOT_ELIGIBLE(422, "구매 또는 방문 완료 후에만 리뷰를 작성할 수 있습니다."),
    REVIEW_BLINDED(403, "블라인드 처리된 리뷰입니다."),
    REVIEW_REPLY_ALREADY_EXISTS(422, "이미 답글이 존재합니다."),
    REVIEW_REPLY_NOT_FOUND(404, "답글을 찾을 수 없습니다."),
    REVIEW_IMAGE_LIMIT_EXCEEDED(422, "리뷰 이미지는 최대 3장까지 등록 가능합니다."),
}
