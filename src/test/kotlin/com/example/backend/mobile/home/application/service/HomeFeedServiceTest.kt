package com.example.backend.mobile.home.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.mobile.home.application.port.outbound.HomeFeedPort
import com.example.backend.mobile.home.application.port.outbound.HomeNearbyPlacePort
import com.example.backend.mobile.home.application.port.outbound.HomeProfilePort
import com.example.backend.mobile.home.application.port.outbound.dto.HomeFeedCourse
import com.example.backend.mobile.home.application.port.outbound.dto.HomeFeedCoursePage
import com.example.backend.mobile.home.application.port.outbound.dto.HomeFeedCursor
import com.example.backend.mobile.home.application.port.outbound.dto.HomeNearbyPlace
import com.example.backend.mobile.home.application.port.outbound.dto.HomeProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * [HomeFeedService] 단위 테스트 — 아웃바운드 포트 3개를 페이크로 대체해 홈 조합 규칙만 검증한다.
 * 검증 대상: 비로그인과 좌표 없음에서 근처 섹션 생략, 추천 코스 상위 5개 요청, 도보 분 올림, 피드 커서 인코딩.
 */
class HomeFeedServiceTest {
    private val fakeFeedPort =
        object : HomeFeedPort {
            var page = HomeFeedCoursePage(courses = listOf(course(1)), hasNext = false)
            val calls = mutableListOf<Pair<HomeFeedCursor?, Int>>()

            override fun listPublicCandidates(
                cursor: HomeFeedCursor?,
                size: Int,
            ): HomeFeedCoursePage {
                calls += cursor to size
                return page
            }
        }

    private val fakeProfilePort =
        object : HomeProfilePort {
            var profiles: Map<Long, HomeProfile> = mapOf(USER_ID to HomeProfile("칠삼이", "https://img/me.jpg"))

            override fun findProfile(userId: Long): HomeProfile? = profiles[userId]
        }

    private val fakeNearbyPort =
        object : HomeNearbyPlacePort {
            var places: List<HomeNearbyPlace> = emptyList()
            val calls = mutableListOf<Triple<Long, Coordinate, Int>>()

            override fun findNearbySavedPlaces(
                userId: Long,
                origin: Coordinate,
                limit: Int,
            ): List<HomeNearbyPlace> {
                calls += Triple(userId, origin, limit)
                return places
            }
        }

    private val service = HomeFeedService(fakeFeedPort, fakeProfilePort, fakeNearbyPort)

    @Test
    fun `비로그인이면 프로필은 null 이고 근처 저장 장소는 조회하지 않는다`() {
        val result = service.getHome(viewerId = null, userLocation = ORIGIN)

        assertNull(result.profile)
        assertEquals(listOf(1L), result.recommendedCourses.map { it.id })
        assertTrue(result.nearbySavedPlaces.isEmpty())
        assertTrue(fakeNearbyPort.calls.isEmpty())
    }

    @Test
    fun `좌표가 없으면 로그인해도 근처 저장 장소는 조회하지 않는다`() {
        val result = service.getHome(viewerId = USER_ID, userLocation = null)

        assertEquals("칠삼이", result.profile?.nickname)
        assertTrue(result.nearbySavedPlaces.isEmpty())
        assertTrue(fakeNearbyPort.calls.isEmpty())
    }

    @Test
    fun `추천 코스는 피드 첫 페이지 상위 5개를 요청한다`() {
        service.getHome(viewerId = null, userLocation = null)

        assertEquals(listOf<Pair<HomeFeedCursor?, Int>>(null to 5), fakeFeedPort.calls)
    }

    @Test
    fun `사용자가 없으면 프로필만 null 이다`() {
        fakeProfilePort.profiles = emptyMap()

        val result = service.getHome(viewerId = USER_ID, userLocation = null)

        assertNull(result.profile)
        assertEquals(1, result.recommendedCourses.size)
    }

    @Test
    fun `근처 저장 장소는 포트 순서를 유지하고 도보 분을 분당 67m 기준으로 올림한다`() {
        fakeNearbyPort.places = listOf(place(1, 400.0), place(2, 670.0), place(3, 671.0), place(4, 0.0))

        val result = service.getHome(viewerId = USER_ID, userLocation = ORIGIN)

        assertEquals(listOf(Triple(USER_ID, ORIGIN, 5)), fakeNearbyPort.calls)
        assertEquals(listOf(1L, 2L, 3L, 4L), result.nearbySavedPlaces.map { it.place.placeId })
        // 400/67=5.97 → 6, 670/67=10 → 10(정수면 그대로), 671/67=10.01 → 11, 0 → 0
        assertEquals(listOf(6, 10, 11, 0), result.nearbySavedPlaces.map { it.walkingMinutes })
    }

    @Test
    fun `근처 포트가 빈 목록이면 섹션도 비어 있다 (저장 0개나 검색 장애 흡수)`() {
        fakeNearbyPort.places = emptyList()

        val result = service.getHome(viewerId = USER_ID, userLocation = ORIGIN)

        assertTrue(result.nearbySavedPlaces.isEmpty())
        assertEquals("칠삼이", result.profile?.nickname)
    }

    @Test
    fun `전체보기 피드는 다음 페이지가 있으면 마지막 코스로 커서를 만든다`() {
        fakeFeedPort.page = HomeFeedCoursePage(courses = listOf(course(3), course(2)), hasNext = true)

        val result = service.getFeed(cursor = null, size = 2)

        assertTrue(result.hasNext)
        assertEquals(2L, HomeFeedCursorCodec.decode(result.nextCursor)?.id)
    }

    private fun course(id: Long) =
        HomeFeedCourse(
            id = id,
            authorId = 1,
            title = "코스 $id",
            coverImageUrl = null,
            theme = "CAFETOUR",
            area = "성수",
            placeCount = 3,
            walkingMinutes = 12,
            likesCnt = 0,
            savesCnt = 0,
            createdAt = Instant.parse("2026-07-20T00:00:00Z"),
        )

    private fun place(
        id: Long,
        distanceMeters: Double,
    ) = HomeNearbyPlace(
        placeId = id,
        name = "장소 $id",
        imageUrl = null,
        category = "CAFE",
        rating = 4.5,
        ratingCount = 10,
        distanceMeters = distanceMeters,
        visited = false,
    )

    private companion object {
        const val USER_ID = 7L
        val ORIGIN = Coordinate(37.544, 127.056)
    }
}
