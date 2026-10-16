package com.example.backend.user.adapter.outbound.social

import com.example.backend.client.oauth.NaverApiException
import com.example.backend.client.oauth.NaverProfileClient
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.user.application.port.outbound.SocialIdentity
import com.example.backend.user.domain.model.SocialProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

/** 네이버 액세스 토큰으로 프로필 API를 호출해 검증한 id만 사용한다. 토큰·프로필 원문은 저장하거나 로그에 남기지 않는다. */
@Component
class NaverSocialVerifier(
    private val naverProfileClient: NaverProfileClient,
) : SocialTokenVerifier {
    private val log = KotlinLogging.logger {}

    override val provider: SocialProvider = SocialProvider.NAVER

    override fun verify(token: String): SocialIdentity {
        if (token.isBlank() || token.any { it.isISOControl() }) {
            throw BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED)
        }
        val profile =
            try {
                naverProfileClient.fetchProfile(token)
            } catch (exception: NaverApiException) {
                if (exception.status in AUTH_FAILURE_STATUSES) {
                    throw BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED)
                }
                if (exception.status != null) {
                    log.warn { "네이버 인증 API 응답 실패: status=${exception.status}" }
                }
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
        // 403(호출 권한 없음)은 사용자 토큰이 아니라 앱 API 권한 설정 문제라 인증 실패로 분류하지 않는다.
        val AUTH_FAILURE_STATUSES = setOf(401)
        val AUTH_FAILURE_CODES = setOf("024", "028")
        const val MAX_SOCIAL_ID_LENGTH = 255
    }
}
