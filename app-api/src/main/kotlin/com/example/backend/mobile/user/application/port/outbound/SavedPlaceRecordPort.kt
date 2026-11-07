package com.example.backend.mobile.user.application.port.outbound

import com.example.backend.mobile.user.application.port.outbound.dto.SavedPlaceRecordPage

/**
 * BFF 아웃바운드 포트 — 저장함 장소 탭의 저장 레코드 한 페이지 + 배지 카운트 조회.
 * 지금은 user 도메인 인바운드 포트에 위임하는 어댑터가 구현하지만,
 * MSA 분리 후엔 같은 계약을 HTTP 클라이언트 어댑터로 바꿔 끼우면 BFF 조합 코드는 그대로다.
 */
interface SavedPlaceRecordPort {
    /**
     * 저장 레코드 한 페이지(최신 저장순)와 필터 무관 전체 배지 카운트.
     * category 는 저장 카테고리 이름(예: CAFE, null 이면 전체) — 아는 이름이 아니면 400 이 전파된다.
     */
    fun findPage(
        userId: Long,
        visited: Boolean,
        category: String?,
        cursor: String?,
        size: Int,
    ): SavedPlaceRecordPage
}
