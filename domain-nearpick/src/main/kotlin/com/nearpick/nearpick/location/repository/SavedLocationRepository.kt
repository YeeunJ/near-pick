package com.nearpick.nearpick.location.repository

import com.nearpick.nearpick.location.entity.SavedLocationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface SavedLocationRepository : JpaRepository<SavedLocationEntity, Long> {

    fun findAllByConsumerUserId(userId: Long): List<SavedLocationEntity>

    fun countByConsumerUserId(userId: Long): Long

    fun findByIdAndConsumerUserId(id: Long, userId: Long): SavedLocationEntity?

    @Modifying
    @Query("UPDATE SavedLocationEntity s SET s.isDefault = false WHERE s.consumer.userId = :userId AND s.id != :exceptId")
    fun clearDefaultExcept(userId: Long, exceptId: Long)

    @Modifying
    @Query("UPDATE SavedLocationEntity s SET s.isDefault = false WHERE s.consumer.userId = :userId")
    fun clearAllDefault(userId: Long)
}
