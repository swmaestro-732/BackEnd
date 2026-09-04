package com.example.backend.course.application.service

import com.example.backend.course.application.port.inbound.CourseSearchCommand
import com.example.backend.course.application.port.inbound.CourseSearchResult
import com.example.backend.course.application.port.inbound.CourseSearchUseCase
import com.example.backend.course.application.port.outbound.CourseSearchCriteria
import com.example.backend.course.application.port.outbound.CourseSearchQueryPort
import org.springframework.stereotype.Service

/**
 * 코스 검색 서비스 — 자신의 아웃바운드 검색 포트([CourseSearchQueryPort])만 호출한다.
 * 외부 문자열 커서의 디코딩과 다음 커서 인코딩([CourseSearchCursorCodec])을 담당하고,
 * 빈 문자열 파라미터는 필터 미적용(null)으로 정규화한다. DB 를 만지지 않아 트랜잭션이 필요 없다.
 */
@Service
class CourseSearchService(
    private val courseSearchQueryPort: CourseSearchQueryPort,
) : CourseSearchUseCase {
    override fun search(command: CourseSearchCommand): CourseSearchResult {
        val criteria =
            CourseSearchCriteria(
                keyword = command.keyword?.takeIf { it.isNotBlank() },
                area = command.area?.takeIf { it.isNotBlank() },
                category = command.category?.takeIf { it.isNotBlank() },
                tags = command.tags.map { it.trim() }.filter { it.isNotBlank() },
                sort = command.sort,
                searchAfter = CourseSearchCursorCodec.decode(command.sort, command.cursor),
                size = command.size,
            )
        val page = courseSearchQueryPort.search(criteria)
        val nextCursor =
            if (page.hasNext && page.hits.isNotEmpty()) {
                CourseSearchCursorCodec.encode(command.sort, page.hits.last())
            } else {
                null
            }
        return CourseSearchResult(courses = page.hits, nextCursor = nextCursor, hasNext = page.hasNext)
    }
}
