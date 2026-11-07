package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.appversion.AppFeature
import com.example.backend.bootstrap.appversion.RequiresAppFeature
import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.user.adapter.inbound.web.request.SavePlaceRequest
import com.example.backend.user.adapter.inbound.web.response.SavedPlaceListResponse
import com.example.backend.user.application.port.inbound.SavedPlaceUseCase
import com.example.backend.user.application.port.inbound.dto.SavedPlacesCommand
import com.example.backend.user.domain.model.SavedPlaceCategory
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** 인바운드 어댑터 */
@RequiresAppFeature(AppFeature.USER_PLACE)
@RestController
class SavedPlaceController(
    private val savedPlaceUseCase: SavedPlaceUseCase,
) {
    @PostMapping("/api/v1/saved-places")
    @ResponseStatus(HttpStatus.CREATED)
    @AccessTokenRequired
    fun save(
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: SavePlaceRequest,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<Nothing?> {
        if (mock) return ApiResponse.ok("장소가 저장되었습니다.")

        savedPlaceUseCase.save(userId, request.placeId)
        return ApiResponse.ok("장소가 저장되었습니다.")
    }

    @DeleteMapping("/api/v1/saved-places/{placeId}")
    @AccessTokenRequired
    fun unsave(
        @CurrentUserId userId: Long,
        @PathVariable placeId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<Nothing?> {
        if (mock) return ApiResponse.ok("장소 저장을 취소했습니다.")

        savedPlaceUseCase.unsave(userId, placeId)
        return ApiResponse.ok("장소 저장을 취소했습니다.")
    }

    /**
     * 모킹 API, 방문 상태를 저장하지 않고 성공 응답만 반환한다.
     * 방문 처리를 실제 구현할 때 saved_places.visited UPDATE와 함께 OS 색인 visited를
     * partial update + 실패 시 SQS 폴백으로 갱신해야 한다(안 하면 홈 근처 표시가 스테일).
     */
    @PatchMapping("/api/v1/saved-places/{placeId}")
    fun visit(
        @PathVariable placeId: Long,
    ): ApiResponse<Nothing?> = ApiResponse.ok("방문이 완료되었습니다.")

    @GetMapping("/api/v1/saved-places")
    fun list(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) visited: Boolean = false,
        @RequestParam(required = false) category: SavedPlaceCategory?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<SavedPlaceListResponse> {
        if (mock) return ApiResponse.success(SavedPlaceListResponse.mock())

        return ApiResponse.success(
            SavedPlaceListResponse.from(
                savedPlaceUseCase.getSavedPlaces(
                    SavedPlacesCommand(
                        userId = userId,
                        visited = visited,
                        // 포트 계약은 카테고리 이름 문자열이다(BFF 도 쓰는 계약) — 바인딩은 enum 으로 받아 잘못된 값을 400 으로 먼저 걸러낸다.
                        category = category?.name,
                        cursor = cursor,
                        size = size,
                    ),
                ),
            ),
        )
    }
}
