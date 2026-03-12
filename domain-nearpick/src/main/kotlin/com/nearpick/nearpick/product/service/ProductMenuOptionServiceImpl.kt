package com.nearpick.nearpick.product.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.product.ProductCategory
import com.nearpick.domain.product.ProductMenuOptionService
import com.nearpick.domain.product.dto.MenuChoiceResponse
import com.nearpick.domain.product.dto.MenuOptionGroupRequest
import com.nearpick.domain.product.dto.ProductMenuOptionGroupResponse
import com.nearpick.nearpick.product.entity.ProductMenuChoiceEntity
import com.nearpick.nearpick.product.entity.ProductMenuOptionGroupEntity
import com.nearpick.nearpick.product.repository.ProductMenuOptionGroupRepository
import com.nearpick.nearpick.product.repository.ProductRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val FOOD_CATEGORIES = setOf(ProductCategory.FOOD, ProductCategory.BEVERAGE)

@Service
@Transactional(readOnly = true)
class ProductMenuOptionServiceImpl(
    private val productRepository: ProductRepository,
    private val menuOptionGroupRepository: ProductMenuOptionGroupRepository,
) : ProductMenuOptionService {

    @Transactional
    @CacheEvict(cacheNames = ["products-detail"], key = "#productId")
    override fun saveMenuOptions(
        merchantId: Long,
        productId: Long,
        groups: List<MenuOptionGroupRequest>,
    ): List<ProductMenuOptionGroupResponse> {
        val product = productRepository.findById(productId)
            .orElseThrow { BusinessException(ErrorCode.PRODUCT_NOT_FOUND) }
        if (product.merchant.userId != merchantId) throw BusinessException(ErrorCode.FORBIDDEN)
        if (product.category !in FOOD_CATEGORIES) throw BusinessException(ErrorCode.MENU_OPTION_NOT_ALLOWED)

        // 전체 교체: 기존 그룹 삭제 후 재저장
        menuOptionGroupRepository.deleteAllByProductId(productId)
        menuOptionGroupRepository.flush()

        val saved = groups.map { req ->
            val group = ProductMenuOptionGroupEntity(
                product = product,
                name = req.name,
                required = req.required,
                maxSelect = req.maxSelect,
                displayOrder = req.displayOrder,
            )
            req.choices.forEach { choiceReq ->
                group.choices.add(
                    ProductMenuChoiceEntity(
                        group = group,
                        name = choiceReq.name,
                        additionalPrice = choiceReq.additionalPrice,
                        displayOrder = choiceReq.displayOrder,
                    )
                )
            }
            menuOptionGroupRepository.save(group)
        }
        return saved.map { it.toResponse() }
    }

    @Transactional
    @CacheEvict(cacheNames = ["products-detail"], key = "#productId")
    override fun deleteMenuOptionGroup(merchantId: Long, productId: Long, groupId: Long) {
        val product = productRepository.findById(productId)
            .orElseThrow { BusinessException(ErrorCode.PRODUCT_NOT_FOUND) }
        if (product.merchant.userId != merchantId) throw BusinessException(ErrorCode.FORBIDDEN)

        val group = menuOptionGroupRepository.findByIdAndProductId(groupId, productId)
            ?: throw BusinessException(ErrorCode.MENU_OPTION_GROUP_NOT_FOUND)
        menuOptionGroupRepository.delete(group)
    }

    override fun getMenuOptions(productId: Long): List<ProductMenuOptionGroupResponse> =
        menuOptionGroupRepository
            .findAllByProductIdOrderByDisplayOrder(productId)
            .map { it.toResponse() }

    private fun ProductMenuOptionGroupEntity.toResponse() = ProductMenuOptionGroupResponse(
        id = id,
        name = name,
        required = required,
        maxSelect = maxSelect,
        displayOrder = displayOrder,
        choices = choices
            .sortedBy { it.displayOrder }
            .map { MenuChoiceResponse(it.id, it.name, it.additionalPrice, it.displayOrder) },
    )
}
