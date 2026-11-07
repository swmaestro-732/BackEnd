package com.example.backend.naver

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * 네이버 회원 프로필 API(`GET /v1/nid/me`) 호출. 액세스 토큰은 Bearer 헤더로만 보내고 저장하거나 로그에 남기지 않는다.
 * 통신, 응답 해석 실패는 RestClientException 으로 던진다. 결과 해석은 호출하는 쪽이 한다.
 */
@Component
class NaverProfileClient(
    @param:Qualifier("naverRestClient")
    private val naverRestClient: RestClient,
) {
    /** 2xx 가 아니면 본문을 읽지 않고 [onErrorStatus] 를 상태 코드로 호출한다(호출하는 쪽이 예외를 던진다). */
    fun fetchProfile(
        accessToken: String,
        onErrorStatus: (Int) -> Nothing,
    ): NaverProfileResponse? =
        naverRestClient
            .get()
            .uri("/v1/nid/me")
            .headers { it.setBearerAuth(accessToken) }
            .retrieve()
            .onStatus({ !it.is2xxSuccessful }) { _, response -> onErrorStatus(response.statusCode.value()) }
            .body(NaverProfileResponse::class.java)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class NaverProfileResponse(
    val resultcode: String? = null,
    val response: NaverProfile? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NaverProfile(
    val id: String? = null,
)
