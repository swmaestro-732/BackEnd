package com.example.backend.mobile.user.application.service

import com.example.backend.mobile.place.application.port.outbound.ScreenPlacePort
import com.example.backend.mobile.place.application.port.outbound.dto.ScreenPlace
import com.example.backend.mobile.user.application.port.inbound.SavedPlaceScreenCommand
import com.example.backend.mobile.user.application.port.outbound.SavedPlaceRecordPort
import com.example.backend.mobile.user.application.port.outbound.ScreenAreaPort
import com.example.backend.mobile.user.application.port.outbound.dto.SavedPlaceCategoryCount
import com.example.backend.mobile.user.application.port.outbound.dto.SavedPlaceRecord
import com.example.backend.mobile.user.application.port.outbound.dto.SavedPlaceRecordPage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * [SavedPlaceScreenService] 단위 테스트 — 아웃바운드 포트 3개를 페이크로 대체해 조합 규칙만 검증한다.
 * 검증 대상: 장소 배치 조회(N+1 회피)·해석 실패 항목 제외·지역 코드 중복 제거 조회·
 * 페이지 메타를 저장 레코드 기준으로 잇는 규칙.
 */
class SavedPlaceScreenServiceTest {
    private val fakeRecordPort =
        object : SavedPlaceRecordPort {
            var page: SavedPlaceRecordPage = emptyPage()
            var findPageArgs: FindPageArgs? = null

            override fun findPage(
                userId: Long,
                visited: Boolean,
                category: String?,
                cursor: String?,
                size: Int,
            ): SavedPlaceRecordPage {
                findPageArgs = FindPageArgs(userId, visited, category, cursor, size)
                return page
            }
        }

    private val fakePlacePort =
        object : ScreenPlacePort {
            var places: List<ScreenPlace> = emptyList()

            /** findByIds 호출 횟수·인자 — 항목별 조회(N+1)가 아닌지 본다. */
            val findByIdsCalls = mutableListOf<List<Long>>()

            override fun findById(placeId: Long): ScreenPlace? = throw AssertionError("목록 조합은 배치 조회를 써야 한다")

            override fun findByIds(placeIds: List<Long>): List<ScreenPlace> {
                findByIdsCalls += placeIds
                return places.filter { it.id in placeIds }
            }
        }

    private val fakeAreaPort =
        object : ScreenAreaPort {
            var namesByCode: Map<String, String> = emptyMap()
            val requestedCodes = mutableListOf<String>()

            override fun findAreaName(code: String): String? {
                requestedCodes += code
                return namesByCode[code]
            }
        }

    private data class FindPageArgs(
        val userId: Long,
        val visited: Boolean,
        val category: String?,
        val cursor: String?,
        val size: Int,
    )

    private val service = SavedPlaceScreenService(fakeRecordPort, fakePlacePort, fakeAreaPort)

    @Test
    fun `저장 레코드에 장소 요약과 지역 이름을 붙여 순서를 유지한 채 내려준다`() {
        fakeRecordPort.page = pageOf(record(3, placeId = 30, category = "CULTURE"), record(2, placeId = 20))
        fakePlacePort.places = listOf(place(30, "리움미술관", "CULTURE", areaCode = "1117013100"), place(20, "센터커피"))
        fakeAreaPort.namesByCode = mapOf("1117013100" to "한남동", AREA_CODE to "성수동1가")

        val result = service.getScreen(command())

        assertEquals(listOf(3L, 2L), result.items.map { it.id }) // 포트가 준 순서 그대로
        assertEquals("리움미술관", result.items[0].place.name)
        assertEquals("CULTURE", result.items[0].place.category)
        assertEquals("한남동", result.items[0].place.area)
        assertEquals("CULTURE", result.items[0].category) // 저장 스냅샷 카테고리(장소 카테고리와 별개)
        assertEquals("센터커피", result.items[1].place.name)
        assertEquals("성수동1가", result.items[1].place.area)
    }

    @Test
    fun `장소는 한 번의 배치 조회로 가져온다 (항목별 N+1 회피)`() {
        fakeRecordPort.page = pageOf(record(3, placeId = 30), record(2, placeId = 20), record(1, placeId = 10))
        fakePlacePort.places = listOf(place(30), place(20), place(10))

        service.getScreen(command())

        assertEquals(1, fakePlacePort.findByIdsCalls.size)
        assertEquals(listOf(30L, 20L, 10L), fakePlacePort.findByIdsCalls.single())
    }

    @Test
    fun `장소를 해석하지 못한 저장 항목은 목록에서 제외한다`() {
        fakeRecordPort.page = pageOf(record(3, placeId = 30), record(2, placeId = 20))
        fakePlacePort.places = listOf(place(20)) // 30 은 삭제돼 결과에 없음

        val result = service.getScreen(command())

        assertEquals(listOf(2L), result.items.map { it.id })
    }

    @Test
    fun `항목이 빠져도 페이지 메타와 배지 카운트는 저장 레코드 기준을 유지한다`() {
        // 마지막 레코드(2)의 장소가 해석 실패다 — 그래서 레코드 기준 커서("2")와
        // 살아남은 마지막 항목 기준 커서("3")가 달라, 둘을 혼동하면 이 테스트가 깨진다.
        fakeRecordPort.page =
            pageOf(
                record(3, placeId = 30),
                record(2, placeId = 20),
                totalCount = 9,
                unvisitedCount = 7,
                visitedCount = 2,
                nextCursor = "2",
                hasNext = true,
            )
        fakePlacePort.places = listOf(place(30)) // 20 해석 실패 → 항목 1건(3)

        val result = service.getScreen(command())

        assertEquals(listOf(3L), result.items.map { it.id })
        // 커서·hasNext 가 항목이 아니라 레코드 기준이어야 다음 페이지가 어긋나지 않는다.
        assertEquals("2", result.nextCursor)
        assertTrue(result.hasNext)
        assertEquals(9L, result.totalCount)
        assertEquals(7L, result.unvisitedCount)
        assertEquals(2L, result.visitedCount)
    }

    @Test
    fun `같은 지역 코드는 한 번만 조회한다`() {
        fakeRecordPort.page = pageOf(record(3, placeId = 30), record(2, placeId = 20), record(1, placeId = 10))
        // 30·20 은 같은 동, 10 은 다른 동
        fakePlacePort.places =
            listOf(place(30, areaCode = AREA_CODE), place(20, areaCode = AREA_CODE), place(10, areaCode = "1117013100"))
        fakeAreaPort.namesByCode = mapOf(AREA_CODE to "성수동1가", "1117013100" to "한남동")

        val result = service.getScreen(command())

        assertEquals(listOf(AREA_CODE, "1117013100"), fakeAreaPort.requestedCodes) // 코드 종류만큼만
        assertEquals("성수동1가", result.items[0].place.area)
        assertEquals("성수동1가", result.items[1].place.area)
        assertEquals("한남동", result.items[2].place.area)
    }

    @Test
    fun `장소에 지역 코드가 없으면 area 는 null 이고 지역 조회를 하지 않는다`() {
        fakeRecordPort.page = pageOf(record(2, placeId = 20))
        fakePlacePort.places = listOf(place(20, areaCode = null))

        val result = service.getScreen(command())

        assertNull(
            result.items
                .single()
                .place.area,
        )
        assertTrue(fakeAreaPort.requestedCodes.isEmpty())
    }

    @Test
    fun `지역 코드를 해석하지 못하면 area 만 null 이고 항목은 남는다`() {
        fakeRecordPort.page = pageOf(record(2, placeId = 20))
        fakePlacePort.places = listOf(place(20, areaCode = "9999999999"))
        fakeAreaPort.namesByCode = emptyMap() // 미존재·비활성 코드

        val result = service.getScreen(command())

        assertEquals(1, result.items.size)
        assertNull(
            result.items
                .single()
                .place.area,
        )
    }

    @Test
    fun `카테고리 미분류 저장은 category 가 null 로 내려간다`() {
        fakeRecordPort.page = pageOf(record(2, placeId = 20, category = null))
        fakePlacePort.places = listOf(place(20))

        val result = service.getScreen(command())

        assertNull(result.items.single().category)
    }

    @Test
    fun `조회 조건을 그대로 저장 레코드 포트에 전달한다`() {
        service.getScreen(command(visited = true, category = "CAFE", cursor = "7", size = 30))

        val args = fakeRecordPort.findPageArgs
        assertEquals(1L, args?.userId)
        assertEquals(true, args?.visited)
        assertEquals("CAFE", args?.category)
        assertEquals("7", args?.cursor)
        assertEquals(30, args?.size)
    }

    @Test
    fun `배지 카운트와 카테고리 칩은 포트가 준 값을 그대로 잇는다`() {
        fakeRecordPort.page =
            pageOf(
                categoryCounts =
                    listOf(SavedPlaceCategoryCount("CAFE", 3), SavedPlaceCategoryCount("CULTURE", 1)),
            )

        val result = service.getScreen(command())

        assertEquals(listOf("CAFE", "CULTURE"), result.categoryCounts.map { it.category })
        assertEquals(listOf(3L, 1L), result.categoryCounts.map { it.count })
    }

    @Test
    fun `저장이 없으면 빈 목록을 내려주고 장소 조회는 빈 배치로만 호출한다`() {
        fakeRecordPort.page = emptyPage()

        val result = service.getScreen(command())

        assertTrue(result.items.isEmpty())
        assertFalse(result.hasNext)
        assertEquals(listOf(emptyList<Long>()), fakePlacePort.findByIdsCalls)
    }

    private fun command(
        visited: Boolean = false,
        category: String? = null,
        cursor: String? = null,
        size: Int = 10,
    ) = SavedPlaceScreenCommand(userId = 1L, visited = visited, category = category, cursor = cursor, size = size)

    private fun record(
        id: Long,
        placeId: Long,
        category: String? = "CAFE",
        visited: Boolean = false,
    ) = SavedPlaceRecord(
        id = id,
        placeId = placeId,
        category = category,
        visited = visited,
        savedAt = Instant.parse("2026-08-13T00:00:00Z"),
    )

    private fun place(
        id: Long,
        name: String = "장소 $id",
        category: String = "CAFE",
        areaCode: String? = AREA_CODE,
    ) = ScreenPlace(
        id = id,
        name = name,
        category = category,
        imageUrl = null,
        latitude = 37.5,
        longitude = 127.0,
        address = "주소 $id",
        areaCode = areaCode,
    )

    private fun pageOf(
        vararg records: SavedPlaceRecord,
        totalCount: Long = records.size.toLong(),
        unvisitedCount: Long = records.size.toLong(),
        visitedCount: Long = 0,
        categoryCounts: List<SavedPlaceCategoryCount> = emptyList(),
        nextCursor: String? = null,
        hasNext: Boolean = false,
    ) = SavedPlaceRecordPage(
        totalCount = totalCount,
        unvisitedCount = unvisitedCount,
        visitedCount = visitedCount,
        categoryCounts = categoryCounts,
        nextCursor = nextCursor,
        hasNext = hasNext,
        records = records.toList(),
    )

    private fun emptyPage() = pageOf()

    private companion object {
        const val AREA_CODE = "1120011400"
    }
}
