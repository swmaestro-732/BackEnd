package com.example.backend.user.adapter.inbound.web.response

import com.example.backend.user.application.port.inbound.dto.SavedPlacesResult
import com.example.backend.user.domain.model.SavedPlaceCategory
import java.time.Instant

/**
 * 저장 장소 조회 응답 — 노션 API 명세(User · user-place · 저장 장소 조회) 기준.
 * 노션 필드 명세 미작성 상태라 저장 레코드 스키마(saved_places)와 디자인(저장함 · 장소 탭)에서 도출.
 *
 * api-spec.md 설계 노트에 따라 이 도메인 API는 화면용이 아니라 **저장 레코드(ID 위주)**를 반환한다
 * — 장소 상세(이름·평점·거리 등)는 화면 조합 API(`GET /service/v1/my/saved-places`, 제안 상태)가 담당.
 * 페이지 메타: 커서 페이지네이션([nextCursor]/[hasNext]) + 전체 개수([totalCount], 저장함 "전체 N" 칩).
 * 카운트: 미방문/방문 탭 배지([unvisitedCount]/[visitedCount]) + 카테고리 칩 배지([categoryCounts], 0개는 제외).
 */
data class SavedPlaceListResponse(
    // 카테고리 전체 합산 저장 개수 — 저장함 "전체 N" 칩
    val totalCount: Int,
    // 미방문 저장 개수 — 미방문 탭
    val unvisitedCount: Int,
    // 방문 저장 개수 — 방문 탭
    val visitedCount: Int,
    // 카테고리별 저장 개수(0개 카테고리 제외), 개수 내림차순 — 카테고리 칩
    val categoryCounts: List<CategoryCount>,
    val nextCursor: String?,
    val hasNext: Boolean,
    val savedPlaces: List<SavedPlaceItem>,
) {
    /** category 는 [SavedPlaceCategory] 이름 문자열(예: CAFE) — enum 직렬화 결과와 동일한 와이어 포맷이다. */
    data class CategoryCount(
        val category: String,
        val count: Int,
    )

    data class SavedPlaceItem(
        // 저장 레코드 id (장소 id 아님)
        val id: Long,
        val placeId: Long,
        // 저장 시 place 도메인에서 복사한 장소 카테고리 스냅샷(저장함 카테고리 칩) — enum 이름 문자열
        val category: String?,
        // 방문 여부 — 저장함 미방문/방문 탭 구분. PATCH 방문 처리 API로 전환된다.
        val visited: Boolean,
        val savedAt: Instant,
    )

    companion object {
        /** 인바운드 포트 결과([SavedPlacesResult])를 웹 응답으로 변환한다. */
        fun from(result: SavedPlacesResult): SavedPlaceListResponse =
            SavedPlaceListResponse(
                totalCount = result.totalCount.toInt(),
                unvisitedCount = result.unvisitedCount.toInt(),
                visitedCount = result.visitedCount.toInt(),
                categoryCounts =
                    result.categoryCounts.map {
                        CategoryCount(category = it.category, count = it.count.toInt())
                    },
                nextCursor = result.nextCursor,
                hasNext = result.hasNext,
                savedPlaces =
                    result.savedPlaces.map {
                        SavedPlaceItem(
                            id = it.id,
                            placeId = it.placeId,
                            category = it.category,
                            visited = it.visited,
                            savedAt = it.savedAt,
                        )
                    },
            )

        /**
         * 목 저장 레코드 — 디자인(저장함 · 장소 · 리스트)의 예시 목록 반영. 최신 저장순 고정 응답.
         * placeId 는 장소 검색 모킹([com.example.backend.place.adapter.inbound.web.PlaceController])의
         * 목 장소 id 와 맞춰 두었다(101 어니언 성수, 104 대림창고 카페, 105 센터커피 성수).
         */
        fun mock(): SavedPlaceListResponse {
            val savedPlaces =
                listOf(
                    SavedPlaceItem(
                        id = 5,
                        placeId = 108, // 평화양조장
                        category = SavedPlaceCategory.BAR.name,
                        visited = false,
                        savedAt = Instant.parse("2026-07-17T09:20:00Z"),
                    ),
                    SavedPlaceItem(
                        id = 4,
                        placeId = 107, // 자그마치
                        category = SavedPlaceCategory.CULTURE.name,
                        visited = false,
                        savedAt = Instant.parse("2026-07-15T13:05:00Z"),
                    ),
                    SavedPlaceItem(
                        id = 3,
                        placeId = 105, // 센터커피 성수
                        category = SavedPlaceCategory.CAFE.name,
                        visited = false,
                        savedAt = Instant.parse("2026-07-12T05:30:00Z"),
                    ),
                    SavedPlaceItem(
                        id = 2,
                        placeId = 104, // 대림창고 카페
                        category = SavedPlaceCategory.CAFE.name,
                        visited = true,
                        savedAt = Instant.parse("2026-07-08T11:00:00Z"),
                    ),
                    SavedPlaceItem(
                        id = 1,
                        placeId = 101, // 어니언 성수
                        category = SavedPlaceCategory.CAFE.name,
                        visited = true,
                        savedAt = Instant.parse("2026-07-05T02:15:00Z"),
                    ),
                )
            return SavedPlaceListResponse(
                totalCount = savedPlaces.size,
                unvisitedCount = savedPlaces.count { !it.visited },
                visitedCount = savedPlaces.count { it.visited },
                categoryCounts =
                    savedPlaces
                        .mapNotNull { it.category }
                        .groupingBy { it }
                        .eachCount()
                        .map { (category, count) -> CategoryCount(category, count) }
                        .sortedByDescending { it.count },
                nextCursor = null,
                hasNext = false,
                savedPlaces = savedPlaces,
            )
        }
    }
}
