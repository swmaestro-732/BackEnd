package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.request.EndCourseTraceRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 코스 따라가기. **모킹 API** (SCRUM-583)
 *
 * 노션 API 명세서 Course › course-track "따라가기 종료" 기준. 본문이 비어 있어 요청 필드는
 * 디자인(따라가기 4 · 완료 화면 — 방문 스팟 수·소요 시간·이동 거리·마지막 스팟 도착 시각)에서 도출했다.
 * 실제 구현 시 인바운드 포트(UseCase) 연동으로 교체한다. `?mockError=<code>` 주입은 `bootstrap.mock.MockAspect` 가 처리한다.
 */
@RestController
@RequestMapping("/api/v1/courses/{courseId}/trace")
class CourseTraceController {
    /** 따라가기 종료 — 고정 성공, 저장하지 않는다. `/api` 하위는 permitAll 이라 access 토큰을 직접 강제한다. */
    @PostMapping
    @AccessTokenRequired
    fun end(
        @CurrentUserId userId: Long,
        @PathVariable courseId: Long,
        @Valid @RequestBody request: EndCourseTraceRequest,
    ): ApiResponse<Nothing?> = ApiResponse.ok("따라가기를 종료했습니다.")
}
