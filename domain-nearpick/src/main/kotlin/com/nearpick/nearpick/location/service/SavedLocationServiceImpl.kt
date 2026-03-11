package com.nearpick.nearpick.location.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.location.SavedLocationService
import com.nearpick.domain.location.dto.CreateSavedLocationRequest
import com.nearpick.domain.location.dto.SavedLocationResponse
import com.nearpick.domain.location.dto.UpdateSavedLocationRequest
import com.nearpick.nearpick.location.entity.SavedLocationEntity
import com.nearpick.nearpick.location.repository.SavedLocationRepository
import com.nearpick.nearpick.user.repository.ConsumerProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val MAX_SAVED_LOCATIONS = 5L

@Service
class SavedLocationServiceImpl(
    private val savedLocationRepository: SavedLocationRepository,
    private val consumerProfileRepository: ConsumerProfileRepository,
) : SavedLocationService {

    @Transactional(readOnly = true)
    override fun getLocations(userId: Long): List<SavedLocationResponse> =
        savedLocationRepository.findAllByConsumerUserId(userId).map { it.toResponse() }

    @Transactional
    override fun addLocation(userId: Long, request: CreateSavedLocationRequest): SavedLocationResponse {
        val count = savedLocationRepository.countByConsumerUserId(userId)
        if (count >= MAX_SAVED_LOCATIONS) throw BusinessException(ErrorCode.SAVED_LOCATION_LIMIT_EXCEEDED)

        val consumer = consumerProfileRepository.findById(userId)
            .orElseThrow { BusinessException(ErrorCode.CONSUMER_NOT_FOUND) }

        if (request.isDefault) {
            savedLocationRepository.clearAllDefault(userId)
        }

        val entity = SavedLocationEntity(
            consumer = consumer,
            label = request.label,
            lat = request.lat,
            lng = request.lng,
            isDefault = request.isDefault,
        )
        return savedLocationRepository.save(entity).toResponse()
    }

    @Transactional
    override fun updateLocation(userId: Long, locationId: Long, request: UpdateSavedLocationRequest): SavedLocationResponse {
        val entity = findOrThrow(locationId, userId)

        request.label?.let { entity.label = it }
        if (request.isDefault == true) {
            savedLocationRepository.clearDefaultExcept(userId, locationId)
            entity.isDefault = true
        }

        return savedLocationRepository.save(entity).toResponse()
    }

    @Transactional
    override fun deleteLocation(userId: Long, locationId: Long) {
        val entity = findOrThrow(locationId, userId)
        savedLocationRepository.delete(entity)
    }

    @Transactional
    override fun setDefault(userId: Long, locationId: Long): SavedLocationResponse {
        val entity = findOrThrow(locationId, userId)
        savedLocationRepository.clearDefaultExcept(userId, locationId)
        entity.isDefault = true
        return savedLocationRepository.save(entity).toResponse()
    }

    @Transactional(readOnly = true)
    override fun getLocation(userId: Long, locationId: Long): SavedLocationResponse =
        findOrThrow(locationId, userId).toResponse()

    private fun findOrThrow(locationId: Long, userId: Long): SavedLocationEntity =
        savedLocationRepository.findByIdAndConsumerUserId(locationId, userId)
            ?: throw BusinessException(ErrorCode.SAVED_LOCATION_NOT_FOUND)

    private fun SavedLocationEntity.toResponse() = SavedLocationResponse(
        id = id,
        label = label,
        lat = lat,
        lng = lng,
        isDefault = isDefault,
        createdAt = createdAt,
    )
}
