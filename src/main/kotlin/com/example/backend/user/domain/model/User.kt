package com.example.backend.user.domain.model

/**
 * User 애그리거트 루트.
 *
 * 프레임워크(Spring·Exposed)에 의존하지 않는 순수 도메인 모델이다.
 * 생성은 [create] 팩토리를 통해서만 하며, 이때 도메인 불변식을 검증한다.
 * [id] 가 null 이면 아직 영속화되지 않은 상태다.
 */
@ConsistentCopyVisibility // copy() 도 private 으로 — 팩토리 우회 차단
data class User private constructor(
    val id: Long?,
    val nickname: String,
    val profileImageUrl: String?,
    val socialProvider: SocialProvider?,
    val socialId: String?,
) {
    companion object {
        const val MAX_NICKNAME_LENGTH = 20

        /** 신규 생성 — 도메인 불변식을 검증한다. */
        fun create(
            nickname: String,
            profileImageUrl: String? = null,
        ): User {
            require(nickname.isNotBlank()) { "닉네임은 비어 있을 수 없습니다." }
            require(nickname.length <= MAX_NICKNAME_LENGTH) { "닉네임은 최대 ${MAX_NICKNAME_LENGTH}자입니다." }
            return User(
                id = null,
                nickname = nickname,
                profileImageUrl = profileImageUrl,
                socialProvider = null,
                socialId = null,
            )
        }

        /** 소셜 회원 신규 생성. 소셜 제공자와 식별자는 항상 함께 존재한다. */
        fun createWithSocial(
            nickname: String,
            profileImageUrl: String?,
            socialProvider: SocialProvider,
            socialId: String,
        ): User {
            require(nickname.isNotBlank()) { "닉네임은 비어 있을 수 없습니다." }
            require(nickname.length <= MAX_NICKNAME_LENGTH) { "닉네임은 최대 ${MAX_NICKNAME_LENGTH}자입니다." }
            require(socialId.isNotBlank()) { "소셜 식별자는 비어 있을 수 없습니다." }
            return User(
                id = null,
                nickname = nickname,
                profileImageUrl = profileImageUrl,
                socialProvider = socialProvider,
                socialId = socialId,
            )
        }

        /** 영속 저장소에서 복원(이미 검증된 데이터). */
        fun reconstitute(
            id: Long,
            nickname: String,
            profileImageUrl: String? = null,
            socialProvider: SocialProvider? = null,
            socialId: String? = null,
        ): User {
            // 소셜 provider 와 socialId 는 한 쌍 — 함께 있거나 함께 없어야 한다(둘 중 하나만 있으면 size == 1).
            require(listOfNotNull(socialProvider, socialId).size != 1) {
                "소셜 provider 와 socialId 는 함께 있거나 함께 없어야 합니다."
            }
            return User(
                id = id,
                nickname = nickname,
                profileImageUrl = profileImageUrl,
                socialProvider = socialProvider,
                socialId = socialId,
            )
        }
    }
}
