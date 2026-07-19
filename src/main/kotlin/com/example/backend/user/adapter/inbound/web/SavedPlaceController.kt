package com.example.backend.user.adapter.inbound.web

import com.example.backend.common.mock.MockErrors
import com.example.backend.common.response.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 저장 장소(노션 명세 · User · user-place).
 *
 * - [save] 장소 저장(`POST /api/v1/my/saved-places/{placeId}`): **모킹 API**.
 *   실제 구현 시 인바운드 포트(UseCase) 연동으로 교체하고 [MockErrors] 호출을 제거한다.
 *
 * `mockError` 파라미터로 모킹 에러를 주입할 수 있다(예: `?mockError=4040`).
 */
@RestController
@RequestMapping("/api/v1/my/saved-places")
class SavedPlaceController {
    @PostMapping("/{placeId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun save(
        @PathVariable placeId: Long,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<Nothing?> {
        MockErrors.throwIfRequested(mockError)
        return ApiResponse.ok("장소가 저장되었습니다.")
    }
}
