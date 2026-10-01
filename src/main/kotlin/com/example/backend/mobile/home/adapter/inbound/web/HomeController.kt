package com.example.backend.mobile.home.adapter.inbound.web

import com.example.backend.bootstrap.appversion.AppFeature
import com.example.backend.bootstrap.appversion.RequiresAppFeature
import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.geo.Coordinate
import com.example.backend.common.response.ApiResponse
import com.example.backend.mobile.home.adapter.inbound.web.response.HomeResponse
import com.example.backend.mobile.home.adapter.inbound.web.response.RecommendedCoursesResponse
import com.example.backend.mobile.home.application.port.inbound.HomeFeedUseCase
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — **홈 화면 조합 API** (BFF). 비로그인 포함 누구나 조회 가능하다.
 * - `GET /service/v2/home`: 헤더 프로필 + 추천 코스 5개 + 근처 저장 장소 5개(`userLat/userLng` 기준).
 * - `GET /service/v2/home/recommended-courses`: 추천 코스 전체보기(저장수 내림차순, 최신순 커서 페이지).
 * 조합은 인바운드 포트([HomeFeedUseCase])가 담당하고, 컨트롤러는 Request → 포트 호출 → Response 매핑만 한다.
 *
 * 시드/DB 없이 프론트가 붙어볼 수 있도록 홈은 `?mock=true` 면 조회 없이 고정 목([HomeResponse.MOCK])을 반환한다(prod 프로파일에서는 [MockGuard]가 무시).
 */
@RequiresAppFeature(AppFeature.COURSE_DETAIL)
@RestController
@RequestMapping("/service/v2/home")
class HomeController(
    private val homeFeedUseCase: HomeFeedUseCase,
    private val mockGuard: MockGuard,
) {
    /** 홈 한 화면. 좌표는 둘 다 보내거나 둘 다 생략한다(한쪽만 오면 400). */
    @GetMapping
    fun getHome(
        @CurrentUserId viewerId: Long?,
        @RequestParam(required = false) userLat: Double?,
        @RequestParam(required = false) userLng: Double?,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<HomeResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(HomeResponse.MOCK)

        require((userLat == null) == (userLng == null)) { "userLat 과 userLng 는 함께 보내야 합니다." }
        val userLocation = userLat?.let { Coordinate(it, userLng!!) }
        return ApiResponse.success(HomeResponse.from(homeFeedUseCase.getHome(viewerId, userLocation)))
    }

    @GetMapping("/recommended-courses")
    fun getRecommendedCourses(
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false)
        @Min(1, message = "1 이상이어야 합니다")
        @Max(50, message = "50 이하여야 합니다")
        size: Int = 20,
    ): ApiResponse<RecommendedCoursesResponse> =
        ApiResponse.success(RecommendedCoursesResponse.from(homeFeedUseCase.getFeed(cursor, size)))
}
