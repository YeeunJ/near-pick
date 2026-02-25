package com.nearpick.nearpick.user

import com.nearpick.domain.user.UserRole
import com.nearpick.domain.user.UserStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<UserEntity, Long> {
    fun findByEmail(email: String): UserEntity?
    fun existsByEmail(email: String): Boolean

    @Query("""
        SELECT u FROM UserEntity u
        WHERE (:role IS NULL OR u.role = :role)
          AND (:status IS NULL OR u.status = :status)
          AND (:query IS NULL OR u.email LIKE %:query%)
        ORDER BY u.createdAt DESC
    """)
    fun searchUsers(
        @Param("role") role: UserRole?,
        @Param("status") status: UserStatus?,
        @Param("query") query: String?,
        pageable: Pageable,
    ): Page<UserEntity>
}
