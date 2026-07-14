package com.example.backend.place.adapter.inbound.web

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.mock.MockErrors
import com.example.backend.common.response.ApiResponse
import com.example.backend.common.response.ErrorCode
import com.example.backend.place.adapter.inbound.web.response.PlaceDetailResponse
import com.example.backend.place.adapter.inbound.web.response.PlaceSearchResponse
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Base64

/**
 * 인바운드 어댑터 — 장소(노션 명세 · Place).
 *
 * - [detail] 장소 상세(`GET /api/v1/places/{placeId}`): 실구현. Result → Response 매핑만 담당하고
 *   조합·정책은 [PlaceQueryUseCase] 구현이 가진다.
 * - [search] 장소 검색(`GET /api/v1/places`): **모킹 API**. 지도 뷰포트 안의 장소를 목록/핀으로 내려준다.
 *   컨트롤러에서 목 데이터를 직접 만들어 반환하며, 실제 구현 시 인바운드 포트(UseCase) 연동으로 교체하고
 *   [MockErrors] 호출을 제거한다. `mockError` 파라미터로 모킹 에러를 주입할 수 있다(예: `?mockError=4040`).
 */
@RestController
@RequestMapping("/api/v1/places")
class PlaceController(
    private val placeQueryUseCase: PlaceQueryUseCase,
) {
    @GetMapping("/{placeId}")
    fun detail(
        @PathVariable placeId: Long,
    ): ApiResponse<PlaceDetailResponse> =
        ApiResponse.success(PlaceDetailResponse.from(placeQueryUseCase.getDetail(placeId)))

    /**
     * 장소 검색(모킹). 지도 화면에서 보이는 영역(뷰포트) 안의 장소를 검색한다.
     *
     * 쿼리 파라미터
     * - q: 검색어(선택). 목업은 매칭 필터를 적용하지 않고 그대로 받는다.
     * - swLat/swLng, neLat/neLng: 지도 뷰포트의 남서·북동 모서리 좌표. 지도 화면은 4개를 함께 보내 영역을 좁히고,
     *   지도 없는 목록 검색(q만)은 생략한다. **4개 전부 또는 전부 생략**이며, 일부만 보내면 400.
     * - userLat/userLng: 사용자 현재 위치(선택). 있을 때만 "도보 N분"(walkingMinutes)을 채운다.
     * - category: 카테고리 칩 필터(선택). 지정 시 해당 카테고리를 가진 장소만.
     * - sort: DISTANCE(거리순, 기본) | REVIEW(리뷰순). 잘못된 값은 400.
     * - cursor: 직전 응답의 nextCursor 를 그대로 넘긴다(첫 페이지는 생략). 유효하지 않은 커서는 400.
     * - size: 페이지 크기(기본 10, 1~50). 범위를 벗어나면 400.
     */
    @GetMapping
    fun search(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) @DecimalMin("-90.0") @DecimalMax("90.0") swLat: Double?,
        @RequestParam(required = false) @DecimalMin("-180.0") @DecimalMax("180.0") swLng: Double?,
        @RequestParam(required = false) @DecimalMin("-90.0") @DecimalMax("90.0") neLat: Double?,
        @RequestParam(required = false) @DecimalMin("-180.0") @DecimalMax("180.0") neLng: Double?,
        @RequestParam(required = false) @DecimalMin("-90.0") @DecimalMax("90.0") userLat: Double?,
        @RequestParam(required = false) @DecimalMin("-180.0") @DecimalMax("180.0") userLng: Double?,
        @RequestParam(required = false) category: String?,
        @RequestParam(defaultValue = "DISTANCE") sort: PlaceSearchSort,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "10") @Min(1) @Max(50) size: Int,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<PlaceSearchResponse> {
        MockErrors.throwIfRequested(mockError)
        validateViewport(swLat, swLng, neLat, neLng)

        val hasUserLocation = userLat != null && userLng != null
        val filtered =
            MOCK_PLACES
                .filter { category == null || category in it.categories }
                .sortedWith(sort.comparator)

        val offset = cursor.toOffset()
        val window = filtered.drop(offset).take(size)
        val nextOffset = offset + window.size
        val hasNext = nextOffset < filtered.size

        return ApiResponse.success(
            PlaceSearchResponse(
                totalCount = filtered.size,
                nextCursor = if (hasNext) nextOffset.toCursor() else null,
                hasNext = hasNext,
                places = window.map { it.toItem(hasUserLocation) },
            ),
        )
    }

    /**
     * 뷰포트 좌표는 4개 전부 또는 전부 생략. 일부만 오면 영역을 확정할 수 없어 400.
     * (전부 생략 = 지도 없는 키워드 검색, 전부 지정 = 지도 뷰포트 검색.) 값 범위는 Bean Validation 이 담당.
     */
    private fun validateViewport(
        swLat: Double?,
        swLng: Double?,
        neLat: Double?,
        neLng: Double?,
    ) {
        val provided = listOf(swLat, swLng, neLat, neLng).count { it != null }
        if (provided in 1..3) {
            throw BusinessException(
                ErrorCode.INVALID_INPUT,
                "지도 영역 좌표(swLat, swLng, neLat, neLng)는 4개를 모두 함께 보내거나 모두 생략해야 합니다.",
            )
        }
        // 4개 모두 지정된 경우, 남서 모서리는 북동 모서리보다 남·서에 있어야 한다(뒤집힌 영역 방지).
        if (provided == 4 && (swLat!! > neLat!! || swLng!! > neLng!!)) {
            throw BusinessException(
                ErrorCode.INVALID_INPUT,
                "지도 영역 좌표가 올바르지 않습니다: 남서(swLat, swLng)는 북동(neLat, neLng)보다 남·서쪽이어야 합니다.",
            )
        }
    }

    private fun MockPlace.toItem(hasUserLocation: Boolean) =
        PlaceSearchResponse.PlaceItem(
            id = id,
            name = name,
            imageUrl = imageUrl,
            categories = categories,
            averageRating = averageRating,
            reviewCount = reviewCount,
            walkingMinutes = if (hasUserLocation) walkingMinutes else null,
            location = PlaceSearchResponse.Location(latitude, longitude),
            hasSaved = hasSaved,
        )

    /** 커서는 오프셋 정수를 Base64 로 감싼 불투명 토큰. 손상된 값은 400. */
    private fun String?.toOffset(): Int {
        if (this == null) return 0
        return runCatching { String(Base64.getUrlDecoder().decode(this)).toInt() }
            .getOrNull()
            ?.takeIf { it >= 0 }
            ?: throw BusinessException(ErrorCode.INVALID_INPUT, "유효하지 않은 커서입니다.")
    }

    private fun Int.toCursor(): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(toString().toByteArray())

    /** 목 데이터 시드 — 응답 매핑 전 내부 표현. */
    private data class MockPlace(
        val id: Long,
        val name: String,
        val imageUrl: String?,
        val categories: List<String>,
        val averageRating: Double,
        val reviewCount: Int,
        val walkingMinutes: Int,
        val latitude: Double,
        val longitude: Double,
        val hasSaved: Boolean,
    )

    private companion object {
        val PlaceSearchSort.comparator: Comparator<MockPlace>
            get() =
                when (this) {
                    PlaceSearchSort.DISTANCE -> compareBy { it.walkingMinutes }
                    PlaceSearchSort.REVIEW -> compareByDescending { it.reviewCount }
                }

        fun image(id: String) = "https://images.unsplash.com/$id?w=600&q=80&auto=format&fit=crop"

        /** 성수동 일대 카페 — 디자인(검색 결과 · 장소 탭)의 예시 목록을 그대로 반영. */
        val MOCK_PLACES: List<MockPlace> =
            listOf(
                MockPlace(
                    id = 101,
                    name = "어니언 성수",
                    imageUrl = image("photo-1517433670267-08bbd4be890f"),
                    categories = listOf("카페", "베이커리"),
                    averageRating = 4.8,
                    reviewCount = 1240,
                    walkingMinutes = 6,
                    latitude = 37.5445,
                    longitude = 127.0578,
                    hasSaved = true,
                ),
                MockPlace(
                    id = 102,
                    name = "콤포트 성수",
                    imageUrl = image("photo-1495474472287-4d71bcdd2085"),
                    categories = listOf("카페", "브런치"),
                    averageRating = 4.6,
                    reviewCount = 430,
                    walkingMinutes = 5,
                    latitude = 37.5432,
                    longitude = 127.0561,
                    hasSaved = false,
                ),
                MockPlace(
                    id = 103,
                    name = "아우어베이커리 성수",
                    imageUrl = image("photo-1521017432531-fbd92d768814"),
                    categories = listOf("카페", "디저트"),
                    averageRating = 4.5,
                    reviewCount = 205,
                    walkingMinutes = 8,
                    latitude = 37.5451,
                    longitude = 127.0549,
                    hasSaved = true,
                ),
                MockPlace(
                    id = 104,
                    name = "대림창고 카페",
                    imageUrl = image("photo-1509042239860-f550ce710b93"),
                    categories = listOf("카페", "전시"),
                    averageRating = 4.6,
                    reviewCount = 320,
                    walkingMinutes = 9,
                    latitude = 37.5418,
                    longitude = 127.0592,
                    hasSaved = false,
                ),
                MockPlace(
                    id = 105,
                    name = "센터커피 성수",
                    imageUrl = image("photo-1442512595331-e89e73853f31"),
                    categories = listOf("카페", "로스터리"),
                    averageRating = 4.7,
                    reviewCount = 512,
                    walkingMinutes = 12,
                    latitude = 37.5463,
                    longitude = 127.0537,
                    hasSaved = false,
                ),
                MockPlace(
                    id = 106,
                    name = "대성정미소 카페",
                    imageUrl = image("photo-1445116572660-236099ec97a0"),
                    categories = listOf("카페", "전시"),
                    averageRating = 4.4,
                    reviewCount = 88,
                    walkingMinutes = 14,
                    latitude = 37.5409,
                    longitude = 127.0605,
                    hasSaved = false,
                ),
            )
    }
}
