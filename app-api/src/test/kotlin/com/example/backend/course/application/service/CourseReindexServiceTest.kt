package com.example.backend.course.application.service

import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CourseSearchIndexPort
import com.example.backend.course.domain.model.Course
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus

class CourseReindexServiceTest {
    private val persistence = mock(CoursePersistencePort::class.java)
    private val index = mock(CourseSearchIndexPort::class.java)
    private val transactionManager = TrackingTransactionManager()
    private val service = CourseReindexService(persistence, index, transactionManager)

    @Test
    fun `코스가 없으면 색인하지 않는다`() {
        `when`(persistence.findForIndex(null, 100)).thenReturn(emptyList())

        assertEquals(0, service.reindexAll())
        verifyNoInteractions(index)
    }

    @Test
    fun `페이지 마지막 id를 커서로 사용해 전부 색인한다`() {
        val first = (1L..100L).map(::course)
        val second = listOf(course(101L), course(102L))
        `when`(persistence.findForIndex(null, 100)).thenReturn(first)
        `when`(persistence.findForIndex(100L, 100)).thenReturn(second)

        assertEquals(102, service.reindexAll())
        verify(index).save(first)
        verify(index).save(second)
    }

    @Test
    fun `단건 재색인은 읽기 트랜잭션에서 읽고 트랜잭션이 끝난 뒤 색인한다`() {
        val found = listOf(course(7L))
        `when`(persistence.findForIndexByIds(listOf(7L))).thenAnswer {
            assertTrue(transactionManager.active) { "DB 읽기는 트랜잭션 안이어야 한다" }
            found
        }
        doAnswer {
            assertFalse(transactionManager.active) { "색인 호출 동안 트랜잭션(DB 연결)을 잡으면 안 된다" }
        }.`when`(index).save(found)

        service.reindex(7L)

        verify(index).save(found)
        assertTrue(transactionManager.readOnly)
    }

    @Test
    fun `단건 재색인 대상이 없으면 색인하지 않는다`() {
        `when`(persistence.findForIndexByIds(listOf(7L))).thenReturn(emptyList())

        service.reindex(7L)

        verifyNoInteractions(index)
    }

    /** 트랜잭션이 열려 있는지와 읽기 전용 여부를 기록하는 가짜 트랜잭션 관리자. */
    private class TrackingTransactionManager : PlatformTransactionManager {
        var active = false
        var readOnly = false

        override fun getTransaction(definition: TransactionDefinition?): TransactionStatus {
            active = true
            readOnly = definition?.isReadOnly ?: false
            return SimpleTransactionStatus()
        }

        override fun commit(status: TransactionStatus) {
            active = false
        }

        override fun rollback(status: TransactionStatus) {
            active = false
        }
    }

    private fun course(id: Long): Course =
        mock(Course::class.java).also {
            `when`(it.id).thenReturn(id)
        }
}
