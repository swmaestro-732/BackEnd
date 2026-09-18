package com.example.backend.place.adapter.outbound.persistence

import com.example.backend.common.geo.Coordinate
import com.example.backend.common.geo.Viewport
import com.example.backend.place.adapter.outbound.persistence.exposed.PlaceTable
import com.example.backend.place.adapter.outbound.persistence.exposed.repository.PlaceMapRepository
import com.example.backend.place.adapter.outbound.persistence.exposed.repository.PlaceRepository
import com.example.backend.place.application.port.outbound.PlaceSearchCriteria
import com.example.backend.place.domain.model.PlaceBusinessStatus
import com.example.backend.place.domain.model.PlaceCategory
import com.example.backend.place.domain.model.PlaceStatus
import com.example.backend.support.IntegrationTestBase
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.time.Clock

class PlaceSearchPersistenceTest
    @Autowired
    constructor(
        private val mapRepository: PlaceMapRepository,
        private val placeRepository: PlaceRepository,
    ) : IntegrationTestBase() {
        private val viewport = Viewport(Coordinate(37.0, 126.0), Coordinate(38.0, 128.0))

        @Test
        fun `지도 경계는 곡선이 아닌 위경도 사각형으로 안쪽과 경계 점만 포함한다`() {
            transaction {
                try {
                    val query = "지도경계-${UUID.randomUUID()}"
                    val inside = insertPlace(query, Coordinate(37.001, 127.0))
                    val southWest = insertPlace(query, viewport.southWest)
                    val northEast = insertPlace(query, viewport.northEast)
                    insertPlace(query, Coordinate(38.001, 127.0))
                    insertPlace(query, Coordinate(36.999, 127.0))

                    val hits = mapRepository.search(criteria(query), precision = 5)

                    assertEquals(3L, hits.totalCount)
                    assertEquals(setOf(inside, southWest, northEast), hits.ids.toSet())
                    assertEquals(3L, hits.buckets.sumOf { it.count })
                } finally {
                    rollback()
                }
            }
        }

        @Test
        fun `지도는 모든 필터를 적용한 101건을 개수 제한 없이 그룹으로 집계한다`() {
            transaction {
                try {
                    val query = "지도집계-${UUID.randomUUID()}"
                    repeat(60) { insertPlace(query, Coordinate(37.200123, 126.500456)) }
                    repeat(40) { insertPlace(query, Coordinate(37.6, 127.2)) }
                    val singleton = insertPlace(query, Coordinate(37.8, 127.8))
                    insertPlace(query, Coordinate(37.2, 126.5), category = PlaceCategory.RESTAURANT)
                    insertPlace(query, Coordinate(37.2, 126.5), areaCode = "2611010100")
                    insertPlace(query, Coordinate(38.1, 127.0))
                    insertPlace(query, Coordinate(37.2, 126.5), status = PlaceStatus.HIDDEN)
                    insertPlace(query, Coordinate(37.2, 126.5), deleted = true)
                    insertPlace("불일치-${UUID.randomUUID()}", Coordinate(37.2, 126.5))

                    val hits = mapRepository.search(criteria(query), precision = 7)

                    assertEquals(101L, hits.totalCount)
                    assertTrue(hits.ids.isEmpty())
                    assertEquals(listOf(1L, 40L, 60L), hits.buckets.map { it.count }.sorted())
                    assertEquals(101L, hits.buckets.sumOf { it.count })
                    assertEquals(Coordinate(37.200123, 126.500456), hits.buckets.single { it.count == 60L }.center)
                    assertEquals(singleton, hits.buckets.single { it.count == 1L }.singlePlaceId)
                    assertEquals(Coordinate(37.8, 127.8), hits.buckets.single { it.count == 1L }.center)
                } finally {
                    rollback()
                }
            }
        }

        @Test
        fun `목록은 정확한 이름을 우선하고 나머지는 거리순으로 원거리까지 중복 없이 페이지 조회한다`() {
            transaction {
                try {
                    val query = "거리정렬-${UUID.randomUUID()}"
                    val anchor = Coordinate(37.5, 127.0)
                    val far = insertPlace("$query 제주점", Coordinate(33.5, 126.5))
                    val middle = insertPlace("$query 경기점", Coordinate(37.0, 127.0))
                    val near = insertPlace("$query 서울점", Coordinate(37.5001, 127.0))
                    val exact = insertPlace(query, Coordinate(35.1, 129.0))
                    insertPlace(query, anchor, deleted = true)
                    insertPlace("불일치-${UUID.randomUUID()}", anchor)

                    val first = placeRepository.searchNearbyByName(query, anchor, offset = 0, limit = 2)
                    val second = placeRepository.searchNearbyByName(query, anchor, offset = 2, limit = 2)
                    val end = placeRepository.searchNearbyByName(query, anchor, offset = 4, limit = 2)

                    assertEquals(listOf(exact, near), first.map { it.id.value })
                    assertEquals(listOf(middle, far), second.map { it.id.value })
                    assertTrue(end.isEmpty())
                    assertEquals(4L, placeRepository.countByName(query))
                } finally {
                    rollback()
                }
            }
        }

        private fun criteria(query: String): PlaceSearchCriteria =
            PlaceSearchCriteria(
                textTokens = listOf(query),
                categories = listOf(PlaceCategory.CAFE),
                areaCodePrefixes = listOf("11"),
                viewport = viewport,
                from = 0,
                size = 100,
            )

        private fun insertPlace(
            name: String,
            point: Coordinate,
            category: PlaceCategory = PlaceCategory.CAFE,
            areaCode: String = "1168010100",
            status: PlaceStatus = PlaceStatus.ACTIVE,
            deleted: Boolean = false,
        ): Long =
            PlaceTable
                .insert {
                    it[PlaceTable.status] = status
                    it[PlaceTable.name] = name
                    it[PlaceTable.category] = category
                    it[location] = point
                    it[address] = "검색 회귀 테스트 주소"
                    it[PlaceTable.areaCode] = areaCode
                    it[businessStatus] = PlaceBusinessStatus.OPEN
                    if (deleted) it[deletedAt] = Clock.System.now()
                }[PlaceTable.id]
                .value
    }
