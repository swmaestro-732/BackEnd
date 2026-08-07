package com.example.backend.user.adapter.inbound.web.request

import com.example.backend.user.domain.model.User
import jakarta.validation.constraints.Size

/**
 * 프로필 수정 요청 DTO. 모든 필드 선택(부분 수정, null=변경 안 함).
 * 단, 값을 보낼 경우 비어 있으면 안 된다(`@Size(min=1)` — null 은 통과, 빈 문자열은 거부).
 * [areaCodes] 는 관심 지역 전체 치환 — 빈 배열은 전체 삭제, null 은 유지. 항목은 지역 검색의 prefix(읍면동 10자리·시군구 5자리).
 */
data class UpdateProfileRequest(
    @field:Size(min = 1, max = User.MAX_NICKNAME_LENGTH)
    val nickname: String? = null,
    @field:Size(min = 1)
    val handle: String? = null,
    val profileImageUrl: String? = null,
    val areaCodes: List<String>? = null,
)
