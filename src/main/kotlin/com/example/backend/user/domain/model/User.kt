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
) {
    companion object {
        const val MAX_NICKNAME_LENGTH = 20

        /** 신규 생성 — 도메인 불변식을 검증한다. */
        fun create(nickname: String): User {
            require(nickname.isNotBlank()) { "닉네임은 비어 있을 수 없습니다." }
            require(nickname.length <= MAX_NICKNAME_LENGTH) { "닉네임은 최대 ${MAX_NICKNAME_LENGTH}자입니다." }
            return User(id = null, nickname = nickname)
        }

        /** 영속 저장소에서 복원(이미 검증된 데이터). */
        fun reconstitute(
            id: Long,
            nickname: String,
        ): User = User(id = id, nickname = nickname)
    }
}
