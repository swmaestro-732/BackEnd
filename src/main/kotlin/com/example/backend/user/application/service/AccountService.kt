package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.media.application.port.inbound.MediaCleanupUseCase
import com.example.backend.user.application.port.inbound.AccountUseCase
import com.example.backend.user.application.port.inbound.dto.FollowResult
import com.example.backend.user.application.port.inbound.dto.UpdateProfileCommand
import com.example.backend.user.application.port.inbound.dto.UserProfileResult
import com.example.backend.user.application.port.outbound.FollowPersistencePort
import com.example.backend.user.application.port.outbound.UserAreaPersistencePort
import com.example.backend.user.application.port.outbound.UserPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AccountService(
    private val userPersistencePort: UserPersistencePort,
    private val userAreaPersistencePort: UserAreaPersistencePort,
    private val followPersistencePort: FollowPersistencePort,
    private val mediaCleanupUseCase: MediaCleanupUseCase,
    private val userAreaResolver: UserAreaResolver,
) : AccountUseCase {
    override fun getProfile(userId: Long): UserProfileResult {
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
            areas = userAreaResolver.resolve(userAreaPersistencePort.findAreaCodes(userId)),
        )
    }

    @Transactional
    override fun updateProfile(
        userId: Long,
        command: UpdateProfileCommand,
    ): UserProfileResult {
        val user =
            userPersistencePort.findById(userId)
                ?: throw BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: id=$userId")

        val nickname = command.nickname
        val handle = command.handle
        val profileImageUrl = command.profileImageUrl
        if (nickname != null &&
            nickname != user.nickname &&
            userPersistencePort.existsByNickname(nickname)
        ) {
            throw BusinessException(ErrorCode.NICKNAME_ALREADY_TAKEN)
        }
        if (handle != null &&
            handle != user.handle &&
            userPersistencePort.existsByHandle(handle)
        ) {
            throw BusinessException(ErrorCode.HANDLE_ALREADY_TAKEN)
        }

        val oldImageUrl = user.profileImageUrl
        val updated = user.updateProfile(nickname, handle, profileImageUrl)
        userPersistencePort.update(updated)
        // 프로필 이미지가 새 값으로 교체되면 참조 끊긴 옛 이미지(고아)를 정리한다(재사용 함수).
        if (profileImageUrl != null && profileImageUrl != oldImageUrl) {
            mediaCleanupUseCase.deleteByUrl(oldImageUrl)
        }
        // 관심 지역도 부분 수정 시맨틱 — null 은 유지, 빈 리스트는 전체 삭제(전체 치환).
        command.areaCodes?.let {
            userAreaPersistencePort.replaceAreas(userId, userAreaResolver.normalizeAndValidate(it))
        }
        return getProfile(userId)
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
