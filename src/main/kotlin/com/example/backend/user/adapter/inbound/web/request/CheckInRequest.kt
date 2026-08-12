package com.example.backend.user.adapter.inbound.web.request

import jakarta.validation.constraints.Positive

/**
 * 장소 방문 체크인 요청 — 웹 어댑터 DTO.
 * - placeId: 체크인할 장소 id. 코스에 담긴 장소여야 한다(그 외 400).
 */
data class CheckInRequest(
    @field:Positive val placeId: Long,
)
