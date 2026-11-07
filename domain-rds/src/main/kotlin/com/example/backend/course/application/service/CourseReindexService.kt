package com.example.backend.course.application.service

import com.example.backend.course.application.port.inbound.CourseReindexUseCase
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CourseSearchIndexPort
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

@Service
@Transactional(readOnly = true)
class CourseReindexService(
    private val coursePersistencePort: CoursePersistencePort,
    private val courseSearchIndexPort: CourseSearchIndexPort,
    transactionManager: PlatformTransactionManager,
) : CourseReindexUseCase {
    // 커밋 후 리스너에서 불리면 끝난 트랜잭션이 아직 묶여 있으므로 합류하지 않고 새로 연다.
    private val readOnlyTx =
        TransactionTemplate(transactionManager).apply {
            isReadOnly = true
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }

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
     * 커밋 후(AFTER_COMMIT) 리스너가 요청 스레드에서 부른다. DB 는 짧은 읽기 전용 트랜잭션에서만 읽고,
     * 검색 색인 호출은 트랜잭션 밖에서 해 OpenSearch 응답을 기다리는 동안 DB 연결을 잡지 않는다.
     * 색인 자체는 어댑터가 fail-soft.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    override fun reindex(courseId: Long) {
        val found = readOnlyTx.execute { _ -> coursePersistencePort.findForIndexByIds(listOf(courseId)) }.orEmpty()
        if (found.isNotEmpty()) courseSearchIndexPort.save(found)
    }

    private companion object {
        // 한 번의 bulk 요청이 클라이언트 응답 대기(2초) 안에 끝나도록 작게 잡는다. 500건은 dev(SigV4, nori 분석)에서 Read timed out 이 났다.
        const val PAGE = 100
    }
}
