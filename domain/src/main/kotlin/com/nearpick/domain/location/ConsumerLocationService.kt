package com.nearpick.domain.location

import com.nearpick.domain.location.dto.UpdateCurrentLocationRequest

interface ConsumerLocationService {
    fun updateCurrentLocation(userId: Long, request: UpdateCurrentLocationRequest)
}
