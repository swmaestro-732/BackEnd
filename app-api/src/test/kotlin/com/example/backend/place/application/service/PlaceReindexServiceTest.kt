package com.example.backend.place.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.port.outbound.PlacePersistencePort
import com.example.backend.place.application.port.outbound.PlaceSearchIndexPort
import com.example.backend.place.domain.model.Place
import com.example.backend.place.domain.model.PlaceBusinessStatus
import com.example.backend.place.domain.model.PlaceCategory
import com.example.backend.place.domain.model.PlaceStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus

class PlaceReindexServiceTest {
    private fun place(id: Long): Place =
        Place.reconstitute(
            id = id,
            status = PlaceStatus.ACTIVE,
            name = "장소 $id",
            description = null,
            category = PlaceCategory.CAFE,
            location = Coordinate(latitude = 37.5, longitude = 127.0),
            address = "a",
            areaCode = null,
            imageUrl = null,
            businessStatus = PlaceBusinessStatus.UNKNOWN,
            kakaoPlaceId = null,
            createdAt = null,
            updatedAt = null,
            deletedAt = null,
        )

    @Test
    fun `전체 장소를 페이지 단위로 한 번씩 색인하고 색인 수를 반환한다`() {
        val n = 1200
        val all = (1L..n).map { place(it) }

        var pageCalls = 0
        val fakePersistence =
            object : PlacePersistencePort {
                override fun findByKakaoIds(kakaoIds: List<String>): List<Place> = emptyList()

                override fun insertIgnoringConflicts(places: List<Place>) = Unit

                override fun findForIndex(
                    afterId: Long?,
                    limit: Int,
                ): List<Place> {
                    pageCalls++
                    return all.filter { it.id!! > (afterId ?: 0L) }.take(limit)
                }

                override fun findForIndexByIds(ids: List<Long>): List<Place> = all.filter { it.id in ids }
            }

        val indexedIds = mutableListOf<Long>()
        val fakeSearchIndex =
            object : PlaceSearchIndexPort {
                override fun save(places: List<Place>) {
                    indexedIds += places.map { it.id!! }
                }
            }

        val total = PlaceReindexService(fakePersistence, fakeSearchIndex, TrackingTransactionManager()).reindexAll()

        assertEquals(n, total) { "색인 수 불일치" }
        assertEquals(all.map { it.id }, indexedIds) { "모든 id 를 정확히 한 번씩 색인하지 않음" }
        assertEquals(indexedIds.size, indexedIds.toSet().size) { "중복 색인 발생" }
        assertTrue(pageCalls > 2) { "페이징이 2페이지 초과로 돌지 않음: $pageCalls" }
    }

    @Test
    fun `단건 재색인은 읽기 트랜잭션에서 읽고 트랜잭션이 끝난 뒤 색인한다`() {
        val transactionManager = TrackingTransactionManager()
        val fakePersistence =
            object : PlacePersistencePort {
                override fun findByKakaoIds(kakaoIds: List<String>): List<Place> = emptyList()

                override fun insertIgnoringConflicts(places: List<Place>) = Unit

                override fun findForIndex(
                    afterId: Long?,
                    limit: Int,
                ): List<Place> = emptyList()

                override fun findForIndexByIds(ids: List<Long>): List<Place> {
                    assertTrue(transactionManager.active) { "DB 읽기는 트랜잭션 안이어야 한다" }
                    return ids.map { place(it) }
                }
            }
        val indexedIds = mutableListOf<Long>()
        val fakeSearchIndex =
            object : PlaceSearchIndexPort {
                override fun save(places: List<Place>) {
                    assertFalse(transactionManager.active) { "색인 호출 동안 트랜잭션(DB 연결)을 잡으면 안 된다" }
                    indexedIds += places.map { it.id!! }
                }
            }

        PlaceReindexService(fakePersistence, fakeSearchIndex, transactionManager).reindex(listOf(1L, 2L))

        assertEquals(listOf(1L, 2L), indexedIds)
        assertTrue(transactionManager.readOnly)
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
}
