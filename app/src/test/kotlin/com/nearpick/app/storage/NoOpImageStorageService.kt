package com.nearpick.app.storage

import com.nearpick.domain.product.ImageStorageService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("test")
class NoOpImageStorageService : ImageStorageService {
    override fun generatePresignedPutUrl(s3Key: String, contentType: String): String = "http://test/$s3Key"
    override fun buildPublicUrl(s3Key: String): String = "http://test/$s3Key"
    override fun deleteObject(s3Key: String) {}
}
