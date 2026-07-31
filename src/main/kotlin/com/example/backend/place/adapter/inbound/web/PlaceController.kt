package com.example.backend.place.adapter.inbound.web

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ApiResponse
import com.example.backend.common.response.ErrorCode
import com.example.backend.place.adapter.inbound.web.response.PlaceSearchResponse
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Base64

/**
 * 인바운드 어댑터 — 장소(노션 명세 · Place).
 *
 * - [search] 장소 검색(`GET /api/v1/places`): **모킹 API**. 지도 뷰포트 안의 장소를 목록/핀으로 내려준다.
 *   목 데이터는 [PlaceSearchResponse.MOCK] 시드에서 온다. 실제 구현 시 인바운드 포트(UseCase) 연동으로
 *   교체한다. 모킹 에러(`?mockError=<code>`)는 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 *
 * 장소 상세는 화면 조합이라 BFF 경로로 이관했다 → `GET /service/v1/places/{placeId}`
 * ([com.example.backend.mobile.place.adapter.inbound.web.PlaceDetailScreenController]).
 */
@RestController
@RequestMapping("/api/v1/places")
class PlaceController {
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
    ): ApiResponse<PlaceSearchResponse> {
        validateViewport(swLat, swLng, neLat, neLng)

        val hasUserLocation = userLat != null && userLng != null
        val filtered =
            PlaceSearchResponse.MOCK
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

    private fun PlaceSearchResponse.MockPlace.toItem(hasUserLocation: Boolean) =
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

    private companion object {
        val PlaceSearchSort.comparator: Comparator<PlaceSearchResponse.MockPlace>
            get() =
                when (this) {
                    PlaceSearchSort.DISTANCE -> compareBy { it.walkingMinutes }
                    PlaceSearchSort.REVIEW -> compareByDescending { it.reviewCount }
                }
    }
}
