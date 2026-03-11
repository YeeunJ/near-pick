package com.nearpick.domain.location

import com.nearpick.domain.location.dto.CreateSavedLocationRequest
import com.nearpick.domain.location.dto.SavedLocationResponse
import com.nearpick.domain.location.dto.UpdateSavedLocationRequest

interface SavedLocationService {
    fun getLocations(userId: Long): List<SavedLocationResponse>
    fun addLocation(userId: Long, request: CreateSavedLocationRequest): SavedLocationResponse
    fun updateLocation(userId: Long, locationId: Long, request: UpdateSavedLocationRequest): SavedLocationResponse
    fun deleteLocation(userId: Long, locationId: Long)
    fun setDefault(userId: Long, locationId: Long): SavedLocationResponse
    fun getLocation(userId: Long, locationId: Long): SavedLocationResponse
}
