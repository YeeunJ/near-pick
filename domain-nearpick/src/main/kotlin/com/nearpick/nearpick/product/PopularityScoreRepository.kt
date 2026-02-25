package com.nearpick.nearpick.product

import org.springframework.data.jpa.repository.JpaRepository

interface PopularityScoreRepository : JpaRepository<PopularityScoreEntity, Long>
