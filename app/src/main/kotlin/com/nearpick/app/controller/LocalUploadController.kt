package com.nearpick.app.controller

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * Local 전용 이미지 업로드/다운로드 컨트롤러.
 * S3 Presigned URL 업로드와 동일한 방식(PUT + binary body)으로 동작.
 *
 * - PUT /local-upload/{s3Key} : 파일 저장 (./uploads/{s3Key} 경로에 저장)
 * - GET /local-upload/{s3Key} : 파일 서빙
 */
@RestController
@Profile("local")
class LocalUploadController(
    @Value("\${product.image.local.upload-dir:./uploads}") private val uploadDir: String,
) {

    @PutMapping("/local-upload/**")
    fun uploadFile(
        request: HttpServletRequest,
        @RequestBody body: ByteArray,
    ): ResponseEntity<Void> {
        val s3Key = extractS3Key(request.requestURI)
        val targetPath = Path.of(uploadDir).resolve(s3Key)
        targetPath.parent.toFile().mkdirs()
        Files.write(targetPath, body)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/local-upload/**")
    fun downloadFile(request: HttpServletRequest): ResponseEntity<FileSystemResource> {
        val s3Key = extractS3Key(request.requestURI)
        val file = File("$uploadDir/$s3Key")
        if (!file.exists()) return ResponseEntity.notFound().build()

        val contentType = runCatching { Files.probeContentType(file.toPath()) }.getOrNull()
            ?: "application/octet-stream"

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .body(FileSystemResource(file))
    }

    private fun extractS3Key(uri: String): String =
        uri.removePrefix("/local-upload/")
}
