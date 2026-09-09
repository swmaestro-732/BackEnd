package com.example.backend.course.application.service

import com.example.backend.course.application.port.inbound.CourseSearchCommand
import com.example.backend.course.application.port.inbound.CourseSearchResult
import com.example.backend.course.application.port.inbound.CourseSearchUseCase
import com.example.backend.course.application.port.outbound.CourseSearchCriteria
import com.example.backend.course.application.port.outbound.CourseSearchQueryPort
import org.springframework.stereotype.Service

/**
 * 코스 검색 서비스 — 자신의 아웃바운드 검색 포트([CourseSearchQueryPort])만 호출한다.
 * 빈 문자열 파라미터를 필터 미적용(null)으로 정규화하고, 커서는 불투명 문자열이라 그대로 넘기고 그대로 받는다
 * (커서 해석·다음 커서 생성은 검색 어댑터 몫). DB 를 만지지 않아 트랜잭션이 필요 없다.
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
        return CourseSearchResult(courses = page.hits, nextCursor = page.nextCursor, hasNext = page.hasNext)
    }
}
