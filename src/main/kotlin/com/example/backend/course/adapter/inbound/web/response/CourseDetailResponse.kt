package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.course.application.port.inbound.dto.CourseDetailResult
import com.example.backend.course.domain.model.CourseVisibility

/**
 * 웹 응답 DTO — 코스 상세. 프론트 계약대로 최상위를 { "course": {...} } 로 감싼다
 * (공통 ApiResponse.data 안에 들어간다). id 는 외부 계약상 문자열로 다룬다.
 *
 * 표시 로직(도보 시간 합계·팔로우/따라가기 축약 라벨)은 이 계층의 [from] 매퍼가 담당한다 —
 * 애플리케이션 결과([CourseDetailResult])는 원시값만 넘겨준다.
 * 하위 응답 DTO([CourseResponse]·[CourseStatsResponse]·[CoursePlaceResponse]·
 * [CoursePlaceImageResponse]·[CourseViewerResponse])는 같은 패키지의 개별 파일로 나눠 둔다.
 */
data class CourseDetailResponse(
    val course: CourseResponse,
) {
    companion object {
        fun from(result: CourseDetailResult): CourseDetailResponse = CourseDetailResponse(CourseResponse.from(result))

        private fun image(token: String) = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9Gc$token&s=10"

        private fun img(
            token: String,
            orderNo: Int,
        ) = CoursePlaceImageResponse(imageUrl = image(token), orderNo = orderNo)

        /**
         * 코스 상세 `?mock=true` 폴백 응답 — 디자인(코스 상세)의 예시 반영. 화면 조합 목
         * ([com.example.backend.mobile.course.adapter.inbound.web.CourseMobileController])과 같은 코스
         * (비 오는 날 성수 감성 카페 코스)로 값을 맞춰 두었다. caption 은 장소명.
         * 실구현 전환 시 호출부와 함께 제거한다.
         */
        val MOCK: CourseDetailResponse =
            CourseDetailResponse(
                course =
                    CourseResponse(
                        id = "1",
                        title = "비 오는 날 성수 감성 카페 코스",
                        coverImageUrl = image("THb4AHDBpbwjQOwLbBj3pgro4xFRpvBdRRZDTcbVmMkg"),
                        theme = "데이트",
                        tags = listOf("감성카페", "비오는날", "성수동"),
                        description =
                            "비가 오면 더 예쁜 성수 카페만 골라 담았어요. 전부 도보로 이어지고, " +
                                "장소마다 제 팁을 남겨뒀으니 참고하세요 🌧️",
                        visibility = CourseVisibility.PUBLIC,
                        stats =
                            CourseStatsResponse(
                                placeCount = 4,
                                walkingMinutes = 14,
                                tracingCount = 1200,
                            ),
                        authorId = 1L,
                        places =
                            listOf(
                                CoursePlaceResponse(
                                    id = 1L,
                                    placeId = 101L,
                                    orderNo = 0,
                                    caption = "어니언 성수",
                                    walkingMinutesToNext = 6,
                                    images =
                                        listOf(
                                            img("THIxFwvmFDIDNW9rHdqN1wRMZjFTQwfEmgO-O4kBM5nA", 0),
                                            img("Qri_COfUpGil6k79RTh7vRhzDdP08yEcUmXIHnvn7Hfw", 1),
                                        ),
                                ),
                                CoursePlaceResponse(
                                    id = 2L,
                                    placeId = 102L,
                                    orderNo = 1,
                                    caption = "대림창고 갤러리",
                                    walkingMinutesToNext = 3,
                                    images = listOf(img("SYjLV1q0A21vyJJ_N3LlUSp3HwiDDouEZRzcVhJb8KJw", 0)),
                                ),
                                CoursePlaceResponse(
                                    id = 3L,
                                    placeId = 103L,
                                    orderNo = 2,
                                    caption = "센터커피 성수",
                                    walkingMinutesToNext = 5,
                                    images = listOf(img("TMRMGDnfUqzsxQXY1TOrhMtWZ8-otKbsLPlfnIkvDfUw", 0)),
                                ),
                                CoursePlaceResponse(
                                    id = 4L,
                                    placeId = 104L,
                                    orderNo = 3,
                                    caption = "카페 할아버지공장",
                                    walkingMinutesToNext = null,
                                    images =
                                        listOf(
                                            img("Qr6pSHzsT4DD0ieT5VQ__SVo2ErRODzDyViWmZeXHGlA", 0),
                                            img("R_3CDZ5UcouOOEkvGYQVI2emgnCGRIzRysaKhwNlq-kw", 1),
                                        ),
                                ),
                            ),
                        viewer =
                            CourseViewerResponse(
                                hasSaved = false,
                                hasStartedCourse = false,
                            ),
                    ),
            )
    }
}
