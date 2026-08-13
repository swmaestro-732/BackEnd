package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.course.application.port.inbound.CourseCounterUseCase
import com.example.backend.course.application.port.inbound.CourseUseCase
import com.example.backend.user.application.port.outbound.CourseCleanupPort
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [CourseCleanupPort] 를 course 인바운드 포트에 위임한다(인프로세스).
 * user 애플리케이션이 course 를 직접 알지 않게 하는 경계 지점. MSA 분리 시 이 어댑터만 교체한다.
 */
@Component
class CourseCleanupAdapter(
    private val courseUseCase: CourseUseCase,
    private val courseCounterUseCase: CourseCounterUseCase,
) : CourseCleanupPort {
    override fun softDeleteCoursesByAuthor(authorId: Long) = courseUseCase.deleteAllByAuthor(authorId)

    override fun decreaseSavesCounts(courseIds: List<Long>) =
        courseIds.forEach(courseCounterUseCase::decreaseSavesCount)
}
