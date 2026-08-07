package com.example.backend.mobile.user.adapter.inbound.web.response

import com.example.backend.mobile.place.adapter.inbound.web.response.PlaceLocationResponse
import com.example.backend.mobile.user.application.port.inbound.dto.SavedCourseScreenResult
import java.time.Instant

/**
 * 웹 응답 DTO — 저장함 · 코스 탭 화면 조합(BFF). 프론트 화면 계약 형태.
 * 도메인 API(`GET /service/v1/saved-courses`)의 저장 레코드·카운트·페이지 메타를 유지하고,
 * 각 항목에 코스 요약(제목·지역·테마·장소 수·소요 시간·작성자 — 디자인 J 밴드)과
 * 완주 상태(trace)를 덧붙여 내려준다.
 */
data class SavedCourseScreenResponse(
    // 전체 저장 코스 개수 — 저장함 "전체 N" 칩
    val totalCount: Int,
    // 안 가봄/완주 개수 — 상태 필터 칩 배지
    val uncompletedCount: Int,
    val completedCount: Int,
    // 폴더 칩 — 폴더별 저장 개수(order_no 순)
    val folders: List<CourseFolderCountResponse>,
    val nextCursor: String?,
    val hasNext: Boolean,
    val savedCourses: List<SavedCourseScreenItemResponse>,
) {
    companion object {
        private fun image(id: String) = "https://images.unsplash.com/$id?w=600&q=80&auto=format&fit=crop"

        private fun place(
            orderNo: Int,
            placeId: Long,
            name: String,
            category: String,
            address: String,
            imageId: String,
            latitude: Double,
            longitude: Double,
            caption: String? = null,
        ) = SavedCoursePlaceResponse(
            orderNo = orderNo,
            placeId = placeId,
            name = name,
            category = category,
            address = address,
            imageUrls = listOf(image(imageId)),
            caption = caption,
            location = PlaceLocationResponse(latitude, longitude),
        )

        private val AUTHOR_HYUNWOO =
            CourseAuthorSummaryResponse(
                id = 1,
                handle = "hyunwoo",
                nickname = "현우",
                profileImageUrl = image("photo-1547425260-76bcadfb4f2c"),
            )
        private val AUTHOR_JIHO =
            CourseAuthorSummaryResponse(
                id = 2,
                handle = "jiho_routes",
                nickname = "지호",
                profileImageUrl = image("photo-1502685104226-ee32379fefbe"),
            )
        private val AUTHOR_SEOUL =
            CourseAuthorSummaryResponse(
                id = 3,
                handle = "slow_seoul",
                nickname = "슬로우서울",
                profileImageUrl = image("photo-1544005313-94ddf0286df2"),
            )

        /** 폴더 칩 — 코스 폴더 목록 모킹([com.example.backend.user.adapter.inbound.web.CourseFolderController])과 값을 맞춰 두었다. */
        private val MOCK_FOLDERS: List<CourseFolderCountResponse> =
            listOf(
                CourseFolderCountResponse(id = 1, name = "데이트 코스", count = 2),
                CourseFolderCountResponse(id = 2, name = "주말 나들이", count = 1),
                CourseFolderCountResponse(id = 3, name = "혼자 걷기", count = 1),
            )

        /**
         * 목 데이터 — 저장 레코드는 도메인 모킹([com.example.backend.user.adapter.inbound.web.SavedCourseController])과,
         * courseId=1 코스는 코스 상세 화면 조합 목([com.example.backend.bff.adapter.inbound.web.CourseDetailScreenController]: 비 오는 날 성수 감성 카페 코스,
         * jiho_routes)과 값을 맞춰 두었다. 나머지 코스·좌표는 디자인(저장함 · 코스 · 리스트/지도) 예시 기준.
         * 작성자 id 는 로그인 사용자 목(id=1 · AccountController)과 겹치지 않게 두고, isMine 코스만 id=1 로 맞췄다.
         */
        private val MOCK_ITEMS: List<SavedCourseScreenItemResponse> =
            listOf(
                SavedCourseScreenItemResponse(
                    id = 4,
                    courseId = 3,
                    folderId = 1,
                    savedAt = Instant.parse("2026-07-18T10:40:00Z"),
                    completed = false,
                    completedAt = null,
                    isMine = false,
                    course =
                        SavedCourseSummaryResponse(
                            title = "성수 골목 소품샵 산책",
                            coverImageUrl = image("photo-1509042239860-f550ce710b93"),
                            area = "성수",
                            theme = "SHOPPING",
                            placeCount = 3,
                            durationText = "약 15분",
                            author = AUTHOR_SEOUL,
                            places =
                                listOf(
                                    place(
                                        0,
                                        105,
                                        "성수연방",
                                        "SHOPPING",
                                        "서울 성동구 성수이로14길 14",
                                        "photo-1441986300917-64674bd600d8",
                                        37.5432,
                                        127.0566,
                                    ),
                                    place(
                                        1,
                                        106,
                                        "어반소스 성수",
                                        "SHOPPING",
                                        "서울 성동구 연무장길 33",
                                        "photo-1472851294608-062f824d29cc",
                                        37.5448,
                                        127.0551,
                                    ),
                                    place(
                                        2,
                                        102,
                                        "대림창고 카페",
                                        "CAFE",
                                        "서울 성동구 성수동2가 78-78",
                                        "photo-1509042239860-f550ce710b93",
                                        37.5418,
                                        127.0592,
                                    ),
                                ),
                        ),
                ),
                SavedCourseScreenItemResponse(
                    id = 3,
                    courseId = 2,
                    folderId = 1,
                    savedAt = Instant.parse("2026-07-15T13:05:00Z"),
                    completed = false,
                    completedAt = null,
                    isMine = false,
                    course =
                        SavedCourseSummaryResponse(
                            title = "비 오는 날 성수 감성 카페 코스",
                            coverImageUrl = image("photo-1445116572660-236099ec97a0"),
                            area = "성수",
                            theme = "CAFETOUR",
                            placeCount = 4,
                            durationText = "약 21분",
                            author = AUTHOR_JIHO,
                            places =
                                listOf(
                                    place(
                                        0,
                                        101,
                                        "어니언 성수",
                                        "CAFE",
                                        "서울 성동구 성수동2가 277-135",
                                        "photo-1517433670267-08bbd4be890f",
                                        37.5445,
                                        127.0578,
                                    ),
                                    place(
                                        1,
                                        103,
                                        "센터커피 성수",
                                        "CAFE",
                                        "서울 성동구 성수동1가 656-566",
                                        "photo-1442512595331-e89e73853f31",
                                        37.5463,
                                        127.0537,
                                    ),
                                    place(
                                        2,
                                        104,
                                        "자그마치",
                                        "CULTURE",
                                        "서울 성동구 성수이로 88",
                                        "photo-1513151233558-d860c5398176",
                                        37.5426,
                                        127.0554,
                                    ),
                                    place(
                                        3,
                                        102,
                                        "대림창고 카페",
                                        "CAFE",
                                        "서울 성동구 성수동2가 78-78",
                                        "photo-1509042239860-f550ce710b93",
                                        37.5418,
                                        127.0592,
                                    ),
                                ),
                        ),
                ),
                SavedCourseScreenItemResponse(
                    id = 2,
                    courseId = 4,
                    folderId = 2,
                    savedAt = Instant.parse("2026-07-13T08:30:00Z"),
                    completed = false,
                    completedAt = null,
                    isMine = false,
                    course =
                        SavedCourseSummaryResponse(
                            title = "주말 연남 느긋한 브런치 산책",
                            coverImageUrl = image("photo-1528605248644-14dd04022da1"),
                            area = "연남",
                            theme = "FOOD",
                            placeCount = 5,
                            durationText = "약 26분",
                            author = AUTHOR_SEOUL,
                            places =
                                listOf(
                                    place(
                                        0,
                                        107,
                                        "연남 브런치하우스",
                                        "RESTAURANT",
                                        "서울 마포구 연남동 227-15",
                                        "photo-1528605248644-14dd04022da1",
                                        37.5600,
                                        126.9250,
                                    ),
                                    place(
                                        1,
                                        108,
                                        "연남동 골목카페",
                                        "CAFE",
                                        "서울 마포구 성미산로 161",
                                        "photo-1554118811-1e0d58224f24",
                                        37.5619,
                                        126.9236,
                                    ),
                                    place(
                                        2,
                                        109,
                                        "경의선숲길 연남",
                                        "NATURE",
                                        "서울 마포구 연남동 385",
                                        "photo-1441974231531-c6227db76b6e",
                                        37.5588,
                                        126.9262,
                                    ),
                                    place(
                                        3,
                                        110,
                                        "연남 소품샵",
                                        "SHOPPING",
                                        "서울 마포구 동교로 245",
                                        "photo-1472851294608-062f824d29cc",
                                        37.5628,
                                        126.9270,
                                    ),
                                    place(
                                        4,
                                        111,
                                        "연남 화덕피자",
                                        "RESTAURANT",
                                        "서울 마포구 월드컵북로6길 61",
                                        "photo-1513104890138-7c749659a591",
                                        37.5645,
                                        126.9243,
                                    ),
                                ),
                        ),
                ),
                SavedCourseScreenItemResponse(
                    id = 1,
                    courseId = 5,
                    folderId = 3,
                    savedAt = Instant.parse("2026-03-10T02:00:00Z"),
                    completed = true,
                    completedAt = Instant.parse("2026-03-12T07:30:00Z"),
                    isMine = true,
                    course =
                        SavedCourseSummaryResponse(
                            title = "한남동 갤러리 하나씩 도장깨기",
                            coverImageUrl = image("photo-1554118811-1e0d58224f24"),
                            area = "한남",
                            theme = "CULTURE",
                            placeCount = 3,
                            durationText = "약 17분",
                            author = AUTHOR_HYUNWOO,
                            places =
                                listOf(
                                    place(
                                        0,
                                        112,
                                        "리움미술관",
                                        "CULTURE",
                                        "서울 용산구 이태원로55길 60-16",
                                        "photo-1518998053901-5348d3961a04",
                                        37.5385,
                                        127.0000,
                                    ),
                                    place(
                                        1,
                                        113,
                                        "한남 갤러리길",
                                        "CULTURE",
                                        "서울 용산구 대사관로 35",
                                        "photo-1513151233558-d860c5398176",
                                        37.5346,
                                        127.0043,
                                    ),
                                    place(
                                        2,
                                        114,
                                        "블루스퀘어",
                                        "LANDMARK",
                                        "서울 용산구 이태원로 294",
                                        "photo-1470229722913-7c0e2dbbafd3",
                                        37.5326,
                                        127.0086,
                                    ),
                                ),
                        ),
                ),
            )

        /** 저장함 · 코스 탭 화면 조합 목 — 항상 고정 목 응답(nextCursor=null·hasNext=false). */
        fun mock(): SavedCourseScreenResponse =
            SavedCourseScreenResponse(
                totalCount = MOCK_ITEMS.size,
                uncompletedCount = MOCK_ITEMS.count { !it.completed },
                completedCount = MOCK_ITEMS.count { it.completed },
                folders = MOCK_FOLDERS,
                nextCursor = null,
                hasNext = false,
                savedCourses = MOCK_ITEMS,
            )

        /** 화면 조합 결과([SavedCourseScreenResult])를 웹 응답으로 변환한다. */
        fun from(result: SavedCourseScreenResult): SavedCourseScreenResponse {
            val total = result.totalCount.toInt()
            val completed = result.completedCount.toInt()
            return SavedCourseScreenResponse(
                totalCount = total,
                uncompletedCount = total - completed,
                completedCount = completed,
                folders =
                    result.folders.map {
                        CourseFolderCountResponse(
                            id = it.id,
                            name = it.name,
                            count = it.count,
                        )
                    },
                nextCursor = result.nextCursor,
                hasNext = result.hasNext,
                savedCourses = result.items.map { toItem(it, result.viewerId) },
            )
        }

        private fun toItem(
            item: SavedCourseScreenResult.Item,
            viewerId: Long,
        ): SavedCourseScreenItemResponse {
            val course = item.course
            val walkingMinutes = course.places.sumOf { it.walkingMinutesToNext ?: 0 }
            // 삭제·누락으로 placeById 에서 해석되지 않는 장소는 제외한 목록 — placeCount·places 를 이 하나에서 파생한다.
            val resolvedPlaces =
                course.places
                    .sortedBy { it.orderNo }
                    .mapNotNull { coursePlace ->
                        item.placeById[coursePlace.placeId]?.let { p ->
                            SavedCoursePlaceResponse(
                                orderNo = coursePlace.orderNo,
                                placeId = p.id,
                                name = p.name,
                                category = p.category,
                                address = p.address,
                                // 코스에서 이 장소에 올린 사진들(course_place_images) — 장소 자체 이미지가 아님.
                                imageUrls = coursePlace.images.sortedBy { it.orderNo }.map { it.imageUrl },
                                caption = coursePlace.caption,
                                location = PlaceLocationResponse(p.latitude, p.longitude),
                            )
                        }
                    }
            return SavedCourseScreenItemResponse(
                id = item.savedId,
                courseId = course.id,
                folderId = item.folderId,
                savedAt = item.savedAt,
                completed = item.completedAt != null,
                completedAt = item.completedAt,
                isMine = course.authorId == viewerId,
                course =
                    SavedCourseSummaryResponse(
                        title = course.title,
                        coverImageUrl = course.coverImageUrl.ifBlank { null },
                        area = course.area,
                        theme = course.theme,
                        placeCount = resolvedPlaces.size,
                        durationText = durationText(walkingMinutes),
                        author =
                            CourseAuthorSummaryResponse(
                                id = item.author.id,
                                handle = item.author.handle,
                                nickname = item.author.nickname,
                                profileImageUrl = item.author.profileImageUrl,
                            ),
                        places = resolvedPlaces,
                    ),
            )
        }

        /** 코스 장소 간 도보 시간 합(분)을 카드 표기 텍스트로 만든다. 도보 정보가 없으면 null. */
        private fun durationText(walkingMinutes: Int): String? =
            when {
                walkingMinutes <= 0 -> null
                walkingMinutes < 60 -> "약 ${walkingMinutes}분"
                else -> "약 ${(walkingMinutes + 30) / 60}시간"
            }
    }
}

/** 폴더 칩 배지 — 코스 폴더 목록 API(`GET /service/v1/course-folders`)와 같은 폴더. */
data class CourseFolderCountResponse(
    val id: Long,
    val name: String,
    val count: Int,
)

data class SavedCourseScreenItemResponse(
    // 저장 레코드 id (코스 id 아님)
    val id: Long,
    val courseId: Long,
    // 저장된 폴더 id — null 이면 폴더 미분류(saved_courses.folder_id NULL 허용)
    val folderId: Long?,
    val savedAt: Instant,
    // 완주 여부 — 안 가봄/완주 칩 구분. "3월 12일 완주" 날짜 표기는 completedAt 을 클라이언트가 포맷
    val completed: Boolean,
    val completedAt: Instant?,
    // 내가 만든 코스 여부 — 카드에 작성자 핸들 대신 "내가 만든 코스" 배지
    val isMine: Boolean,
    val course: SavedCourseSummaryResponse,
)

/** 코스 요약 — 저장함 코스 카드에 표시되는 course 도메인 정보. */
data class SavedCourseSummaryResponse(
    val title: String,
    val coverImageUrl: String?,
    // 카드 배지 "성수 · 데이트" — 지역·테마. 코스에 값이 없으면 null(클라이언트가 해당 배지 생략).
    val area: String?,
    val theme: String?,
    // "장소 4곳"
    val placeCount: Int,
    // 소요 시간 표시 텍스트(예: "약 3시간") — 코스 장소 간 도보 시간 합으로 계산. 도보 정보가 없으면 null.
    val durationText: String?,
    val author: CourseAuthorSummaryResponse,
    // 코스에 담긴 장소들(orderNo 0부터) — 지도 탭 핀 + 장소 미리보기용 상세(이름·카테고리·이미지·주소).
    val places: List<SavedCoursePlaceResponse>,
)

/** 코스 작성자 — 카드 표기(닉네임·핸들·프로필). id 는 작성자 페이지 링크 대비(api-design). handle 은 미설정 시 null. */
data class CourseAuthorSummaryResponse(
    val id: Long,
    val handle: String?,
    val nickname: String,
    val profileImageUrl: String?,
)

/** 코스에 담긴 장소 한 곳 — 지도 핀(location) + 장소 상세(이름·카테고리·주소)·코스 사진·캡션. */
data class SavedCoursePlaceResponse(
    val orderNo: Int,
    val placeId: Long,
    val name: String,
    val category: String,
    val address: String,
    // 이 코스에서 이 장소에 올린 사진들(course_place_images, orderNo 순). 장소 자체 이미지가 아니다. 여러 장 가능.
    val imageUrls: List<String>,
    // 이 코스에서 이 장소에 붙인 캡션(course_places.caption). 없으면 null.
    val caption: String?,
    val location: PlaceLocationResponse,
)
