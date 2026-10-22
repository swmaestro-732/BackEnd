package com.example.backend.mobile.user.application.port.outbound

import com.example.backend.mobile.user.application.port.outbound.dto.ProfileSnapshot

/**
 * BFF 아웃바운드 포트 — 프로필 조회. 지금은 user 도메인 인바운드 포트에 위임하는 어댑터가 구현하지만,
 * MSA 분리 후엔 같은 계약을 HTTP 클라이언트 어댑터로 바꿔 끼우면 BFF 조합 코드는 그대로다.
 */
interface MyPageProfilePort {
    /** 내 프로필. 팔로우 플래그는 자기 자신 기준이라 false. */
    fun getMyProfile(userId: Long): ProfileSnapshot

    /** 타인 프로필(handle 기준). viewerId 기준 팔로우 플래그 포함. 비로그인이면 viewerId=null. */
    fun getProfileByHandle(
        handle: String,
        viewerId: Long?,
    ): ProfileSnapshot
}
