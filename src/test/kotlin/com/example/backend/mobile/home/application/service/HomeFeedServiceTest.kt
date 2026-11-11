package com.example.backend.mobile.home.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.mobile.home.application.port.outbound.HomeFeedPort
import com.example.backend.mobile.home.application.port.outbound.HomeInterestPort
import com.example.backend.mobile.home.application.port.outbound.HomeNearbyPlacePort
import com.example.backend.mobile.home.application.port.outbound.HomeProfilePort
import com.example.backend.mobile.home.application.port.outbound.HomeTracePort
import com.example.backend.mobile.home.application.port.outbound.HomeUserLocationPort
import com.example.backend.mobile.home.application.port.outbound.dto.HomeFeedCourse
import com.example.backend.mobile.home.application.port.outbound.dto.HomeFeedCoursePage
import com.example.backend.mobile.home.application.port.outbound.dto.HomeInterest
import com.example.backend.mobile.home.application.port.outbound.dto.HomeNearbyPlace
import com.example.backend.mobile.home.application.port.outbound.dto.HomeProfile
import com.example.backend.mobile.home.application.port.outbound.dto.HomeTracingCourse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * [HomeFeedService] 단위 테스트 — 아웃바운드 포트 6개를 페이크로 대체해 홈 조합 규칙만 검증한다.
 * 검증 대상: 비로그인과 좌표 없음에서 근처 섹션 생략, 추천 코스 상위 5개 요청, 관심사 전달, 도보 분 올림, 커서 통과,
 * 최근 위치 저장(로그인과 좌표가 있을 때만, 실패해도 홈 정상).
 */
class HomeFeedServiceTest {
    private val fakeFeedPort =
        object : HomeFeedPort {
            var page = HomeFeedCoursePage(courses = listOf(course(1)), hasNext = false, nextCursor = null)
            val calls = mutableListOf<Pair<String?, Int>>()
            val categories = mutableListOf<String?>()
            val interests = mutableListOf<HomeInterest>()

            override fun listRecommended(
                interest: HomeInterest,
                cursor: String?,
                size: Int,
                category: String?,
                region: String?,
            ): HomeFeedCoursePage {
                interests += interest
                calls += cursor to size
                categories += category
                return page
            }
        }

    private val fakeInterestPort =
        object : HomeInterestPort {
            var interest = HomeInterest(likeThemes = listOf("CAFETOUR"), areaCodes = listOf("1168000000"))

            override fun findInterests(userId: Long): HomeInterest = interest
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
            val radii = mutableListOf<Double>()

            override fun findNearbySavedPlaces(
                userId: Long,
                origin: Coordinate,
                radiusMeters: Double,
                limit: Int,
            ): List<HomeNearbyPlace> {
                calls += Triple(userId, origin, limit)
                radii += radiusMeters
                return places
            }
        }

    private val fakeLocationPort =
        object : HomeUserLocationPort {
            var failure: RuntimeException? = null
            val calls = mutableListOf<Pair<Long, Coordinate>>()

            override fun record(
                userId: Long,
                location: Coordinate,
            ) {
                calls += userId to location
                failure?.let { throw it }
            }
        }

    private val service =
        HomeFeedService(
            fakeFeedPort,
            fakeInterestPort,
            fakeProfilePort,
            fakeNearbyPort,
            object : HomeTracePort {
                override fun findInProgress(userId: Long): HomeTracingCourse? = null
            },
            fakeLocationPort,
        )

    @Test
    fun `로그인 사용자가 좌표를 보내면 최근 위치로 저장한다`() {
        service.getHome(viewerId = USER_ID, userLocation = ORIGIN)

        assertEquals(listOf(USER_ID to ORIGIN), fakeLocationPort.calls)
    }

    @Test
    fun `비로그인이거나 좌표가 없으면 위치를 저장하지 않는다`() {
        service.getHome(viewerId = null, userLocation = ORIGIN)
        service.getHome(viewerId = USER_ID, userLocation = null)

        assertTrue(fakeLocationPort.calls.isEmpty())
    }

    @Test
    fun `위치 저장이 실패해도 홈은 정상 응답한다`() {
        fakeLocationPort.failure = IllegalStateException("db down")

        val result = service.getHome(viewerId = USER_ID, userLocation = ORIGIN)

        assertEquals(listOf(1L), result.recommendedCourses.map { it.id })
        assertEquals("칠삼이", result.profile?.nickname)
    }

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

        assertEquals(listOf<Pair<String?, Int>>(null to 5), fakeFeedPort.calls)
    }

    @Test
    fun `로그인하면 관심사를 추천 포트에 넘기고 비로그인은 빈 관심사다`() {
        service.getHome(viewerId = USER_ID, userLocation = null)
        service.getHome(viewerId = null, userLocation = null)

        assertEquals(listOf(fakeInterestPort.interest, HomeInterest.NONE), fakeFeedPort.interests)
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

        assertEquals(listOf(Triple(USER_ID, ORIGIN, 3)), fakeNearbyPort.calls)
        assertEquals(listOf(1L, 2L, 3L, 4L), result.nearbySavedPlaces.map { it.place.placeId })
        // 400/67=5.97 → 6, 670/67=10 → 10(정수면 그대로), 671/67=10.01 → 11, 0 → 0
        assertEquals(listOf(6, 10, 11, 0), result.nearbySavedPlaces.map { it.walkingMinutes })
    }

    @Test
    fun `반경 제한은 조회 포트에 전달하고 도보 시간은 표시만 한다`() {
        fakeNearbyPort.places = listOf(place(1, 2010.0), place(2, 2011.0))

        val result = service.getHome(viewerId = USER_ID, userLocation = ORIGIN)

        assertEquals(listOf(2010.0), fakeNearbyPort.radii)
        assertEquals(listOf(Triple(USER_ID, ORIGIN, 3)), fakeNearbyPort.calls)
        assertEquals(listOf(1L, 2L), result.nearbySavedPlaces.map { it.place.placeId })
        assertEquals(listOf(30, 31), result.nearbySavedPlaces.map { it.walkingMinutes })
    }

    @Test
    fun `근처 포트가 빈 목록이면 섹션도 비어 있다 (저장 0개나 검색 장애 흡수)`() {
        fakeNearbyPort.places = emptyList()

        val result = service.getHome(viewerId = USER_ID, userLocation = ORIGIN)

        assertTrue(result.nearbySavedPlaces.isEmpty())
        assertEquals("칠삼이", result.profile?.nickname)
    }

    @Test
    fun `전체보기 피드는 포트가 준 커서와 hasNext 를 그대로 내려준다`() {
        fakeFeedPort.page =
            HomeFeedCoursePage(courses = listOf(course(3), course(2)), hasNext = true, nextCursor = "next")

        val result = service.getFeed(viewerId = null, cursor = "prev", size = 2)

        assertTrue(result.hasNext)
        assertEquals("next", result.nextCursor)
        assertEquals(listOf<Pair<String?, Int>>("prev" to 2), fakeFeedPort.calls)
    }

    @Test
    fun `전체보기 피드는 카테고리를 포트에 그대로 넘긴다`() {
        service.getFeed(viewerId = null, cursor = null, size = 2, category = "DATE")

        assertEquals(listOf<String?>("DATE"), fakeFeedPort.categories)
    }

    private fun course(id: Long) =
        HomeFeedCourse(
            id = id,
            authorId = 1,
            title = "코스 $id",
            coverImageUrl = null,
            themes = listOf("CAFETOUR"),
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
