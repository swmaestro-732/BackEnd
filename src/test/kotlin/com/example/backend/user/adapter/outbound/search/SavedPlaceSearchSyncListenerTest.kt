package com.example.backend.user.adapter.outbound.search

import com.example.backend.common.geo.Coordinate
import com.example.backend.user.application.event.SavedPlaceIndexedEvent
import com.example.backend.user.application.event.SavedPlaceRemovedEvent
import com.example.backend.user.application.port.outbound.SavedPlaceIndexDocument
import com.example.backend.user.application.port.outbound.SavedPlaceSearchIndexPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.event.TransactionalEventListenerFactory
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.function.Supplier

class SavedPlaceSearchSyncListenerTest {
    private val indexedDocuments = mutableListOf<SavedPlaceIndexDocument>()
    private val deletedIds = mutableListOf<Pair<Long, Long>>()
    private val port =
        object : SavedPlaceSearchIndexPort {
            override fun index(document: SavedPlaceIndexDocument) {
                indexedDocuments += document
            }

            override fun index(documents: List<SavedPlaceIndexDocument>) {
                indexedDocuments += documents
            }

            override fun delete(
                userId: Long,
                placeId: Long,
            ) {
                deletedIds += userId to placeId
            }
        }
    private val listener = SavedPlaceSearchSyncListener(port)

    @Test
    fun `저장 이벤트의 관계와 좌표 방문 여부 저장 시각을 색인 문서에 싣는다`() {
        listener.onSavedPlaceIndexed(
            SavedPlaceIndexedEvent(
                userId = 7L,
                placeId = 42L,
                location = Coordinate(latitude = 37.51, longitude = 127.02),
                visited = true,
                savedAt = Instant.parse("2026-10-11T08:00:00Z"),
            ),
        )

        assertEquals(
            listOf(
                SavedPlaceIndexDocument(
                    userId = 7L,
                    placeId = 42L,
                    location = Coordinate(latitude = 37.51, longitude = 127.02),
                    visited = true,
                    savedAt = Instant.parse("2026-10-11T08:00:00Z"),
                ),
            ),
            indexedDocuments,
        )
        assertTrue(deletedIds.isEmpty())
    }

    @Test
    fun `저장 취소 이벤트는 사용자와 장소의 복합 키로 삭제한다`() {
        listener.onSavedPlaceRemoved(SavedPlaceRemovedEvent(userId = 7L, placeId = 42L))

        assertEquals(listOf(7L to 42L), deletedIds)
        assertTrue(indexedDocuments.isEmpty())
    }

    @Test
    fun `저장과 삭제 이벤트는 트랜잭션 커밋 이후에만 색인 포트에 반영된다`() {
        eventContext().use { context ->
            transactionTemplate().executeWithoutResult {
                context.publishEvent(savedEvent())
                context.publishEvent(SavedPlaceRemovedEvent(7L, 42L))

                assertTrue(indexedDocuments.isEmpty())
                assertTrue(deletedIds.isEmpty())
            }

            assertEquals(listOf(42L), indexedDocuments.map { it.placeId })
            assertEquals(listOf(7L to 42L), deletedIds)
        }
    }

    @Test
    fun `트랜잭션이 롤백되면 저장과 삭제 이벤트 모두 색인하지 않는다`() {
        eventContext().use { context ->
            transactionTemplate().executeWithoutResult { status ->
                context.publishEvent(savedEvent())
                context.publishEvent(SavedPlaceRemovedEvent(7L, 42L))
                status.setRollbackOnly()
            }

            assertTrue(indexedDocuments.isEmpty())
            assertTrue(deletedIds.isEmpty())
        }
    }

    @Test
    fun `트랜잭션 밖에서 발행된 이벤트는 색인하지 않는다`() {
        eventContext().use { context ->
            context.publishEvent(savedEvent())
            context.publishEvent(SavedPlaceRemovedEvent(7L, 42L))

            assertTrue(indexedDocuments.isEmpty())
            assertTrue(deletedIds.isEmpty())
        }
    }

    private fun eventContext() =
        AnnotationConfigApplicationContext().apply {
            registerBean(
                TransactionalEventListenerFactory::class.java,
                Supplier { TransactionalEventListenerFactory() },
            )
            registerBean(SavedPlaceSearchSyncListener::class.java, Supplier { listener })
            refresh()
        }

    private fun savedEvent() =
        SavedPlaceIndexedEvent(7L, 42L, Coordinate(37.51, 127.02), false, Instant.parse("2026-10-11T08:00:00Z"))

    private fun transactionTemplate(): TransactionTemplate =
        TransactionTemplate(
            object : AbstractPlatformTransactionManager() {
                override fun doGetTransaction(): Any = Any()

                override fun doBegin(
                    transaction: Any,
                    definition: TransactionDefinition,
                ) = Unit

                override fun doCommit(status: DefaultTransactionStatus) = Unit

                override fun doRollback(status: DefaultTransactionStatus) = Unit
            },
        )
}
