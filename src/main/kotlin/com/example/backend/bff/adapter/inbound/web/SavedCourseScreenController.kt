package com.example.backend.bff.adapter.inbound.web

import com.example.backend.bff.adapter.inbound.web.response.SavedCourseScreenResponse
import com.example.backend.common.response.ApiResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 저장함 · 코스 탭 **화면 조합 목업 API** (BFF) — `GET /service/v1/my/saved-courses`.
 * 모킹 에러(`?mockError=<code>`)는 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 */
@RestController
@RequestMapping("/service/v1")
class SavedCourseScreenController {
    /**
     * - folderId: 폴더 칩 필터. 생략 시 전체.
     * - completed: 완주 여부 필터(안 가봄/완주 칩).
     * - cursor: 직전 응답의 nextCursor(첫 페이지는 생략). 응답은 `nextCursor=null`/`hasNext=false` 고정.
     * - size: 페이지 크기(기본 10, 1~50). 범위를 벗어나면 400.
     */
    @GetMapping("/my/saved-courses")
    fun getScreen(
        @RequestParam(required = false) folderId: Long?,
        @RequestParam(required = false) completed: Boolean = false,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
    ): ApiResponse<SavedCourseScreenResponse> = ApiResponse.success(SavedCourseScreenResponse.mock())
}
