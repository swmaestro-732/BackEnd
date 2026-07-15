package com.example.backend.user.adapter.inbound.web.response

import com.example.backend.user.application.dto.UserResult

/** 웹 응답 DTO. 유스케이스 결과([UserResult])를 직렬화 형태로 변환한다. */
data class UserResponse(
    val id: Long,
    val nickname: String,
) {
    companion object {
        fun from(result: UserResult): UserResponse = UserResponse(id = result.id, nickname = result.nickname)
    }
}
