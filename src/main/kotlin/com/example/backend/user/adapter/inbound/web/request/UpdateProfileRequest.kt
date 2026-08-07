package com.example.backend.user.adapter.inbound.web.request

import com.example.backend.user.domain.model.User
import jakarta.validation.constraints.Size

/**
 * 프로필 수정 요청 DTO. 모든 필드 선택(부분 수정, null=변경 안 함).
 * 단, 값을 보낼 경우 비어 있으면 안 된다(`@Size(min=1)` — null 은 통과, 빈 문자열은 거부).
 * 기존 유스케이스 스타일을 유지해 필드를 그대로 넘긴다.
 */
data class UpdateProfileRequest(
    @field:Size(min = 1, max = User.MAX_NICKNAME_LENGTH)
    val nickname: String? = null,
    @field:Size(min = 1)
    val handle: String? = null,
    val profileImageUrl: String? = null,
    val bio: String? = null,
)
