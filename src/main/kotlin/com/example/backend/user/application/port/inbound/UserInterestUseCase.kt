package com.example.backend.user.application.port.inbound

import com.example.backend.user.application.port.inbound.dto.UserInterestResult

/** 인바운드 포트 — 사용자의 관심 테마·지역(추천 입력). 다른 도메인·BFF 가 개인화에 쓴다. */
interface UserInterestUseCase {
    /** 관심 테마(코스 카테고리 이름)와 관심 지역 코드(10자리, 시군구는 뒤 5자리 00000). 사용자가 없거나 미등록이면 빈 목록. */
    fun findInterests(userId: Long): UserInterestResult
}
