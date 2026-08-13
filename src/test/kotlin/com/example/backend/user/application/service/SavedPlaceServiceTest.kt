package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.user.application.port.inbound.dto.SavedPlacesCommand
import com.example.backend.user.application.port.outbound.PlaceAccessPort
import com.example.backend.user.application.port.outbound.PlaceRef
import com.example.backend.user.application.port.outbound.SavedPlaceCategoryCountRow
import com.example.backend.user.application.port.outbound.SavedPlacePersistencePort
import com.example.backend.user.application.port.outbound.SavedPlaceRow
import com.example.backend.user.domain.model.SavedPlace
import com.example.backend.user.domain.model.SavedPlaceCategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

/**
 * [SavedPlaceService] 단위 테스트 — 포트를 페이크로 대체해 서비스 규칙만 검증한다([SavedCourseServiceTest] 와 같은 형식).
 * 검증 대상: 저장 시 장소 존재 검증(ACL 포트)·중복 차단·카테고리 스냅샷 매핑, 취소 위임(멱등),
 * 조회의 커서 파싱·hasNext 판정(size+1 조회)·필터와 무관한 배지 카운트.
 */
class SavedPlaceServiceTest {
    private val fakePlaceAccess =
        object : PlaceAccessPort {
            /** placeId → 장소 카테고리 코드. 없는 키는 삭제/부재 장소로 취급한다. */
            var places: Map<Long, String> = emptyMap()
            val requestedPlaceIds = mutableListOf<Long>()

            override fun findPlace(placeId: Long): PlaceRef? {
                requestedPlaceIds += placeId
                return places[placeId]?.let { PlaceRef(id = placeId, category = it) }
            }
        }

    private val fakePort =
        object : SavedPlacePersistencePort {
            var savedPlaces: Set<Pair<Long, Long>> = emptySet()
            var pageRows: List<SavedPlaceRow> = emptyList()
            var categoryCounts: List<SavedPlaceCategoryCountRow> = emptyList()

            /** visited 별 카운트 — 배지가 필터와 무관한 전체 기준임을 검증하려고 값을 따로 둔다. */
            var unvisitedCount: Long = 0
            var visitedCount: Long = 0

            // 호출 캡처
            var insertArgs: Triple<Long, Long, SavedPlaceCategory?>? = null
            var deleteArgs: Pair<Long, Long>? = null
            var findPageArgs: FindPageArgs? = null

            override fun existsSavedPlace(
                userId: Long,
                placeId: Long,
            ): Boolean = (userId to placeId) in savedPlaces

            override fun insert(
                userId: Long,
                placeId: Long,
                category: SavedPlaceCategory?,
            ): SavedPlace {
                insertArgs = Triple(userId, placeId, category)
                return SavedPlace(
                    id = 100L,
                    userId = userId,
                    placeId = placeId,
                    category = category,
                    visited = false,
                    savedAt = Instant.parse("2026-08-13T00:00:00Z"),
                )
            }

            override fun deleteByUserAndPlace(
                userId: Long,
                placeId: Long,
            ): Boolean {
                deleteArgs = userId to placeId
                return true
            }

            override fun countByVisited(
                userId: Long,
                visited: Boolean,
            ): Long = if (visited) visitedCount else unvisitedCount

            override fun countByCategory(userId: Long): List<SavedPlaceCategoryCountRow> = categoryCounts

            override fun findPage(
                userId: Long,
                visited: Boolean,
                category: SavedPlaceCategory?,
                cursorId: Long?,
                limit: Int,
            ): List<SavedPlaceRow> {
                findPageArgs = FindPageArgs(userId, visited, category, cursorId, limit)
                return pageRows
            }
        }

    private data class FindPageArgs(
        val userId: Long,
        val visited: Boolean,
        val category: SavedPlaceCategory?,
        val cursorId: Long?,
        val limit: Int,
    )

    private val service = SavedPlaceService(fakePort, fakePlaceAccess)

    private fun row(
        id: Long,
        category: SavedPlaceCategory? = null,
    ) = SavedPlaceRow(id = id, placeId = id * 10, category = category, visited = false, savedAt = Instant.EPOCH)

    // --- save ---

    @Test
    fun `저장할 장소가 없으면 PLACE_NOT_FOUND 를 던지고 저장하지 않는다`() {
        fakePlaceAccess.places = emptyMap()

        val ex = assertThrows<BusinessException> { service.save(userId = 1L, placeId = 42L) }

        assertEquals(ErrorCode.PLACE_NOT_FOUND, ex.errorCode)
        assertNull(fakePort.insertArgs)
    }

    @Test
    fun `이미 저장한 장소면 PLACE_ALREADY_SAVED 를 던지고 저장하지 않는다`() {
        fakePlaceAccess.places = mapOf(42L to "CAFE")
        fakePort.savedPlaces = setOf(1L to 42L)

        val ex = assertThrows<BusinessException> { service.save(userId = 1L, placeId = 42L) }

        assertEquals(ErrorCode.PLACE_ALREADY_SAVED, ex.errorCode)
        assertNull(fakePort.insertArgs)
    }

    @Test
    fun `유효하면 장소 카테고리를 스냅샷으로 복사해 저장한다`() {
        fakePlaceAccess.places = mapOf(42L to "RESTAURANT")

        val result = service.save(userId = 1L, placeId = 42L)

        assertEquals(Triple(1L, 42L, SavedPlaceCategory.RESTAURANT), fakePort.insertArgs)
        assertEquals(42L, result.placeId)
        assertEquals(SavedPlaceCategory.RESTAURANT, result.category)
        assertFalse(result.visited) // 저장 직후는 미방문
    }

    @Test
    fun `존재 검증은 ACL 포트로 해당 placeId 만 조회한다`() {
        fakePlaceAccess.places = mapOf(42L to "CAFE")

        service.save(userId = 1L, placeId = 42L)

        assertEquals(listOf(42L), fakePlaceAccess.requestedPlaceIds)
    }

    @Test
    fun `place 카테고리가 SavedPlaceCategory 에 없는 값이면 미분류(null)로 저장한다`() {
        fakePlaceAccess.places = mapOf(42L to "UNKNOWN_FROM_PLACE_DOMAIN")

        val result = service.save(userId = 1L, placeId = 42L)

        assertEquals(Triple(1L, 42L, null), fakePort.insertArgs)
        assertNull(result.category)
    }

    // --- unsave ---

    @Test
    fun `저장 취소는 삭제 포트에 위임한다 (없어도 성공하는 멱등 연산)`() {
        service.unsave(userId = 1L, placeId = 42L)

        assertEquals(1L to 42L, fakePort.deleteArgs)
    }

    // --- getSavedPlaces ---

    @Test
    fun `hasNext 판정을 위해 size + 1 로 조회하고 size 만큼만 반환한다`() {
        fakePort.pageRows = listOf(row(5), row(4), row(3)) // size=2 + 1 개

        val result = service.getSavedPlaces(command(size = 2))

        assertEquals(3, fakePort.findPageArgs?.limit) // size + 1
        assertEquals(listOf(5L, 4L), result.savedPlaces.map { it.id }) // 초과분 잘라냄
        assertTrue(result.hasNext)
        assertEquals("4", result.nextCursor) // 잘라낸 페이지의 마지막 id
    }

    @Test
    fun `마지막 페이지면 hasNext 는 false 이고 nextCursor 는 null 이다`() {
        fakePort.pageRows = listOf(row(5), row(4)) // size 와 같음 = 더 없음

        val result = service.getSavedPlaces(command(size = 2))

        assertFalse(result.hasNext)
        assertNull(result.nextCursor)
        assertEquals(2, result.savedPlaces.size)
    }

    @Test
    fun `커서를 파싱해 cursorId 로 넘긴다`() {
        val result = service.getSavedPlaces(command(cursor = "7", size = 2))

        assertEquals(7L, fakePort.findPageArgs?.cursorId)
        assertNull(result.nextCursor) // 빈 페이지
    }

    @Test
    fun `커서가 숫자가 아니면 INVALID_INPUT 을 던진다`() {
        val ex = assertThrows<BusinessException> { service.getSavedPlaces(command(cursor = "not-a-number")) }

        assertEquals(ErrorCode.INVALID_INPUT, ex.errorCode)
    }

    @Test
    fun `배지 카운트는 조회 필터와 무관한 전체 기준이고 total 은 미방문 + 방문 이다`() {
        fakePort.unvisitedCount = 3
        fakePort.visitedCount = 4

        // visited=true, category 필터가 걸린 조회여도 카운트는 전체 기준이어야 한다.
        val result = service.getSavedPlaces(command(visited = true, category = SavedPlaceCategory.CAFE))

        assertEquals(3L, result.unvisitedCount)
        assertEquals(4L, result.visitedCount)
        assertEquals(7L, result.totalCount)
    }

    @Test
    fun `categoryCounts 는 개수 내림차순으로 정렬한다`() {
        fakePort.categoryCounts =
            listOf(
                SavedPlaceCategoryCountRow(SavedPlaceCategory.CAFE, 2),
                SavedPlaceCategoryCountRow(SavedPlaceCategory.BAR, 5),
                SavedPlaceCategoryCountRow(SavedPlaceCategory.NATURE, 3),
            )

        val result = service.getSavedPlaces(command())

        assertEquals(
            listOf(SavedPlaceCategory.BAR, SavedPlaceCategory.NATURE, SavedPlaceCategory.CAFE),
            result.categoryCounts.map { it.category },
        )
        assertEquals(listOf(5L, 3L, 2L), result.categoryCounts.map { it.count })
    }

    @Test
    fun `조회 필터(visited, category)를 그대로 포트에 전달한다`() {
        service.getSavedPlaces(command(visited = true, category = SavedPlaceCategory.WELLNESS, size = 10))

        val args = fakePort.findPageArgs
        assertEquals(1L, args?.userId)
        assertEquals(true, args?.visited)
        assertEquals(SavedPlaceCategory.WELLNESS, args?.category)
    }

    private fun command(
        visited: Boolean = false,
        category: SavedPlaceCategory? = null,
        cursor: String? = null,
        size: Int = 20,
    ) = SavedPlacesCommand(userId = 1L, visited = visited, category = category, cursor = cursor, size = size)
}
