package com.nearpick.domain.product

interface ImageStorageService {
    fun generatePresignedPutUrl(s3Key: String, contentType: String): String
    fun buildPublicUrl(s3Key: String): String
    fun deleteObject(s3Key: String)
}
