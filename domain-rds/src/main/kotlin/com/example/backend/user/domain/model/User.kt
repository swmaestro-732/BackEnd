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
    val handle: String?,
    val profileImageUrl: String?,
    val bio: String?,
    val socialProvider: SocialProvider?,
    val socialId: String?,
    val status: UserStatus,
) {
    companion object {
        const val MAX_NICKNAME_LENGTH = 20
        const val MAX_HANDLE_LENGTH = 30

        /** 신규 생성 — 도메인 불변식을 검증한다. */
        fun create(
            nickname: String,
            profileImageUrl: String? = null,
            bio: String? = null,
        ): User {
            require(nickname.isNotBlank()) { "닉네임은 비어 있을 수 없습니다." }
            require(nickname.length <= MAX_NICKNAME_LENGTH) { "닉네임은 최대 ${MAX_NICKNAME_LENGTH}자입니다." }
            return User(
                id = null,
                nickname = nickname,
                handle = null,
                profileImageUrl = profileImageUrl,
                bio = bio,
                socialProvider = null,
                socialId = null,
                status = UserStatus.ACTIVE,
            )
        }

        /** 소셜 회원 신규 생성. 소셜 제공자와 식별자는 항상 함께 존재한다. */
        fun createWithSocial(
            nickname: String,
            profileImageUrl: String?,
            socialProvider: SocialProvider,
            socialId: String,
            handle: String? = null,
            bio: String? = null,
        ): User {
            require(nickname.isNotBlank()) { "닉네임은 비어 있을 수 없습니다." }
            require(nickname.length <= MAX_NICKNAME_LENGTH) { "닉네임은 최대 ${MAX_NICKNAME_LENGTH}자입니다." }
            if (handle != null) {
                require(handle.isNotBlank()) { "핸들은 비어 있을 수 없습니다." }
                require(handle.length <= MAX_HANDLE_LENGTH) { "핸들은 최대 ${MAX_HANDLE_LENGTH}자입니다." }
            }
            require(socialId.isNotBlank()) { "소셜 식별자는 비어 있을 수 없습니다." }
            return User(
                id = null,
                nickname = nickname,
                handle = handle,
                profileImageUrl = profileImageUrl,
                bio = bio,
                socialProvider = socialProvider,
                socialId = socialId,
                status = UserStatus.ACTIVE,
            )
        }

        /** 영속 저장소에서 복원(이미 검증된 데이터). */
        fun reconstitute(
            id: Long,
            nickname: String,
            profileImageUrl: String? = null,
            socialProvider: SocialProvider? = null,
            socialId: String? = null,
            status: UserStatus = UserStatus.ACTIVE,
            handle: String? = null,
            bio: String? = null,
        ): User {
            // 소셜 provider 와 socialId 는 한 쌍 — 함께 있거나 함께 없어야 한다(둘 중 하나만 있으면 size == 1).
            require(listOfNotNull(socialProvider, socialId).size != 1) {
                "소셜 provider 와 socialId 는 함께 있거나 함께 없어야 합니다."
            }
            return User(
                id = id,
                nickname = nickname,
                handle = handle,
                profileImageUrl = profileImageUrl,
                bio = bio,
                socialProvider = socialProvider,
                socialId = socialId,
                status = status,
            )
        }
    }

    /** 프로필 부분 수정 — null 인 필드는 그대로 둔다. 갱신 후 불변식을 재검증한다. */
    fun updateProfile(
        nickname: String? = null,
        handle: String? = null,
        profileImageUrl: String? = null,
        bio: String? = null,
    ): User {
        val newNickname = nickname ?: this.nickname
        val newHandle = handle ?: this.handle
        require(newNickname.isNotBlank()) { "닉네임은 비어 있을 수 없습니다." }
        require(newNickname.length <= MAX_NICKNAME_LENGTH) { "닉네임은 최대 ${MAX_NICKNAME_LENGTH}자입니다." }
        if (newHandle != null) {
            require(newHandle.isNotBlank()) { "핸들은 비어 있을 수 없습니다." }
            require(newHandle.length <= MAX_HANDLE_LENGTH) { "핸들은 최대 ${MAX_HANDLE_LENGTH}자입니다." }
        }
        return copy(
            nickname = newNickname,
            handle = newHandle,
            profileImageUrl = profileImageUrl ?: this.profileImageUrl,
            bio = bio ?: this.bio,
        )
    }

    /** 회원 탈퇴 — 활성 상태에서만 가능한 도메인 전이. */
    fun withdraw(): User {
        require(status == UserStatus.ACTIVE) { "활성 상태의 사용자만 탈퇴할 수 있습니다." }
        return copy(status = UserStatus.WITHDRAWN)
    }

    /** 재활성화 — 탈퇴한 사용자만 새 온보딩 프로필로 다시 활성화하는 도메인 전이. */
    fun reactivate(
        nickname: String,
        handle: String,
        profileImageUrl: String?,
    ): User {
        require(status == UserStatus.WITHDRAWN) { "탈퇴한 사용자만 재활성화할 수 있습니다." }
        require(nickname.isNotBlank()) { "닉네임은 비어 있을 수 없습니다." }
        require(handle.isNotBlank()) { "핸들은 비어 있을 수 없습니다." }
        require(nickname.length <= MAX_NICKNAME_LENGTH) { "닉네임은 최대 ${MAX_NICKNAME_LENGTH}자입니다." }
        require(handle.length <= MAX_HANDLE_LENGTH) { "핸들은 최대 ${MAX_HANDLE_LENGTH}자입니다." }
        return copy(
            status = UserStatus.ACTIVE,
            nickname = nickname,
            handle = handle,
            profileImageUrl = profileImageUrl,
        )
    }
}
