package com.nearpick.app.storage

import com.nearpick.domain.product.ImageStorageService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.io.File

@Service
@Profile("local")
class LocalImageStorageService(
    @Value("\${server.port:8080}") private val serverPort: Int,
    @Value("\${product.image.local.upload-dir:./uploads}") private val uploadDir: String,
) : ImageStorageService {

    private val baseUrl get() = "http://localhost:$serverPort"

    override fun generatePresignedPutUrl(s3Key: String, contentType: String): String =
        "$baseUrl/local-upload/$s3Key"

    override fun buildPublicUrl(s3Key: String): String =
        "$baseUrl/local-upload/$s3Key"

    override fun deleteObject(s3Key: String) {
        val file = File("$uploadDir/$s3Key")
        if (file.exists()) file.delete()
    }
}
