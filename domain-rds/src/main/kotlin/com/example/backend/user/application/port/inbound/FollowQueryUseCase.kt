package com.example.backend.user.application.port.inbound

import com.example.backend.user.application.port.inbound.dto.FollowListCommand
import com.example.backend.user.application.port.inbound.dto.FollowListResult

/**
 * 인바운드 포트 — 팔로워/팔로잉 목록 조회.
 * 팔로우/언팔로우(쓰기)는 [MyUseCase] 소관이고, 여기서는 목록 조회(읽기)만 담당한다.
 */
interface FollowQueryUseCase {
    /** targetUserId 를 팔로우하는 사용자(팔로워) 목록. */
    fun getFollowers(command: FollowListCommand): FollowListResult

    /** targetUserId 가 팔로우하는 사용자(팔로잉) 목록. */
    fun getFollowings(command: FollowListCommand): FollowListResult
}
