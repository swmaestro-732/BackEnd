package com.example.backend.mobile.home.application.port.inbound

import com.example.backend.mobile.home.application.port.inbound.dto.HomeFeedResult

/**
 * 코스 피드 화면 조합 (BFF). 공개 코스 후보(course) + 코스별 저장수(user)를 조합해
 * 저장수 내림차순·최신순으로 랭킹한 피드를 만든다. 비로그인 포함 누구나 조회 가능한 공개 피드다.
 */
interface HomeFeedUseCase {
    /** 저장수 내림차순·최신순으로 랭킹한 공개 코스 피드를 [cursor] 이후부터 [size] 개 내려준다. */
    fun getFeed(
        cursor: String?,
        size: Int,
    ): HomeFeedResult
}
