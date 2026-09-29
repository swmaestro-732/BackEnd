package com.example.backend.mobile.home.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.mobile.home.application.port.inbound.HomeFeedUseCase
import com.example.backend.mobile.home.application.port.inbound.dto.HomeFeedResult
import com.example.backend.mobile.home.application.port.inbound.dto.HomeResult
import com.example.backend.mobile.home.application.port.outbound.HomeFeedPort
import com.example.backend.mobile.home.application.port.outbound.HomeNearbyPlacePort
import com.example.backend.mobile.home.application.port.outbound.HomeProfilePort
import com.example.backend.mobile.home.application.port.outbound.dto.HomeFeedCursor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import kotlin.math.ceil

/**
 * 홈 화면 조합 서비스 (BFF). 자신의 아웃바운드 포트([HomeFeedPort], [HomeProfilePort], [HomeNearbyPlacePort])만 호출해
 * 한 화면 응답 재료를 만든다.
 * 타 도메인 인바운드에 직접 의존하지 않아 MSA 분리 시 어댑터만 교체하면 된다.
 *
 * 저장수 랭킹은 course 도메인이 소유한 denormalized `courses.saves_cnt`(저장/취소 시 갱신)를 기준으로
 * SQL(saves_cnt DESC, created_at DESC, id DESC)에서 정렬·키셋 조회되어 내려오며,
 * BFF 는 외부 문자열 커서의 디코딩과 다음 커서 인코딩을 담당한다.
 */
@Service
@Transactional(readOnly = true)
class HomeFeedService(
    private val homeFeedPort: HomeFeedPort,
    private val homeProfilePort: HomeProfilePort,
    private val homeNearbyPlacePort: HomeNearbyPlacePort,
) : HomeFeedUseCase {
    /**
     * 바깥 트랜잭션 없이 조합한다 — 근처 장소 어댑터가 흡수하는 검색 503 이 참여 트랜잭션을 rollback-only 로 찍어
     * 커밋 시 UnexpectedRollbackException 으로 홈 전체가 실패하지 않도록, 도메인 호출마다 각자의 트랜잭션을 쓴다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    override fun getHome(
        viewerId: Long?,
        userLocation: Coordinate?,
    ): HomeResult =
        HomeResult(
            profile = viewerId?.let(homeProfilePort::findProfile),
            recommendedCourses = homeFeedPort.listPublicCandidates(null, HOME_SECTION_SIZE).courses,
            nearbySavedPlaces =
                if (viewerId == null || userLocation == null) {
                    emptyList()
                } else {
                    homeNearbyPlacePort.findNearbySavedPlaces(viewerId, userLocation, HOME_SECTION_SIZE).map {
                        HomeResult.NearbySavedPlace(
                            place = it,
                            walkingMinutes = ceil(it.distanceMeters / WALKING_METERS_PER_MINUTE).toInt(),
                        )
                    }
                },
        )

    override fun getFeed(
        cursor: String?,
        size: Int,
    ): HomeFeedResult {
        val page = homeFeedPort.listPublicCandidates(HomeFeedCursorCodec.decode(cursor), size)
        val nextCursor =
            if (page.hasNext) {
                page.courses.last().let {
                    HomeFeedCursorCodec.encode(
                        HomeFeedCursor(
                            savesCnt = it.savesCnt,
                            createdAt = it.createdAt,
                            id = it.id,
                        ),
                    )
                }
            } else {
                null
            }
        return HomeFeedResult(
            courses = page.courses,
            nextCursor = nextCursor,
            hasNext = page.hasNext,
        )
    }

    private companion object {
        const val HOME_SECTION_SIZE = 5

        /** 도보 속도 근사(시속 4km = 분당 약 67m). 경로 API(TMAP)는 부르지 않는다. */
        const val WALKING_METERS_PER_MINUTE = 67.0
    }
}
