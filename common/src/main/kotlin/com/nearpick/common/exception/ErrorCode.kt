package com.nearpick.common.exception

enum class ErrorCode(
    val httpStatus: Int,
    val message: String,
) {
    // Common
    INVALID_INPUT(400, "Invalid input value"),
    RESOURCE_NOT_FOUND(404, "Resource not found"),
    INTERNAL_SERVER_ERROR(500, "Internal server error"),
}
