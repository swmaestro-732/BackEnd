package com.example.backend.mobile.home.adapter.inbound.web.response

import com.example.backend.mobile.home.application.port.inbound.dto.HomeFeedResult
import java.time.Instant

/**
 * 웹 응답 DTO — 공개 코스 피드 화면 조합(BFF). 프론트 화면 계약 형태.
 * 저장수 내림차순·최신순으로 랭킹된 코스 목록([Item])과 nextCursor/hasNext를 내려준다.
 * 목([MOCK]) 응답에만 고정 예시를 채워 프론트가 형태를 확인할 수 있게 한다.
 */
data class HomeFeedResponse(
    val courses: List<Item>,
    val nextCursor: String?,
    val hasNext: Boolean,
) {
    /** 피드 카드 한 장 — 코스 요약 + 지표(저장수는 실제 saved_courses 집계값). */
    data class Item(
        val id: Long,
        val authorId: Long,
        val title: String,
        val coverImageUrl: String?,
        val theme: String?,
        val likesCnt: Int,
        val savesCnt: Int,
        val createdAt: Instant,
    )

    companion object {
        fun from(result: HomeFeedResult): HomeFeedResponse =
            HomeFeedResponse(
                courses =
                    result.courses.map {
                        Item(
                            id = it.id,
                            authorId = it.authorId,
                            title = it.title,
                            coverImageUrl = it.coverImageUrl,
                            theme = it.theme,
                            likesCnt = it.likesCnt,
                            savesCnt = it.savesCnt,
                            createdAt = it.createdAt,
                        )
                    },
                nextCursor = result.nextCursor,
                hasNext = result.hasNext,
            )

        private fun image(token: String) = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9Gc$token&s=10"

        /**
         * `?mock=true` 폴백 응답 — 시드/DB 없이 프론트가 붙어볼 수 있게 고정 피드 목을 내려준다.
         * 저장수 내림차순·최신순 정렬 결과를 흉내낸다(첫 카드가 저장수 최다).
         */
        val MOCK: HomeFeedResponse =
            HomeFeedResponse(
                courses =
                    listOf(
                        Item(
                            id = 1,
                            authorId = 1,
                            title = "비 오는 날 성수 감성 카페 코스",
                            coverImageUrl = image("HDBpbwjQOwLbBj3pgro4xFRpvBdRRZDTcbVmMkg"),
                            theme = "CAFETOUR",
                            likesCnt = 128,
                            savesCnt = 342,
                            createdAt = Instant.parse("2026-07-20T02:30:00Z"),
                        ),
                        Item(
                            id = 2,
                            authorId = 3,
                            title = "연남동 브런치 & 산책 코스",
                            coverImageUrl = image("SYjLV1q0A21vyJJ_N3LlUSp3HwiDDouEZRzcVhJb8KJw"),
                            theme = "WALK",
                            likesCnt = 74,
                            savesCnt = 205,
                            createdAt = Instant.parse("2026-07-24T05:10:00Z"),
                        ),
                        Item(
                            id = 3,
                            authorId = 5,
                            title = "을지로 노포 술집 투어",
                            coverImageUrl = image("Qr6pSHzsT4DD0ieT5VQ__SVo2ErRODzDyViWmZeXHGlA"),
                            theme = "FOODIE",
                            likesCnt = 41,
                            savesCnt = 96,
                            createdAt = Instant.parse("2026-07-28T09:45:00Z"),
                        ),
                    ),
                nextCursor = null,
                hasNext = false,
            )
    }
}
