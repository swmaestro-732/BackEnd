package com.example.backend.mobile.user.adapter.outbound

import com.example.backend.mobile.user.application.port.outbound.MyPageProfilePort
import com.example.backend.mobile.user.application.port.outbound.dto.ProfileSnapshot
import com.example.backend.user.application.port.inbound.AccountUseCase
import com.example.backend.user.application.port.inbound.UserUseCase
import com.example.backend.user.application.port.inbound.dto.UserProfileResult
import org.springframework.stereotype.Component

/**
 * BFF 아웃바운드 어댑터 — 프로필 조회를 user 도메인 인바운드 포트에 위임하고, 응답을 BFF 격리 DTO 로 매핑한다.
 * (지금은 인프로세스 위임. MSA 분리 시 이 어댑터만 user 서비스 HTTP 클라이언트로 교체한다.)
 */
@Component
class MyPageProfileAdapter(
    private val accountUseCase: AccountUseCase,
    private val userUseCase: UserUseCase,
) : MyPageProfilePort {
    override fun getMyProfile(userId: Long): ProfileSnapshot = accountUseCase.getProfile(userId).toSnapshot()

    override fun getProfileByHandle(
        handle: String,
        viewerId: Long?,
    ): ProfileSnapshot = userUseCase.getProfileByHandle(handle, viewerId).toSnapshot()

    private fun UserProfileResult.toSnapshot() =
        ProfileSnapshot(
            id = id,
            nickname = nickname,
            handle = handle,
            profileImageUrl = profileImageUrl,
            isFollowing = isFollowing,
            isFollower = isFollower,
            followersCnt = followersCnt,
            followingsCnt = followingsCnt,
        )
}
