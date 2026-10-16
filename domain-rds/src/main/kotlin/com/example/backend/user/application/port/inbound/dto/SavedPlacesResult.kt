package com.example.backend.user.application.port.inbound.dto

import java.time.Instant

/**
 * 저장 장소 조회 명령 — 인바운드 포트([com.example.backend.user.application.port.inbound.SavedPlaceUseCase]) 입력.
 *
 * - userId: 조회 주체(JWT subject).
 * - visited: 방문 여부 필터(미방문/방문 탭). 생략 시 미방문(false) 기준 — 모킹 계약과 동일.
 * - category: 저장 카테고리 칩 필터([com.example.backend.user.domain.model.SavedPlaceCategory] 이름, 예: CAFE).
 *   null 이면 전체이고, 아는 이름이 아니면 400(서비스가 검증).
 * - cursor: 직전 응답의 nextCursor(첫 페이지는 null). 저장 레코드 id 기반 불투명 커서.
 * - size: 페이지 크기(1~50). 웹 어댑터에서 검증한다.
 *
 * 카테고리를 도메인 enum 이 아니라 이름 문자열로 주고받는다 — 이 DTO 는 포트 계약이라 BFF·타 도메인도 쓰는데,
 * 그들은 user 도메인 모델을 참조할 수 없기 때문이다(헥사고날 경계 규칙). place 도메인의 `PlaceSummary.category` 와 같은 방식.
 */
data class SavedPlacesCommand(
    val userId: Long,
    val visited: Boolean = false,
    val category: String? = null,
    val cursor: String?,
    val size: Int,
)

/**
 * 저장 장소 조회 결과 — 저장 레코드(ID 위주) + 커서 페이지 메타 + 저장함 칩/탭 배지 카운트.
 * 장소 상세(이름·평점·거리 등) 화면용 조합은 BFF(`GET /service/v1/my/saved-places`)가 담당한다.
 *
 * 카운트([totalCount]/[unvisitedCount]/[visitedCount]/[categoryCounts])는 조회 필터(visited·category)와
 * 무관하게 살아있는 저장 레코드 전체 기준이다 — 탭·칩 배지는 어느 탭에서도 같은 값을 보여주기 때문(모킹 계약과 동일).
 */
data class SavedPlacesResult(
    // 카테고리 전체 합산 저장 개수 — 저장함 "전체 N" 칩
    val totalCount: Long,
    // 미방문 저장 개수 — 미방문 탭 배지
    val unvisitedCount: Long,
    // 방문 저장 개수 — 방문 탭 배지
    val visitedCount: Long,
    // 카테고리별 저장 개수(0개 카테고리 제외), 개수 내림차순 — 카테고리 칩 배지
    val categoryCounts: List<CategoryCount>,
    val nextCursor: String?,
    val hasNext: Boolean,
    val savedPlaces: List<SavedPlaceItem>,
) {
    /** category 는 [com.example.backend.user.domain.model.SavedPlaceCategory] 이름 문자열(예: CAFE). */
    data class CategoryCount(
        val category: String,
        val count: Long,
    )

    data class SavedPlaceItem(
        // 저장 레코드 id (장소 id 아님)
        val id: Long,
        val placeId: Long,
        // 저장 시 place 도메인에서 복사한 장소 카테고리 스냅샷의 이름 문자열(미분류면 null)
        val category: String?,
        // 방문 여부 — 저장함 미방문/방문 탭 구분. PATCH 방문 처리 API로 전환된다.
        val visited: Boolean,
        val savedAt: Instant,
    )
}
