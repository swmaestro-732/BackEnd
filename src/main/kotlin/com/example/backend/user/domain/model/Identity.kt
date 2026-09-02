package com.example.backend.user.domain.model

/**
 * Identity 애그리거트 루트 — 인증 주체(사람 1명).
 *
 * OAuth 자격증명([OAuthCredential]) n개를 소유하고, 하나의 Identity 아래 여러 [User] 프로필(부캐)이 매달린다.
 * 프레임워크에 의존하지 않는 순수 도메인 모델이며, [id] 가 null 이면 아직 영속화되지 않은 상태다.
 */
data class Identity private constructor(
    val id: Long?,
    val credentials: List<OAuthCredential>,
) {
    companion object {
        /** 소셜 자격증명 하나로 새 Identity 를 만든다(최초 가입 시점). */
        fun create(credential: OAuthCredential): Identity =
            Identity(
                id = null,
                credentials = listOf(credential),
            )
    }
}
