package com.example.backend.course.application.service

import com.example.backend.course.application.port.inbound.CourseReindexUseCase
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CourseSearchIndexPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
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

    /**
     * 커밋 후(AFTER_COMMIT) 리스너가 요청 스레드에서 부른다. 끝난 트랜잭션에 합류하지 않도록 새 트랜잭션으로 읽는다.
     * 색인 자체는 어댑터가 fail-soft.
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    override fun reindex(courseId: Long) {
        val found = coursePersistencePort.findForIndexByIds(listOf(courseId))
        if (found.isNotEmpty()) courseSearchIndexPort.save(found)
    }

    private companion object {
        // 한 번의 bulk 요청이 클라이언트 응답 대기(2초) 안에 끝나도록 작게 잡는다. 500건은 dev(SigV4, nori 분석)에서 Read timed out 이 났다.
        const val PAGE = 100
    }
}
