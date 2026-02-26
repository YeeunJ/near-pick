package com.nearpick.nearpick.user.repository

import org.springframework.data.jpa.repository.JpaRepository
import com.nearpick.nearpick.user.entity.AdminProfileEntity

interface AdminProfileRepository : JpaRepository<AdminProfileEntity, Long>
