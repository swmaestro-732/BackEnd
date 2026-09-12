package com.example.backend.course.application.service

import com.example.backend.course.application.port.inbound.CourseSearchUseCase
import com.example.backend.course.application.port.inbound.dto.CourseSearchCommand
import com.example.backend.course.application.port.inbound.dto.CourseSearchResult
import com.example.backend.course.application.port.inbound.dto.CourseSummary
import com.example.backend.course.application.port.outbound.CourseSearchCriteria
import com.example.backend.course.application.port.outbound.CourseSearchQueryPort
import com.example.backend.course.application.port.outbound.CourseSearchRow
import org.springframework.stereotype.Service

/**
 * 코스 검색 서비스 — 자신의 아웃바운드 검색 포트([CourseSearchQueryPort])만 호출한다.
 * 빈 문자열 파라미터를 필터 미적용(null)으로 정규화하고 커서는 불투명 문자열이라 그대로 넘기고 그대로 받는다
 * (커서 해석·다음 커서 생성은 검색 어댑터 몫). DB 를 만지지 않아 트랜잭션이 필요 없다.
 * 검색은 키워드로 찾는 기능이라 keyword 필수·공백 검증은 컨트롤러(@NotBlank)에서 한다. area·category·tags 는 그 위에 얹는 선택 필터다.
 */
@Service
class CourseSearchService(
    private val courseSearchQueryPort: CourseSearchQueryPort,
) : CourseSearchUseCase {
    override fun search(command: CourseSearchCommand): CourseSearchResult {
        val criteria =
            CourseSearchCriteria(
                keyword = command.keyword?.takeIf { it.isNotBlank() },
                // area·category 는 어댑터가 term(정확 일치) 필터로 쓰므로 색인 값과 맞도록 trim 한다(공백 있으면 0건).
                area = command.area?.trim()?.takeIf { it.isNotEmpty() },
                category = command.category?.trim()?.takeIf { it.isNotEmpty() },
                tags = command.tags.map { it.trim() }.filter { it.isNotBlank() },
                sort = command.sort,
                cursor = command.cursor,
                size = command.size,
            )
        val page = courseSearchQueryPort.search(criteria)
        return CourseSearchResult(
            courses = page.hits.map(::toCourseSummary),
            nextCursor = page.nextCursor,
            hasNext = page.hasNext,
        )
    }

    /** 아웃바운드 검색 행을 인바운드 계약(CourseSummary)으로 옮긴다(영속성 경로의 toCourseSummary 와 동형). */
    private fun toCourseSummary(row: CourseSearchRow) =
        CourseSummary(
            id = row.id,
            authorId = row.authorId,
            title = row.title,
            coverImageUrl = row.coverImageUrl,
            theme = row.theme,
            area = row.area,
            likesCnt = row.likesCnt,
            savesCnt = row.savesCnt,
            createdAt = row.createdAt,
        )
}
