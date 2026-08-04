package com.example.backend.mobile.user.application.port.inbound

import com.example.backend.mobile.user.application.port.inbound.dto.MyPageResult

/**
 * 마이 화면 조합 (BFF). 프로필(user) + 그 사용자의 공개 코스(course)를 조합한다.
 *
 * - [getMyPage] 로그인한 나 — 내 프로필 + 내 발행 코스 전체(viewerId==userId 라 공개범위 무관).
 * - [getUserPage] 타인(handle) — 대상 프로필 + 조회자(viewerId) 기준 공개 코스만.
 */
interface MyPageUseCase {
    fun getMyPage(userId: Long): MyPageResult

    fun getUserPage(
        handle: String,
        viewerId: Long?,
    ): MyPageResult
}
