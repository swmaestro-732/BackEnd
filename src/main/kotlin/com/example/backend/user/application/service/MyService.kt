package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.user.application.port.inbound.MyUseCase
import com.example.backend.user.application.port.inbound.dto.FollowResult
import com.example.backend.user.application.port.inbound.dto.UpdateProfileCommand
import com.example.backend.user.application.port.inbound.dto.UserProfileResult
import com.example.backend.user.application.port.outbound.FollowPersistencePort
import com.example.backend.user.application.port.outbound.RefreshTokenPort
import com.example.backend.user.application.port.outbound.UserPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MyService(
    private val userPersistencePort: UserPersistencePort,
    private val followPersistencePort: FollowPersistencePort,
    private val refreshTokenPort: RefreshTokenPort,
) : MyUseCase {
    override fun getMyProfile(userId: Long): UserProfileResult {
        val row =
            userPersistencePort.findProfile(userId)
                ?: throw BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: id=$userId")
        return UserProfileResult(
            id = row.id,
            nickname = row.nickname,
            handle = row.handle,
            profileImageUrl = row.profileImageUrl,
            isFollowing = false,
            isFollower = false,
            followersCnt = row.followersCnt,
            followingsCnt = row.followingsCnt,
            coursesCnt = row.coursesCnt,
        )
    }

    @Transactional
    override fun updateMyProfile(
        userId: Long,
        command: UpdateProfileCommand,
    ): UserProfileResult {
        val user =
            userPersistencePort.findById(userId)
                ?: throw BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: id=$userId")

        if (command.nickname != null &&
            command.nickname != user.nickname &&
            userPersistencePort.existsByNickname(command.nickname)
        ) {
            throw BusinessException(ErrorCode.NICKNAME_ALREADY_TAKEN)
        }
        if (command.handle != null &&
            command.handle != user.handle &&
            userPersistencePort.existsByHandle(command.handle)
        ) {
            throw BusinessException(ErrorCode.HANDLE_ALREADY_TAKEN)
        }

        val updated = user.updateProfile(command.nickname, command.handle, command.profileImageUrl)
        userPersistencePort.update(updated)
        return getMyProfile(userId)
    }

    @Transactional
    override fun withdraw(userId: Long) {
        userPersistencePort.softDelete(userId)
        refreshTokenPort.revokeAllByUser(userId)
    }

    @Transactional
    override fun follow(
        followerId: Long,
        targetId: Long,
    ): FollowResult {
        require(followerId != targetId) { "자기 자신은 팔로우할 수 없습니다." }
        userPersistencePort.findById(targetId)
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: id=$targetId")
        followPersistencePort.follow(followerId, targetId)
        val followersCnt = userPersistencePort.findProfile(targetId)!!.followersCnt
        return FollowResult(isFollowing = true, followersCnt = followersCnt)
    }

    @Transactional
    override fun unfollow(
        followerId: Long,
        targetId: Long,
    ): FollowResult {
        userPersistencePort.findById(targetId)
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: id=$targetId")
        followPersistencePort.unfollow(followerId, targetId)
        val followersCnt = userPersistencePort.findProfile(targetId)!!.followersCnt
        return FollowResult(isFollowing = false, followersCnt = followersCnt)
    }
}
