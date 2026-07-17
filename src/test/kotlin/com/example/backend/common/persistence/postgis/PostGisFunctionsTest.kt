package com.example.backend.common.persistence.postgis

import com.example.backend.place.adapter.outbound.persistence.PlaceTable
import com.example.backend.place.domain.model.PlaceBusinessStatus
import com.example.backend.place.domain.model.PlaceStatus
import com.example.backend.support.IntegrationTestBase
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PostGisFunctionsTest : IntegrationTestBase() {
    @Test
    fun `ST_Y와 ST_X로 위도와 경도를 조회한다`() {
        transaction {
            val point = GeoPoint(latitude = 37.544575, longitude = 127.055969)
            val placeId = insertPlace("좌표 함수 테스트 장소", point)
            val latitude = PlaceTable.location.stY()
            val longitude = PlaceTable.location.stX()

            val row =
                PlaceTable
                    .select(latitude, longitude)
                    .where { PlaceTable.id eq placeId }
                    .single()

            assertEquals(point.latitude, row[latitude], COORDINATE_TOLERANCE)
            assertEquals(point.longitude, row[longitude], COORDINATE_TOLERANCE)
            rollback()
        }
    }

    @Test
    fun `geography Point를 GeoPoint로 왕복한다`() {
        transaction {
            val point = GeoPoint(latitude = 37.546061, longitude = 127.049256)
            val placeId = insertPlace("좌표 왕복 테스트 장소", point)

            val actual =
                PlaceTable
                    .select(PlaceTable.location)
                    .where { PlaceTable.id eq placeId }
                    .single()[PlaceTable.location]

            assertEquals(point, actual)
            rollback()
        }
    }

    @Test
    fun `ST_DWithin은 반경 안의 장소만 조회한다`() {
        transaction {
            val reference = makePoint(latitude = 37.544575, longitude = 127.055969)
            val nearPlaceId =
                insertPlace(
                    name = "반경 내 테스트 장소",
                    point = GeoPoint(latitude = 37.544612, longitude = 127.056065),
                )
            val farPlaceId =
                insertPlace(
                    name = "반경 밖 테스트 장소",
                    point = GeoPoint(latitude = 37.566535, longitude = 126.977969),
                )

            val matchedPlaceIds =
                PlaceTable
                    .select(PlaceTable.id)
                    .where { PlaceTable.location.stDWithin(reference, meters = 100.0) }
                    .map { it[PlaceTable.id] }

            assertTrue(nearPlaceId in matchedPlaceIds)
            assertFalse(farPlaceId in matchedPlaceIds)
            rollback()
        }
    }

    private fun insertPlace(
        name: String,
        point: GeoPoint,
    ): Long =
        PlaceTable.insert {
            it[status] = PlaceStatus.ACTIVE
            it[PlaceTable.name] = name
            it[category] = "CAFE"
            it[location] = point
            it[address] = "서울특별시 성동구 성수동"
            it[businessStatus] = PlaceBusinessStatus.OPEN
        }[PlaceTable.id]

    private companion object {
        const val COORDINATE_TOLERANCE = 0.000001
    }
}
