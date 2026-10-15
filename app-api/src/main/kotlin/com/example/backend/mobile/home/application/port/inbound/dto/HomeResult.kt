package com.example.backend.mobile.home.application.port.inbound.dto

import com.example.backend.mobile.home.application.port.outbound.dto.HomeFeedCourse
import com.example.backend.mobile.home.application.port.outbound.dto.HomeNearbyPlace
import com.example.backend.mobile.home.application.port.outbound.dto.HomeProfile

/**
 * 홈 화면 조합 결과 (BFF). profile 은 비로그인이면 null, 추천 코스는 피드 랭킹 상위, 근처 저장 장소는 거리순이다.
 * 재료는 도메인 타입이 아닌 BFF 격리 DTO 다.
 */
data class HomeResult(
    val profile: HomeProfile?,
    val recommendedCourses: List<HomeFeedCourse>,
    val nearbySavedPlaces: List<NearbySavedPlace>,
) {
    /** 근처 저장 장소 한 건 — [walkingMinutes] 는 직선 거리를 도보 속도(분당 67m)로 나눠 올림한 근사값이다. */
    data class NearbySavedPlace(
        val place: HomeNearbyPlace,
        val walkingMinutes: Int,
    )
}
