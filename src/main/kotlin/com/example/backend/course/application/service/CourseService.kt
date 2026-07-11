package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.course.application.dto.CourseAuthorResult
import com.example.backend.course.application.dto.CourseDetailResult
import com.example.backend.course.application.dto.CoursePlaceResult
import com.example.backend.course.application.dto.CourseReviewAuthorResult
import com.example.backend.course.application.dto.CourseReviewResult
import com.example.backend.course.application.dto.CourseReviewSummaryResult
import com.example.backend.course.application.dto.CourseStatsResult
import com.example.backend.course.application.dto.CourseViewerResult
import com.example.backend.course.application.port.inbound.CourseUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * 유스케이스 구현. 인바운드 포트([CourseUseCase])를 구현한다.
 *
 * MOCK(SCRUM-223): 아직 아웃바운드 포트/영속성 어댑터가 없어 고정 데이터를 반환한다.
 * 실제 구현 시 CoursePersistencePort(아웃바운드)를 주입받아 이 하드코딩만 교체하면 되고,
 * 컨트롤러·인바운드 포트·Result/Response 는 그대로 둔다.
 */
@Service
@Transactional(readOnly = true)
class CourseService : CourseUseCase {
    override fun getDetail(courseId: Long): CourseDetailResult =
        MOCK_COURSES[courseId]
            ?: throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "코스를 찾을 수 없습니다: id=$courseId")

    private companion object {
        /** id=1 만 성공 데이터, 나머지는 404 */
        val MOCK_COURSES: Map<Long, CourseDetailResult> =
            mapOf(
                1L to
                    CourseDetailResult(
                        id = "1",
                        title = "비 오는 날 성수 감성 카페 코스",
                        coverImageUrl =
                            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTHb4AHDBpbwjQOwLbBj3pgro4xFRpvBdRRZDTcbVmMkg&s=10",
                        themes = listOf("데이트"),
                        description =
                            "비가 오면 더 예쁜 성수 카페만 골라 담았어요. 전부 도보로 이어지고, " +
                                "장소마다 제 팁을 남겨뒀으니 참고하세요 🌧️",
                        stats =
                            CourseStatsResult(
                                placeCount = 4,
                                walkingMinutes = 20,
                                followerCount = 1200,
                                followerCountLabel = "1.2k",
                            ),
                        author =
                            CourseAuthorResult(
                                id = "1",
                                nickname = "지호님",
                                handle = "jiho_routes",
                                profileImageUrl =
                                    "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQi7ZSFKA2brmDYt72J8vLDQxgOJKxs-lj4tavhXo_pEA&s=10",
                                isFollowing = false,
                            ),
                        places =
                            listOf(
                                CoursePlaceResult(
                                    order = 1,
                                    id = "1",
                                    name = "어니언 성수",
                                    categories = listOf("카페", "베이커리"),
                                    thumbnailUrl =
                                        "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTHIxFwvmFDIDNW9rHdqN1wRMZjFTQwfEmgO-O4kBM5nA&s=10",
                                    authorTip = "통창 자리가 명당이에요. 비 오는 날 앉으면 뷰가 최고.",
                                    label = "도보 6분",
                                ),
                                CoursePlaceResult(
                                    order = 2,
                                    id = "2",
                                    name = "대림창고 갤러리",
                                    categories = listOf("전시", "카페"),
                                    thumbnailUrl =
                                        "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSYjLV1q0A21vyJJ_N3LlUSp3HwiDDouEZRzcVhJb8KJw&s=10",
                                    authorTip = "안쪽 전시 공간 꼭 들러보세요.",
                                    label = "도보 3분",
                                ),
                                CoursePlaceResult(
                                    order = 3,
                                    id = "3",
                                    name = "센터커피 성수",
                                    categories = listOf("카페"),
                                    thumbnailUrl =
                                        "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTMRMGDnfUqzsxQXY1TOrhMtWZ8-otKbsLPlfnIkvDfUw&s=10",
                                    authorTip = "원두 향이 좋아요. 2층 창가 추천.",
                                    label = "도보 5분",
                                ),
                                CoursePlaceResult(
                                    order = 4,
                                    id = "4",
                                    name = "카페 할아버지공장",
                                    categories = listOf("카페", "베이커리"),
                                    thumbnailUrl =
                                        "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQr6pSHzsT4DD0ieT5VQ__SVo2ErRODzDyViWmZeXHGlA&s=10",
                                    authorTip = "마무리로 딱. 넓어서 웨이팅 걱정 없어요.",
                                    label = null,
                                ),
                            ),
                        reviewSummary =
                            CourseReviewSummaryResult(
                                averageRating = 4.8,
                                totalCount = 128,
                                reviews =
                                    listOf(
                                        CourseReviewResult(
                                            id = "1",
                                            author =
                                                CourseReviewAuthorResult(
                                                    nickname = "소마님",
                                                    profileImageUrl =
                                                        "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSw5pb0y29fQQhEAjcazcmovozw_80MX3PBd_8mlxGtNA&s=10",
                                                ),
                                            rating = 5,
                                            content =
                                                "비 오는 날 딱이에요. 통창 자리 순서대로 도니 동선도 완벽했어요. " +
                                                    "웨이팅도 거의 없었어요 🌧️",
                                            createdAt =
                                                OffsetDateTime.of(2026, 7, 7, 13, 20, 0, 0, ZoneOffset.ofHours(9)),
                                            relativeTime = "2일 전",
                                            photoUrls =
                                                listOf(
                                                    "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQri_COfUpGil6k79RTh7vRhzDdP08yEcUmXIHnvn7Hfw&s=10",
                                                    "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR_3CDZ5UcouOOEkvGYQVI2emgnCGRIzRysaKhwNlq-kw&s=10",
                                                ),
                                        ),
                                        CourseReviewResult(
                                            id = "2",
                                            author =
                                                CourseReviewAuthorResult(
                                                    nickname = "커피러버",
                                                    profileImageUrl =
                                                        "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSqUUfhoYwxXJCW781fTarStEzoNyMS9vYcpAnhNVbjeg&s=10",
                                                ),
                                            rating = 4,
                                            content = "코스 좋아요! 세 번째 카페가 조금 붐볐어요.",
                                            createdAt =
                                                OffsetDateTime.of(2026, 7, 5, 18, 5, 0, 0, ZoneOffset.ofHours(9)),
                                            relativeTime = "4일 전",
                                            photoUrls = emptyList(),
                                        ),
                                    ),
                            ),
                        viewer =
                            CourseViewerResult(
                                hasSaved = false,
                                hasStartedCourse = false,
                            ),
                    ),
            )
    }
}
