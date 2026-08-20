package com.example.backend.course.application.service

import com.example.backend.course.application.port.inbound.CourseReindexUseCase
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CourseSearchIndexPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CourseReindexService(
    private val coursePersistencePort: CoursePersistencePort,
    private val courseSearchIndexPort: CourseSearchIndexPort,
) : CourseReindexUseCase {
    /** 전체 코스를 페이지 단위로 재색인(멱등 upsert). 색인 자체는 어댑터가 fail-soft. */
    override fun reindexAll(): Int {
        var afterId: Long? = null
        var total = 0
        while (true) {
            val batch = coursePersistencePort.findForIndex(afterId, PAGE)
            if (batch.isEmpty()) break
            courseSearchIndexPort.save(batch)
            total += batch.size
            afterId = batch.last().id!!
            if (batch.size < PAGE) break
        }
        return total
    }

    private companion object {
        const val PAGE = 500
    }
}
