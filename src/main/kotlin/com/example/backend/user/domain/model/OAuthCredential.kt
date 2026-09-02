package com.example.backend.user.domain.model

/**
 * 소셜 인증 자격증명 — (제공자, 소셜 식별자) 한 쌍. [Identity] 애그리거트가 소유하는 값 객체다.
 * 한 Identity 는 서로 다른 제공자의 자격증명을 여러 개 가질 수 있다(계정 링크).
 */
data class OAuthCredential private constructor(
    val provider: SocialProvider,
    val socialId: String,
) {
    companion object {
        fun create(
            provider: SocialProvider,
            socialId: String,
        ): OAuthCredential {
            require(socialId.isNotBlank()) { "소셜 식별자는 비어 있을 수 없습니다." }
            return OAuthCredential(provider, socialId)
        }
    }
}
