package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.CourseRecommendPage
import com.example.backend.course.application.port.inbound.dto.CourseRecommendQuery

/**
 * 인바운드 포트 — 추천 코스(홈·전체보기). 발행 PUBLIC 코스를 최신성 + 관심 테마·지역 일치 + 저장수 점수로 랭킹한다.
 * 검색 엔진이 없거나 실패하면 공개 피드(저장수·최신순, [CourseQueryUseCase.listPublic] 과 같은 정렬)로 폴백한다.
 * 커서는 불투명 문자열이며 엔진/폴백 출처를 함께 담아 한 페이지 흐름 안에서 출처가 바뀌지 않는다.
 */
interface CourseRecommendUseCase {
    fun recommend(query: CourseRecommendQuery): CourseRecommendPage
}
