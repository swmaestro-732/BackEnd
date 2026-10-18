package com.example.backend.mobile.home.application.port.inbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.mobile.home.application.port.inbound.dto.HomeFeedResult
import com.example.backend.mobile.home.application.port.inbound.dto.HomeResult

/**
 * 홈 화면 조합 (BFF). 헤더 프로필(user) + 추천 코스(course) + 근처 저장 장소(user, place)를 한 화면으로 묶고,
 * 추천 코스 "전체보기"용 공개 코스 피드(저장수 내림차순, 최신순)를 커서 페이지로 내려준다. 비로그인도 조회 가능하다.
 */
interface HomeFeedUseCase {
    /**
     * 홈 한 화면. 비로그인([viewerId] null)이면 프로필은 null, 좌표([userLocation])가 없거나 비로그인이면
     * 근처 저장 장소는 빈 목록이다.
     */
    fun getHome(
        viewerId: Long?,
        userLocation: Coordinate?,
    ): HomeResult

    /** 저장수 내림차순·최신순으로 랭킹한 공개 코스 피드를 [cursor] 이후부터 [size] 개 내려준다. */
    fun getFeed(
        cursor: String?,
        size: Int,
    ): HomeFeedResult
}
