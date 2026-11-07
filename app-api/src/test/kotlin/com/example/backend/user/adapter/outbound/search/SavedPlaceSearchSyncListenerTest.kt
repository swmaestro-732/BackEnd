package com.example.backend.user.adapter.outbound.search

import com.example.backend.common.geo.Coordinate
import com.example.backend.user.application.event.SavedPlaceIndexedEvent
import com.example.backend.user.application.event.SavedPlaceRemovedEvent
import com.example.backend.user.application.port.inbound.SavedPlaceReindexUseCase
import com.example.backend.user.application.port.outbound.SavedPlaceIndexDocument
import com.example.backend.user.application.port.outbound.SavedPlaceIndexFallbackPort
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
    private var failIndex = false
    private var failDelete = false
    private val port =
        object : SavedPlaceSearchIndexPort {
            override fun index(document: SavedPlaceIndexDocument) {
                if (failIndex) throw RuntimeException("색인 전송 실패")
                indexedDocuments += document
            }

            override fun index(documents: List<SavedPlaceIndexDocument>) {
                indexedDocuments += documents
            }

            override fun delete(
                userId: Long,
                placeId: Long,
            ) {
                if (failDelete) throw RuntimeException("삭제 전송 실패")
                deletedIds += userId to placeId
            }
        }
    private val fallbackIndexed = mutableListOf<SavedPlaceIndexDocument>()
    private val fallbackDeleted = mutableListOf<Pair<Long, Long>>()
    private val fallbackPort =
        object : SavedPlaceIndexFallbackPort {
            override fun enqueueIndex(document: SavedPlaceIndexDocument) {
                fallbackIndexed += document
            }

            override fun enqueueDelete(
                userId: Long,
                placeId: Long,
            ) {
                fallbackDeleted += userId to placeId
            }
        }

    // DB 현재 상태: 여기 있으면 저장된 관계, 없으면 저장 안 함(취소됨)
    private val savedInDb = mutableMapOf<Pair<Long, Long>, SavedPlaceIndexDocument>()
    private val savedPlaceReindexUseCase =
        object : SavedPlaceReindexUseCase {
            override fun reindexAll(): Int = error("동기화는 전체 재색인을 쓰지 않는다")

            override fun findDocument(
                userId: Long,
                placeId: Long,
            ): SavedPlaceIndexDocument? = savedInDb[userId to placeId]
        }
    private val listener = SavedPlaceSearchSyncListener(port, fallbackPort, savedPlaceReindexUseCase)

    @Test
    fun `저장 이벤트는 DB 에서 다시 읽은 현재 저장 관계로 색인한다`() {
        savedInDb[7L to 42L] = document(visited = true)

        listener.onSavedPlaceIndexed(savedEvent()) // 이벤트의 visited=true 스냅샷이 아니라 DB 값을 쓴다

        assertEquals(listOf(document(visited = true)), indexedDocuments)
        assertTrue(deletedIds.isEmpty())
    }

    @Test
    fun `저장 이벤트가 늦게 와도 DB 에서 이미 취소됐으면 색인에서 지운다`() {
        listener.onSavedPlaceIndexed(savedEvent())

        assertTrue(indexedDocuments.isEmpty())
        assertEquals(listOf(7L to 42L), deletedIds)
    }

    @Test
    fun `취소 이벤트는 DB 에 저장이 없으면 사용자와 장소의 복합 키로 삭제한다`() {
        listener.onSavedPlaceRemoved(SavedPlaceRemovedEvent(userId = 7L, placeId = 42L))

        assertEquals(listOf(7L to 42L), deletedIds)
        assertTrue(indexedDocuments.isEmpty())
    }

    @Test
    fun `취소 이벤트가 재저장보다 늦게 와도 DB 에 저장돼 있으면 다시 색인한다`() {
        savedInDb[7L to 42L] = document()

        listener.onSavedPlaceRemoved(SavedPlaceRemovedEvent(userId = 7L, placeId = 42L))

        assertEquals(listOf(document()), indexedDocuments)
        assertTrue(deletedIds.isEmpty())
    }

    @Test
    fun `동기 색인이 실패하면 처리 시점에 읽은 문서를 폴백 큐로 넘긴다`() {
        failIndex = true
        savedInDb[7L to 42L] = document()

        listener.onSavedPlaceRemoved(SavedPlaceRemovedEvent(userId = 7L, placeId = 42L))

        assertTrue(indexedDocuments.isEmpty())
        assertEquals(listOf(document()), fallbackIndexed)
        assertTrue(fallbackDeleted.isEmpty())
    }

    @Test
    fun `동기 삭제가 실패하면 같은 키를 폴백 큐로 넘긴다`() {
        failDelete = true

        listener.onSavedPlaceIndexed(savedEvent())

        assertTrue(deletedIds.isEmpty())
        assertEquals(listOf(7L to 42L), fallbackDeleted)
        assertTrue(fallbackIndexed.isEmpty())
    }

    @Test
    fun `저장과 삭제 이벤트는 트랜잭션 커밋 이후에만 색인 포트에 반영된다`() {
        savedInDb[7L to 42L] = document()
        eventContext().use { context ->
            transactionTemplate().executeWithoutResult {
                context.publishEvent(savedEvent())
                context.publishEvent(SavedPlaceRemovedEvent(7L, 42L))

                assertTrue(indexedDocuments.isEmpty())
                assertTrue(deletedIds.isEmpty())
            }

            // 두 이벤트 모두 DB 현재 상태(저장됨)로 수렴한다
            assertEquals(listOf(42L, 42L), indexedDocuments.map { it.placeId })
            assertTrue(deletedIds.isEmpty())
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
        SavedPlaceIndexedEvent(
            userId = 7L,
            placeId = 42L,
            location = Coordinate(37.51, 127.02),
            savedAt = Instant.parse("2026-10-11T08:00:00Z"),
            visited = true,
            name = "성수 카페",
            category = "CAFE",
            imageUrl = null,
            rating = 4.5,
            ratingCount = 100,
        )

    private fun document(visited: Boolean = false) =
        SavedPlaceIndexDocument(
            userId = 7L,
            placeId = 42L,
            location = Coordinate(37.51, 127.02),
            savedAt = Instant.parse("2026-10-11T08:00:00Z"),
            visited = visited,
            name = "성수 카페",
            category = "CAFE",
            imageUrl = null,
            rating = 4.5,
            ratingCount = 100,
        )

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
