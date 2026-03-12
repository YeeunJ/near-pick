package com.nearpick.domain.product

import com.nearpick.domain.product.dto.MenuOptionGroupRequest
import com.nearpick.domain.product.dto.ProductMenuOptionGroupResponse

interface ProductMenuOptionService {
    fun saveMenuOptions(merchantId: Long, productId: Long, groups: List<MenuOptionGroupRequest>): List<ProductMenuOptionGroupResponse>
    fun deleteMenuOptionGroup(merchantId: Long, productId: Long, groupId: Long)
    fun getMenuOptions(productId: Long): List<ProductMenuOptionGroupResponse>
}
