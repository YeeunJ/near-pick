package com.nearpick.domain.product.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class MenuChoiceRequest(
    @field:NotBlank @field:Size(max = 50) val name: String,
    @field:Min(0) val additionalPrice: Int = 0,
    val displayOrder: Int = 0,
)

data class MenuOptionGroupRequest(
    @field:NotBlank @field:Size(max = 50) val name: String,
    val required: Boolean = false,
    @field:Min(1) val maxSelect: Int = 1,
    val displayOrder: Int = 0,
    val choices: List<MenuChoiceRequest> = emptyList(),
)

data class MenuChoiceResponse(
    val id: Long,
    val name: String,
    val additionalPrice: Int,
    val displayOrder: Int,
)

data class ProductMenuOptionGroupResponse(
    val id: Long,
    val name: String,
    val required: Boolean,
    val maxSelect: Int,
    val displayOrder: Int,
    val choices: List<MenuChoiceResponse>,
)
