package com.nearpick.nearpick.user.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.admin.AdminService
import com.nearpick.domain.admin.dto.AdminProductItem
import com.nearpick.domain.admin.dto.AdminProfileResponse
import com.nearpick.domain.admin.dto.UserSummary
import com.nearpick.domain.product.ProductStatus
import com.nearpick.domain.user.UserRole
import com.nearpick.domain.user.UserStatus
import com.nearpick.nearpick.product.mapper.ProductMapper.toAdminItem
import com.nearpick.nearpick.product.repository.ProductRepository
import com.nearpick.nearpick.user.mapper.AdminMapper.toProfileResponse
import com.nearpick.nearpick.user.mapper.AdminMapper.toUserSummary
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.nearpick.nearpick.user.repository.AdminProfileRepository
import com.nearpick.nearpick.user.repository.UserRepository

@Service
@Transactional(readOnly = true)
class AdminServiceImpl(
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val adminProfileRepository: AdminProfileRepository,
) : AdminService {

    override fun getUsers(
        role: UserRole?,
        status: UserStatus?,
        query: String?,
        page: Int,
        size: Int,
    ): Page<UserSummary> =
        userRepository.searchUsers(
            role = role,
            status = status,
            query = query,
            pageable = PageRequest.of(page, size),
        ).map { it.toUserSummary() }

    @Transactional
    override fun suspendUser(userId: Long): UserSummary {
        val user = userRepository.findById(userId).orElseThrow {
            BusinessException(ErrorCode.USER_NOT_FOUND)
        }
        user.status = UserStatus.SUSPENDED
        return user.toUserSummary()
    }

    @Transactional
    override fun withdrawUser(userId: Long): UserSummary {
        val user = userRepository.findById(userId).orElseThrow {
            BusinessException(ErrorCode.USER_NOT_FOUND)
        }
        user.status = UserStatus.WITHDRAWN
        return user.toUserSummary()
    }

    override fun getProducts(status: ProductStatus?, page: Int, size: Int): Page<AdminProductItem> =
        productRepository.findAllByOptionalStatus(
            status = status,
            pageable = PageRequest.of(page, size),
        ).map { it.toAdminItem() }

    @Transactional
    override fun forceCloseProduct(productId: Long): AdminProductItem {
        val product = productRepository.findById(productId).orElseThrow {
            BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
        }
        product.status = ProductStatus.CLOSED
        return product.toAdminItem()
    }

    override fun getProfile(adminId: Long): AdminProfileResponse {
        val admin = adminProfileRepository.findById(adminId).orElseThrow {
            BusinessException(ErrorCode.USER_NOT_FOUND)
        }
        return admin.toProfileResponse()
    }
}
