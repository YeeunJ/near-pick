package com.nearpick.nearpick.admin

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.admin.AdminService
import com.nearpick.domain.admin.dto.AdminProductResponse
import com.nearpick.domain.admin.dto.AdminProductStatusResponse
import com.nearpick.domain.admin.dto.AdminUserResponse
import com.nearpick.domain.admin.dto.AdminUserStatusResponse
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.user.UserRole
import com.nearpick.domain.user.UserStatus
import com.nearpick.nearpick.product.ProductRepository
import com.nearpick.nearpick.user.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminServiceImpl(
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
) : AdminService {

    @Transactional(readOnly = true)
    override fun getUsers(
        role: UserRole?,
        status: UserStatus?,
        query: String?,
        page: Int,
        size: Int,
    ): Page<AdminUserResponse> =
        userRepository.searchUsers(role, status, query, PageRequest.of(page, size)).map { u ->
            AdminUserResponse(
                id = u.id,
                email = u.email,
                role = u.role,
                status = u.status,
                createdAt = u.createdAt,
            )
        }

    @Transactional
    override fun suspendUser(userId: Long): AdminUserStatusResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        user.status = UserStatus.SUSPENDED
        return AdminUserStatusResponse(id = user.id, status = user.status)
    }

    @Transactional
    override fun deleteUser(userId: Long) {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        userRepository.delete(user)
    }

    @Transactional(readOnly = true)
    override fun getProducts(status: ProductStatus?, page: Int, size: Int): Page<AdminProductResponse> =
        productRepository.findAllByOptionalStatus(status, PageRequest.of(page, size)).map { p ->
            AdminProductResponse(
                id = p.id,
                title = p.title,
                merchantName = p.merchant.businessName,
                status = p.status,
                price = p.price,
            )
        }

    @Transactional
    override fun forceClose(productId: Long): AdminProductStatusResponse {
        val product = productRepository.findById(productId)
            .orElseThrow { BusinessException(ErrorCode.PRODUCT_NOT_FOUND) }
        product.status = ProductStatus.FORCE_CLOSED
        return AdminProductStatusResponse(id = product.id, status = product.status)
    }
}
