package com.nearpick.nearpick.product.storage

import com.nearpick.domain.product.ImageStorageService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.time.Duration

@Service
@Profile("!local & !test")
class S3ImageStorageService(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    @Value("\${aws.s3.bucket}") private val bucket: String,
    @Value("\${aws.s3.region:ap-northeast-2}") private val region: String,
) : ImageStorageService {

    override fun generatePresignedPutUrl(s3Key: String, contentType: String): String {
        val putObjectRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(s3Key)
            .contentType(contentType)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(5))
            .putObjectRequest(putObjectRequest)
            .build()

        return s3Presigner.presignPutObject(presignRequest).url().toString()
    }

    override fun buildPublicUrl(s3Key: String): String =
        "https://$bucket.s3.$region.amazonaws.com/$s3Key"

    override fun deleteObject(s3Key: String) {
        s3Client.deleteObject(
            DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build()
        )
    }
}
