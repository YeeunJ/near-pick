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
}
