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
