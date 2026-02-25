package com.nearpick.nearpick.user

import org.springframework.data.jpa.repository.JpaRepository

interface AdminProfileRepository : JpaRepository<AdminProfileEntity, Long>
