package com.nearpick.nearpick.location.service

import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.location.ConsumerLocationService
import com.nearpick.domain.location.dto.UpdateCurrentLocationRequest
import com.nearpick.nearpick.user.repository.ConsumerProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ConsumerLocationServiceImpl(
    private val consumerProfileRepository: ConsumerProfileRepository,
) : ConsumerLocationService {

    @Transactional
    override fun updateCurrentLocation(userId: Long, request: UpdateCurrentLocationRequest) {
        val updated = consumerProfileRepository.updateCurrentLocation(
            userId = userId,
            lat = request.lat,
            lng = request.lng,
            now = LocalDateTime.now(),
        )
        if (updated == 0) throw BusinessException(ErrorCode.CONSUMER_NOT_FOUND)
    }
}
