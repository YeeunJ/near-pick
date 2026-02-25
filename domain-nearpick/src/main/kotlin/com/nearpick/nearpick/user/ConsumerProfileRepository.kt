package com.nearpick.nearpick.user

import org.springframework.data.jpa.repository.JpaRepository

interface ConsumerProfileRepository : JpaRepository<ConsumerProfileEntity, Long>
