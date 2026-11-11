package com.example.backend.mobile.home.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.mobile.home.application.port.inbound.HomeFeedUseCase
import com.example.backend.mobile.home.application.port.inbound.dto.HomeFeedResult
import com.example.backend.mobile.home.application.port.inbound.dto.HomeResult
import com.example.backend.mobile.home.application.port.outbound.HomeFeedPort
import com.example.backend.mobile.home.application.port.outbound.HomeInterestPort
import com.example.backend.mobile.home.application.port.outbound.HomeNearbyPlacePort
import com.example.backend.mobile.home.application.port.outbound.HomeProfilePort
import com.example.backend.mobile.home.application.port.outbound.HomeTracePort
import com.example.backend.mobile.home.application.port.outbound.HomeUserLocationPort
import com.example.backend.mobile.home.application.port.outbound.dto.HomeInterest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import kotlin.math.ceil

/**
 * 홈 화면 조합 서비스 (BFF). 자신의 아웃바운드 포트([HomeFeedPort], [HomeInterestPort], [HomeProfilePort], [HomeNearbyPlacePort],
 * [HomeTracePort], [HomeUserLocationPort])만 호출해 한 화면 응답 재료를 만든다. 로그인 사용자가 좌표를 보내면 최근 위치로 남긴다.
 * 타 도메인 인바운드에 직접 의존하지 않아 MSA 분리 시 어댑터만 교체하면 된다.
 *
 * 추천 코스는 로그인 사용자의 관심 테마·지역을 course 도메인에 넘겨 랭킹하며(비로그인은 관심 없이 최신성·저장수만),
 * 커서는 course 도메인이 만든 불투명 문자열을 그대로 내려주고 그대로 돌려받는다.
 */
@Service
@Transactional(readOnly = true)
class HomeFeedService(
    private val homeFeedPort: HomeFeedPort,
    private val homeInterestPort: HomeInterestPort,
    private val homeProfilePort: HomeProfilePort,
    private val homeNearbyPlacePort: HomeNearbyPlacePort,
    private val homeTracePort: HomeTracePort,
    private val homeUserLocationPort: HomeUserLocationPort,
) : HomeFeedUseCase {
    private val log = KotlinLogging.logger {}

    /**
     * 바깥 트랜잭션 없이 조합한다 — OS 조회 동안 DB 트랜잭션을 잡아두지 않고,
     * 프로필·추천 코스·장소 상세는 각 도메인의 읽기 트랜잭션으로 조회한다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    override fun getHome(
        viewerId: Long?,
        userLocation: Coordinate?,
    ): HomeResult {
        if (viewerId != null && userLocation != null) recordLocation(viewerId, userLocation)
        return HomeResult(
            profile = viewerId?.let(homeProfilePort::findProfile),
            tracingCourse = viewerId?.let(homeTracePort::findInProgress),
            recommendedCourses = homeFeedPort.listRecommended(interestOf(viewerId), null, HOME_SECTION_SIZE).courses,
            nearbySavedPlaces =
                if (viewerId == null || userLocation == null) {
                    emptyList()
                } else {
                    // 반경 제한은 엔진 쿼리에 적용하고, 도보 시간은 표시용으로만 계산한다.
                    homeNearbyPlacePort
                        .findNearbySavedPlaces(
                            viewerId,
                            userLocation,
                            MAX_WALKING_MINUTES * WALKING_METERS_PER_MINUTE,
                            HOME_NEARBY_SIZE,
                        ).map {
                            HomeResult.NearbySavedPlace(
                                place = it,
                                walkingMinutes = ceil(it.distanceMeters / WALKING_METERS_PER_MINUTE).toInt(),
                            )
                        }
                },
        )
    }

    /**
     * 위치 저장은 홈 응답의 부수 효과라 실패해도 홈은 그대로 내려준다.
     * 예외 메시지(ExposedSQLException 의 바인딩 펼친 SQL 등)에 좌표가 실리므로 예외 객체는 로그에 넘기지 않는다.
     */
    private fun recordLocation(
        userId: Long,
        location: Coordinate,
    ) {
        try {
            homeUserLocationPort.record(userId, location)
        } catch (e: Exception) {
            log.warn { "홈 최근 위치 저장 실패: userId=$userId, error=${e.javaClass.name}" }
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    override fun getFeed(
        viewerId: Long?,
        cursor: String?,
        size: Int,
        category: String?,
        region: String?,
    ): HomeFeedResult {
        val page = homeFeedPort.listRecommended(interestOf(viewerId), cursor, size, category, region)
        return HomeFeedResult(
            courses = page.courses,
            nextCursor = page.nextCursor,
            hasNext = page.hasNext,
        )
    }

    private fun interestOf(viewerId: Long?): HomeInterest =
        viewerId?.let(homeInterestPort::findInterests) ?: HomeInterest.NONE

    private companion object {
        const val HOME_SECTION_SIZE = 5
        const val HOME_NEARBY_SIZE = 3

        /** 도보 속도 근사(시속 4km = 분당 약 67m). 경로 API(TMAP)는 부르지 않는다. */
        const val WALKING_METERS_PER_MINUTE = 67.0

        /** "근처"로 보여 줄 도보 시간 상한(분). 이보다 먼 저장 장소는 홈에 내리지 않는다(약 2km). */
        const val MAX_WALKING_MINUTES = 30
    }
}
