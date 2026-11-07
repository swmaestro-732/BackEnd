package com.example.backend.user.adapter.outbound.search

import com.example.backend.common.geo.Coordinate
import com.example.backend.user.application.event.SavedPlaceIndexedEvent
import com.example.backend.user.application.event.SavedPlaceRemovedEvent
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
    private val listener = SavedPlaceSearchSyncListener(port, fallbackPort)

    @Test
    fun `저장 이벤트의 관계와 좌표 저장 시각을 색인 문서에 싣는다`() {
        listener.onSavedPlaceIndexed(
            SavedPlaceIndexedEvent(
                userId = 7L,
                placeId = 42L,
                location = Coordinate(latitude = 37.51, longitude = 127.02),
                savedAt = Instant.parse("2026-10-11T08:00:00Z"),
                visited = true,
                name = "어니언 성수",
                category = "CAFE",
                imageUrl = "https://img/42.jpg",
                rating = 4.8,
                ratingCount = 212,
            ),
        )

        assertEquals(
            listOf(
                SavedPlaceIndexDocument(
                    userId = 7L,
                    placeId = 42L,
                    location = Coordinate(latitude = 37.51, longitude = 127.02),
                    savedAt = Instant.parse("2026-10-11T08:00:00Z"),
                    visited = true,
                    name = "어니언 성수",
                    category = "CAFE",
                    imageUrl = "https://img/42.jpg",
                    rating = 4.8,
                    ratingCount = 212,
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
    fun `동기 색인이 실패하면 같은 문서를 폴백 큐로 넘긴다`() {
        failIndex = true
        val event = savedEvent()

        listener.onSavedPlaceIndexed(event)

        assertTrue(indexedDocuments.isEmpty())
        assertEquals(
            listOf(
                SavedPlaceIndexDocument(
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
                ),
            ),
            fallbackIndexed,
        )
    }

    @Test
    fun `동기 삭제가 실패하면 같은 키를 폴백 큐로 넘긴다`() {
        failDelete = true

        listener.onSavedPlaceRemoved(SavedPlaceRemovedEvent(userId = 7L, placeId = 42L))

        assertTrue(deletedIds.isEmpty())
        assertEquals(listOf(7L to 42L), fallbackDeleted)
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
