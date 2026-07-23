package com.example.backend.user.domain.model

/** 사용자 상태(users.status SMALLINT). V1 코드 순서 유지. */
enum class UserStatus(
    val code: Short,
) {
    ACTIVE(0),
    SUSPENDED(1),
    PENDING(2),
    WITHDRAWN(3),
    DELETED(4),
    ;

    companion object {
        fun fromCode(code: Short): UserStatus =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("알 수 없는 사용자 상태 코드: $code")
    }
}
