package com.example.backend.course.application.port.inbound

/**
 * 인바운드 포트 — 코스 생성 시 추천 태그 조회.
 * 코스에 담긴 장소들([placeIds])을 기반으로 태그를 추천하고,
 * 장소가 없으면 인기 태그를 내려준다(노션 명세 · Course · 추천 태그).
 */
interface RecommendedTagUseCase {
    fun recommend(
        placeIds: List<Long>,
        limit: Int,
    ): List<String>
}
