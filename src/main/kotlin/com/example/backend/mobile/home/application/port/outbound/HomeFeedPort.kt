package com.example.backend.mobile.home.application.port.outbound

import com.example.backend.mobile.home.application.port.outbound.dto.HomeFeedCoursePage
import com.example.backend.mobile.home.application.port.outbound.dto.HomeInterest

/**
 * BFF 아웃바운드 포트 — 추천 코스 조회. 지금은 course 도메인 인바운드 포트에 위임하는 어댑터가 구현하지만,
 * MSA 분리 후엔 course 서비스 HTTP 클라이언트로 바꿔 끼운다.
 */
interface HomeFeedPort {
    /**
     * [interest] 로 개인화한 추천 코스를 불투명 [cursor] 이후부터 [size] 개 조회한다. [category]가 있으면 그 카테고리만.
     * 커서의 해석·다음 커서 생성은 course 도메인 몫이다.
     */
    fun listRecommended(
        interest: HomeInterest,
        cursor: String?,
        size: Int,
        category: String? = null,
    ): HomeFeedCoursePage
}
