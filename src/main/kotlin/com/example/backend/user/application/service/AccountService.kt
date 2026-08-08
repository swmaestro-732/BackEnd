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
import com.example.backend.user.application.port.outbound.UserLikeTagPort
import com.example.backend.user.application.port.outbound.UserPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AccountService(
    private val userPersistencePort: UserPersistencePort,
    private val userAreaPersistencePort: UserAreaPersistencePort,
    private val followPersistencePort: FollowPersistencePort,
    private val userLikeTagPort: UserLikeTagPort,
    private val mediaCleanupUseCase: MediaCleanupUseCase,
    private val userAreaResolver: UserAreaResolver,
    private val userLikeTagResolver: UserLikeTagResolver,
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
            bio = row.bio,
            isFollowing = false,
            isFollower = false,
            followersCnt = row.followersCnt,
            followingsCnt = row.followingsCnt,
            publicCoursesCnt = row.publicCoursesCnt,
            followerCoursesCnt = row.followerCoursesCnt,
            privateCoursesCnt = row.privateCoursesCnt,
            areas = userAreaResolver.resolve(userAreaPersistencePort.findAreaCodes(userId)),
            likeTags = userLikeTagResolver.resolve(userLikeTagPort.findLikeTagIds(userId)),
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
        val bio = command.bio
        val likeTagIds = command.likeTagIds
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

        // 관심 테마(코스 태그) 존재 검증 — 없는 id 가 하나라도 있으면 update·미디어 정리 전에 거부한다(FK 없음 방어).
        val validatedLikeTagIds = likeTagIds?.let(userLikeTagResolver::validate)

        val oldImageUrl = user.profileImageUrl
        val updated =
            user.updateProfile(
                nickname = nickname,
                handle = handle,
                profileImageUrl = profileImageUrl,
                bio = bio,
            )
        userPersistencePort.update(updated)
        // 관심 테마(코스 태그)는 보낸 경우에만 전체 치환한다(null=미변경, 빈 배열=전체 해제).
        if (validatedLikeTagIds != null) userLikeTagPort.replaceLikeTags(userId, validatedLikeTagIds)
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
