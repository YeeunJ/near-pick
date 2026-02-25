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
    INVALID_CREDENTIALS(401, "Invalid email or password"),
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
}
