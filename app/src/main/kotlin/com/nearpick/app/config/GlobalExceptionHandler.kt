package com.nearpick.app.config

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.response.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(e: BusinessException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity
            .status(e.errorCode.httpStatus)
            .body(ApiResponse.error(e.errorCode.message))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val message = e.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.badRequest().body(ApiResponse.error(message))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneral(e: Exception): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.internalServerError().body(ApiResponse.error("Internal server error"))
}
