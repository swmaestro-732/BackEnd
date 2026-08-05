package com.example.backend.mobile.user.adapter.inbound.web

import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.mobile.user.adapter.inbound.web.response.MyPageScreenResponse
import com.example.backend.mobile.user.application.port.inbound.MyPageUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 마이페이지 **화면 조합 API** (BFF).
 * 프로필 + 그 사용자의 공개 코스 목록을 한 번에 내려준다. 조합은 인바운드 포트([MyPageUseCase])가 담당하고,
 * 컨트롤러는 Request → 포트 호출 → Response 매핑만 한다.
 *
 * 시드/DB 없이 프론트가 붙어볼 수 있도록 `?mock=true` 면 조회 없이 고정 목([MyPageScreenResponse.MOCK])을 반환한다.
 */
@RestController
@RequestMapping("/service/v1")
class MyPageScreenController(
    private val myPageUseCase: MyPageUseCase,
    private val mockGuard: MockGuard,
) {
    /** 내 마이페이지 — 내 프로필 + 내 발행 코스 전체. */
    @GetMapping("/mypage")
    @AccessTokenRequired
    fun getMyPage(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<MyPageScreenResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(MyPageScreenResponse.MOCK)
        return ApiResponse.success(
            MyPageScreenResponse.from(myPageUseCase.getMyPage(userId, cursor, PAGE_SIZE)),
        )
    }

    /** 타인 마이페이지 — 대상 프로필 + 조회자 기준 공개 코스. 없는 handle 은 404 USER_NOT_FOUND. */
    @GetMapping("/mypage/{handle}")
    fun getUserPage(
        @PathVariable handle: String,
        @CurrentUserId viewerId: Long?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<MyPageScreenResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(MyPageScreenResponse.MOCK)
        return ApiResponse.success(
            MyPageScreenResponse.from(myPageUseCase.getUserPage(handle, viewerId, cursor, PAGE_SIZE)),
        )
    }

    private companion object {
        const val PAGE_SIZE = 10
    }
}
