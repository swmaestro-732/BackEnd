package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.user.adapter.inbound.web.request.CreateUserRequest
import com.example.backend.user.adapter.inbound.web.response.AvailabilityResponse
import com.example.backend.user.adapter.inbound.web.response.FollowListResponse
import com.example.backend.user.adapter.inbound.web.response.UserProfileResponse
import com.example.backend.user.adapter.inbound.web.response.UserResponse
import com.example.backend.user.application.port.inbound.FollowQueryUseCase
import com.example.backend.user.application.port.inbound.UserUseCase
import com.example.backend.user.application.port.inbound.dto.FollowListCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — HTTP 요청을 인바운드 포트([UserUseCase]) 호출로 변환한다.
 * Request → Command, Result → Response 로 매핑해 도메인/애플리케이션 타입을 밖으로 노출하지 않는다.
 */
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userUseCase: UserUseCase,
    private val followQueryUseCase: FollowQueryUseCase,
    private val mockGuard: MockGuard,
) {
    @GetMapping
    fun list(): ApiResponse<List<UserResponse>> = ApiResponse.success(userUseCase.list().map(UserResponse::from))

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

    @GetMapping("/{userId}/followers")
    fun followers(
        @PathVariable userId: Long,
        @CurrentUserId viewerId: Long?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<FollowListResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(FollowListResponse.mock())

        return ApiResponse.success(
            FollowListResponse.from(
                followQueryUseCase.getFollowers(FollowListCommand(userId, viewerId, cursor, size)),
            ),
        )
    }

    @GetMapping("/{userId}/followings")
    fun followings(
        @PathVariable userId: Long,
        @CurrentUserId viewerId: Long?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<FollowListResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(FollowListResponse.mock())

        return ApiResponse.success(
            FollowListResponse.from(
                followQueryUseCase.getFollowings(FollowListCommand(userId, viewerId, cursor, size)),
            ),
        )
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateUserRequest,
    ): ApiResponse<UserResponse> = ApiResponse.success(UserResponse.from(userUseCase.create(request.toCommand())))
}
