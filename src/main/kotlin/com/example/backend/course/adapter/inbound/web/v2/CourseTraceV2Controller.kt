package com.example.backend.course.adapter.inbound.web.v2

import com.example.backend.bootstrap.appversion.AppFeature
import com.example.backend.bootstrap.appversion.RequiresAppFeature
import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.v2.request.StartCourseTraceRequest
import com.example.backend.course.adapter.inbound.web.v2.response.CourseTraceV2Response
import com.example.backend.course.application.port.inbound.CourseTraceUseCase
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 코스 따라가기 V2. 시작할 때 따라갈 장소 목록을 받아 코스 루트를 수정해 따라갈 수 있다.
 * 체크인·종료·루트 변경은 V1([com.example.backend.course.adapter.inbound.web.CourseTraceController])을 그대로 쓴다.
 */
@RequiresAppFeature(AppFeature.COURSE_TRACING)
@RestController
class CourseTraceV2Controller(
    private val courseTraceUseCase: CourseTraceUseCase,
) {
    /** 따라가기 시작 V2 — 따라갈 장소(`placeIds`, 순서대로)를 받는다. 이어가기·409 는 V1 과 같고, 이어가기면 기존 세션의 루트를 돌려준다. */
    @PostMapping("/api/v2/courses/{courseId}/traces")
    @AccessTokenRequired
    fun startWithRoute(
        @CurrentUserId userId: Long,
        @PathVariable courseId: Long,
        @Valid @RequestBody request: StartCourseTraceRequest,
    ): ApiResponse<CourseTraceV2Response> =
        ApiResponse.success(
            CourseTraceV2Response.from(courseTraceUseCase.startWithRoute(userId, courseId, request.placeIds)),
            "따라가기를 시작했습니다.",
        )
}
