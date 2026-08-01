package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.user.adapter.inbound.web.response.AvailabilityResponse
import com.example.backend.user.adapter.inbound.web.response.UserProfileResponse
import com.example.backend.user.application.port.inbound.UserUseCase
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — HTTP 요청을 인바운드 포트([UserUseCase]) 호출로 변환한다.
 * Request → Command, Result → Response 로 매핑해 도메인/애플리케이션 타입을 밖으로 노출하지 않는다.
 */
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userUseCase: UserUseCase,
    private val mockGuard: MockGuard,
) {
    @GetMapping("/{userId}")
    fun getProfile(
        @PathVariable userId: Long,
        @CurrentUserId viewerId: Long?,
    ): ApiResponse<UserProfileResponse> =
        ApiResponse.success(UserProfileResponse.from(userUseCase.getProfile(userId, viewerId)))

    /**
     * 핸들(아이디) 사용 가능 여부. `GET /api/v1/users/availability?handle=`.
     * 예약어이거나 이미 사용 중이면 available=false. (인증 플로우가 아니라 users 리소스 조회 — auth의 구 엔드포인트를 대체.)
     */
    @GetMapping("/availability")
    fun checkHandleAvailability(
        @RequestParam @NotBlank handle: String,
    ): ApiResponse<AvailabilityResponse> =
        ApiResponse.success(AvailabilityResponse(available = userUseCase.isHandleAvailable(handle)))

    /**
     * 회원 탈퇴 — 현재 로그인 사용자("나")를 소프트 삭제한다. `DELETE /api/v1/users`.
     * 대상은 JWT 의 나이므로 식별자 없이 컬렉션 경로에 둔다. 계정 리소스 액션이라 user 도메인에 둔다.
     */
    @DeleteMapping
    @AccessTokenRequired
    fun withdraw(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<Nothing?> {
        if (!(mock && mockGuard.isMockAllowed())) userUseCase.withdraw(userId)
        return ApiResponse.ok()
    }
}
