package com.example.backend.course.adapter.inbound.web

import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.response.CourseSearchResponse
import com.example.backend.course.application.port.inbound.CourseSearchUseCase
import com.example.backend.course.application.port.inbound.dto.CourseSearchCommand
import com.example.backend.course.application.port.inbound.dto.CourseSearchSort
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 공개 코스 검색(`GET /api/v1/courses/search`).
 *
 * 발행된 PUBLIC 코스를 키워드(q)·필터(area/category/tags)·정렬(sort)로 찾아 커서 페이지로 내려준다.
 * `/search` 는 [CourseController] 의 `/{courseId}` 보다 구체적 경로라 매핑이 겹치지 않는다(place 와 동일 분리).
 */
@Tag(name = "Course")
@RestController
@RequestMapping("/api/v1/courses")
class CourseSearchController(
    private val courseSearchUseCase: CourseSearchUseCase,
) {
    @GetMapping("/search")
    fun search(
        @RequestParam
        @NotBlank(message = "검색어(keyword)는 필수입니다")
        keyword: String,
        @RequestParam(required = false) area: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) tags: List<String> = emptyList(),
        @RequestParam(required = false) sort: CourseSearchSort = CourseSearchSort.RELEVANCE,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false)
        @Min(1, message = "1 이상이어야 합니다")
        @Max(50, message = "50 이하여야 합니다")
        size: Int = 20,
    ): ApiResponse<CourseSearchResponse> {
        val result =
            courseSearchUseCase.search(
                CourseSearchCommand(
                    keyword = keyword,
                    area = area,
                    category = category,
                    tags = tags,
                    sort = sort,
                    cursor = cursor,
                    size = size,
                ),
            )
        return ApiResponse.success(CourseSearchResponse.from(result))
    }
}
