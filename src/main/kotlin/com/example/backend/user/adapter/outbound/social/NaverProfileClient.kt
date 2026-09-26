package com.example.backend.user.adapter.outbound.social

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.user.application.port.outbound.SocialIdentity
import com.example.backend.user.domain.model.SocialProvider
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

/** 네이버 프로필 API가 검증한 id만 사용한다. 토큰·프로필 원문은 저장하거나 로그에 남기지 않는다. */
@Component
class NaverProfileClient(
    @param:Qualifier("naverRestClient")
    private val naverRestClient: RestClient,
) {
    private val log = KotlinLogging.logger {}

    fun verify(token: String): SocialIdentity {
        if (token.isBlank() || token.any { it.isISOControl() }) {
            throw BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED)
        }
        val profile =
            try {
                naverRestClient
                    .get()
                    .uri("/v1/nid/me")
                    .headers { it.setBearerAuth(token) }
                    .retrieve()
                    .onStatus({ !it.is2xxSuccessful }) { _, response ->
                        if (response.statusCode.value() in AUTH_FAILURE_STATUSES) {
                            throw BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED)
                        }
                        log.warn { "네이버 인증 API 응답 실패: status=${response.statusCode.value()}" }
                        throw BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_UNAVAILABLE)
                    }.body(NaverProfileResponse::class.java)
            } catch (exception: RestClientException) {
                // 예외 메시지/스택에는 응답 본문이 포함될 수 있으므로 예외 종류만 기록한다.
                log.warn { "네이버 인증 API 통신 또는 응답 해석 실패: type=${exception.javaClass.simpleName}" }
                throw BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_UNAVAILABLE)
            }

        if (profile?.resultcode in AUTH_FAILURE_CODES) {
            throw BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED)
        }
        val socialId = profile?.response?.id
        if (profile?.resultcode != "00" || socialId.isNullOrBlank() || socialId.length > MAX_SOCIAL_ID_LENGTH) {
            log.warn { "네이버 인증 API 응답 형식 또는 결과 코드 오류" }
            throw BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_UNAVAILABLE)
        }
        return SocialIdentity(provider = SocialProvider.NAVER, socialId = socialId)
    }

    private companion object {
        val AUTH_FAILURE_STATUSES = setOf(401, 403)
        val AUTH_FAILURE_CODES = setOf("024", "028", "403")
        const val MAX_SOCIAL_ID_LENGTH = 255
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class NaverProfileResponse(
    val resultcode: String? = null,
    val response: NaverProfile? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class NaverProfile(
    val id: String? = null,
)
