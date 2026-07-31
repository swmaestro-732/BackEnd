package com.example.backend.mobile.course.adapter.inbound.web

import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.mobile.course.adapter.inbound.web.response.CourseDetailScreenResponse
import com.example.backend.mobile.course.application.port.inbound.CourseMobileUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 코스 상세 **화면 조합 API** (BFF).
 * 코스 상세 + 작성자 프로필 + 장소 이름·카테고리를 한 번에 내려준다.
 * 개별 도메인 API(`/api/v1/...`)와 구분해 화면 조합 경로(`/service/v1/...`)로 노출한다.
 *
 * 조합(도메인 인바운드 포트 3개 호출)은 인바운드 포트([CourseMobileUseCase])가 담당하고,
 * 컨트롤러는 Request → 포트 호출 → Response 매핑만 한다. 코스 존재/공개범위 판정은 코스 도메인이 수행한다
 * (없음·비공개는 404 COURSE_NOT_FOUND). `reviewSummary` 는 리뷰 조회 유스케이스 도입 전까지 실 응답에서 null 이다(목 값 노출 금지).
 *
 * 시드/DB 없이 프론트가 붙어볼 수 있도록 `?mock=true` 면 조회 없이 고정 목([CourseDetailScreenResponse.MOCK])을 반환한다.
 * 모킹 에러(`?mockError=<code>`)는 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 */
@RestController
@RequestMapping("/service/v1")
class CourseMobileController(
    private val courseMobileUseCase: CourseMobileUseCase,
) {
    @GetMapping("/courses/{courseId}")
    fun getScreen(
        @PathVariable courseId: Long,
        @CurrentUserId viewerId: Long?,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<CourseDetailScreenResponse> {
        if (mock) return ApiResponse.success(CourseDetailScreenResponse.MOCK)

        return ApiResponse.success(
            CourseDetailScreenResponse.from(
                courseMobileUseCase.getScreen(courseId, viewerId),
            ),
        )
    }
}
