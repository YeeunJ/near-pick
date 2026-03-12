package com.nearpick.nearpick.location.client

import com.fasterxml.jackson.annotation.JsonProperty
import com.nearpick.common.exception.BusinessException
import com.nearpick.common.exception.ErrorCode
import com.nearpick.domain.location.LocationClient
import com.nearpick.domain.location.dto.LocationSearchResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.math.BigDecimal

@Component
@Profile("!local & !test")
class KakaoLocationClient(
    @Value("\${kakao.rest-api-key:}") private val apiKey: String,
) : LocationClient {
    private val restClient: RestClient = RestClient.builder()
        .baseUrl("https://dapi.kakao.com")
        .defaultHeader("Authorization", "KakaoAK $apiKey")
        .build()

    override fun searchAddress(query: String): List<LocationSearchResult> {
        if (apiKey.isBlank()) throw BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE)

        return try {
            val response = restClient.get()
                .uri("/v2/local/search/address.json?query={query}&size=5", query)
                .retrieve()
                .onStatus({ it != HttpStatus.OK }) { _, _ ->
                    throw BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE)
                }
                .body(KakaoAddressResponse::class.java)

            response?.documents?.map { doc ->
                LocationSearchResult(
                    address = doc.addressName,
                    lat = BigDecimal(doc.y),
                    lng = BigDecimal(doc.x),
                )
            } ?: emptyList()
        } catch (e: BusinessException) {
            throw e
        } catch (e: RestClientException) {
            throw BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE)
        }
    }

    data class KakaoAddressResponse(
        val documents: List<KakaoDocument> = emptyList(),
    )

    data class KakaoDocument(
        @JsonProperty("address_name") val addressName: String = "",
        val x: String = "",  // lng
        val y: String = "",  // lat
    )
}
