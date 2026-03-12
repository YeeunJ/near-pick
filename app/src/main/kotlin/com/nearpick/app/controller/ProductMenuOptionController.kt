package com.nearpick.app.controller

import com.nearpick.common.response.ApiResponse
import com.nearpick.domain.product.ProductMenuOptionService
import com.nearpick.domain.product.dto.MenuOptionGroupRequest
import com.nearpick.domain.product.dto.ProductMenuOptionGroupResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "Product Menu Options", description = "음식/음료 상품 메뉴 옵션 관리 API")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/products/{productId}/menu-options")
@PreAuthorize("hasRole('MERCHANT')")
class ProductMenuOptionController(
    private val productMenuOptionService: ProductMenuOptionService,
) {

    @Operation(summary = "메뉴 옵션 일괄 등록", description = "기존 옵션을 전체 교체한다. FOOD/BEVERAGE 카테고리만 허용.")
    @PostMapping
    fun saveMenuOptions(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
        @Valid @RequestBody groups: List<MenuOptionGroupRequest>,
    ): ResponseEntity<ApiResponse<List<ProductMenuOptionGroupResponse>>> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(productMenuOptionService.saveMenuOptions(userId, productId, groups)))

    @Operation(summary = "메뉴 옵션 그룹 삭제")
    @DeleteMapping("/{groupId}")
    fun deleteMenuOptionGroup(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
        @PathVariable groupId: Long,
    ): ResponseEntity<Void> {
        productMenuOptionService.deleteMenuOptionGroup(userId, productId, groupId)
        return ResponseEntity.noContent().build()
    }
}
