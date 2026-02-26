package com.nearpick.nearpick.user.entity

import com.nearpick.domain.user.AdminLevel
import jakarta.persistence.*

@Entity
@Table(name = "admin_profiles")
class AdminProfileEntity(
    @Id
    val userId: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    var user: UserEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "admin_level", nullable = false, length = 20)
    var adminLevel: AdminLevel = AdminLevel.OPERATOR,

    // JSON 배열로 저장 (예: ["USER_BAN","PRODUCT_DEACTIVATE"])
    // 운영 환경(PostgreSQL) 전환 시 columnDefinition = "jsonb" 로 변경
    @Column(columnDefinition = "TEXT")
    var permissions: String = "[]",
)
