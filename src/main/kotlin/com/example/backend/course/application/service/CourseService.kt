package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.course.application.port.inbound.CourseUseCase
import com.example.backend.course.application.port.inbound.dto.CourseDetailResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceImageResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceResult
import com.example.backend.course.application.port.inbound.dto.CourseStatsResult
import com.example.backend.course.application.port.inbound.dto.CourseViewerResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
                                tracingCountLabel = "1.2k",
                            ),
                        authorId = 1L,
                        places =
                            listOf(
                                CoursePlaceResult(
                                    id = 1L,
                                    placeId = 101L,
                                    orderNo = 1,
                                    caption = "어니언 성수",
                                    walkingMinutesToNext = 6,
                                    images =
                                        listOf(
                                            CoursePlaceImageResult(
                                                imageUrl =
                                                    "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTHIxFwvmFDIDNW9rHdqN1wRMZjFTQwfEmgO-O4kBM5nA&s=10",
                                                orderNo = 0,
                                            ),
                                            CoursePlaceImageResult(
                                                imageUrl =
                                                    "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQri_COfUpGil6k79RTh7vRhzDdP08yEcUmXIHnvn7Hfw&s=10",
                                                orderNo = 1,
                                            ),
                                        ),
                                ),
                                CoursePlaceResult(
                                    id = 2L,
                                    placeId = 102L,
                                    orderNo = 2,
                                    caption = "대림창고 갤러리",
                                    walkingMinutesToNext = 3,
                                    images =
                                        listOf(
                                            CoursePlaceImageResult(
                                                imageUrl =
                                                    "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSYjLV1q0A21vyJJ_N3LlUSp3HwiDDouEZRzcVhJb8KJw&s=10",
                                                orderNo = 0,
                                            ),
                                        ),
                                ),
                                CoursePlaceResult(
                                    id = 3L,
                                    placeId = 103L,
                                    orderNo = 3,
                                    caption = "센터커피 성수",
                                    walkingMinutesToNext = 5,
                                    images =
                                        listOf(
                                            CoursePlaceImageResult(
                                                imageUrl =
                                                    "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTMRMGDnfUqzsxQXY1TOrhMtWZ8-otKbsLPlfnIkvDfUw&s=10",
                                                orderNo = 0,
                                            ),
                                        ),
                                ),
                                CoursePlaceResult(
                                    id = 4L,
                                    placeId = 104L,
                                    orderNo = 4,
                                    caption = "카페 할아버지공장",
                                    walkingMinutesToNext = null,
                                    images =
                                        listOf(
                                            CoursePlaceImageResult(
                                                imageUrl =
                                                    "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQr6pSHzsT4DD0ieT5VQ__SVo2ErRODzDyViWmZeXHGlA&s=10",
                                                orderNo = 0,
                                            ),
                                            CoursePlaceImageResult(
                                                imageUrl =
                                                    "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR_3CDZ5UcouOOEkvGYQVI2emgnCGRIzRysaKhwNlq-kw&s=10",
                                                orderNo = 1,
                                            ),
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
