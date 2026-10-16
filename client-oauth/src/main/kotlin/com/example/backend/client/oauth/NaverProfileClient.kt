package com.example.backend.client.oauth

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

/** 네이버 프로필 API(`GET /v1/nid/me`) 클라이언트. 토큰·프로필 원문은 로그에 남기지 않는다. */
@Component
class NaverProfileClient(
    @param:Qualifier("naverRestClient")
    private val naverRestClient: RestClient,
) {
    private val log = KotlinLogging.logger {}

    /** 2xx 가 아니면 상태 코드를, 통신·응답 해석 실패면 null 상태로 [NaverApiException] 을 던진다. */
    fun fetchProfile(accessToken: String): NaverProfileResponse? =
        try {
            naverRestClient
                .get()
                .uri("/v1/nid/me")
                .headers { it.setBearerAuth(accessToken) }
                .retrieve()
                .onStatus({ !it.is2xxSuccessful }) { _, response ->
                    throw NaverApiException(response.statusCode.value())
                }.body(NaverProfileResponse::class.java)
        } catch (exception: RestClientException) {
            // 예외 메시지/스택에는 응답 본문이 포함될 수 있으므로 예외 종류만 기록한다.
            log.warn { "네이버 인증 API 통신 또는 응답 해석 실패: type=${exception.javaClass.simpleName}" }
            throw NaverApiException(null)
        }
}

class NaverApiException(
    val status: Int?,
) : RuntimeException("네이버 API 호출 실패: status=$status")

@JsonIgnoreProperties(ignoreUnknown = true)
data class NaverProfileResponse(
    val resultcode: String? = null,
    val response: NaverProfile? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NaverProfile(
    val id: String? = null,
)
