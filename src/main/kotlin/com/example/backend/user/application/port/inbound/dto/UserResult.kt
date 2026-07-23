package com.example.backend.user.application.port.inbound.dto

import com.example.backend.user.domain.model.User

/** 유스케이스 출력 — 도메인 모델을 밖으로 노출하지 않기 위한 애플리케이션 경계 타입. */
data class UserResult(
    val id: Long,
    val nickname: String,
) {
    companion object {
        fun from(user: User): UserResult =
            UserResult(
                id = checkNotNull(user.id) { "영속화된 User 는 id 를 가진다." },
                nickname = user.nickname,
            )
    }
}
