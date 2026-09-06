package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.UserErrorCode
import com.example.backend.media.application.port.inbound.MediaCleanupUseCase
import com.example.backend.user.application.port.inbound.AccountUseCase
import com.example.backend.user.application.port.inbound.dto.FollowResult
import com.example.backend.user.application.port.inbound.dto.UpdateProfileCommand
import com.example.backend.user.application.port.inbound.dto.UserProfileResult
import com.example.backend.user.application.port.outbound.FollowPersistencePort
import com.example.backend.user.application.port.outbound.UserAreaPersistencePort
import com.example.backend.user.application.port.outbound.UserLikeThemePort
import com.example.backend.user.application.port.outbound.UserPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AccountService(
    private val userPersistencePort: UserPersistencePort,
    private val userAreaPersistencePort: UserAreaPersistencePort,
    private val followPersistencePort: FollowPersistencePort,
    private val userLikeThemePort: UserLikeThemePort,
    private val mediaCleanupUseCase: MediaCleanupUseCase,
    private val userAreaResolver: UserAreaResolver,
    private val userLikeThemeResolver: UserLikeThemeResolver,
) : AccountUseCase {
    override fun getProfile(userId: Long): UserProfileResult {
        val row =
            userPersistencePort.findProfile(userId)
                ?: throw BusinessException(UserErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: id=$userId")
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
            likeThemes = userLikeThemePort.findLikeThemes(userId),
        )
    }

    @Transactional
    override fun updateProfile(
        userId: Long,
        command: UpdateProfileCommand,
    ): UserProfileResult {
        val user =
            userPersistencePort.findById(userId)
                ?: throw BusinessException(UserErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: id=$userId")

        val nickname = command.nickname
        val handle = command.handle
        val profileImageUrl = command.profileImageUrl
        val bio = command.bio
        val likeThemes = command.likeThemes
        if (nickname != null &&
            nickname != user.nickname &&
            userPersistencePort.existsByNickname(nickname)
        ) {
            throw BusinessException(UserErrorCode.NICKNAME_ALREADY_TAKEN)
        }
        if (handle != null &&
            handle != user.handle &&
            userPersistencePort.existsByHandle(handle)
        ) {
            throw BusinessException(UserErrorCode.HANDLE_ALREADY_TAKEN)
        }

        // 관심 테마 검증 — 유효한 코스 카테고리가 아닌 값이 섞이면 update·미디어 정리 전에 거부한다(FK 없음 방어).
        val validatedLikeThemes = likeThemes?.let(userLikeThemeResolver::validate)

        val oldImageUrl = user.profileImageUrl
        val updated =
            user.updateProfile(
                nickname = nickname,
                handle = handle,
                profileImageUrl = profileImageUrl,
                bio = bio,
            )
        userPersistencePort.update(updated)
        // 관심 테마는 보낸 경우에만 전체 치환한다(null=미변경, 빈 배열=전체 해제).
        if (validatedLikeThemes != null) userLikeThemePort.replaceLikeThemes(userId, validatedLikeThemes)
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
        // 두 사용자 행을 FOR UPDATE 로 잠가 동시 탈퇴 정리와 직렬화한다(활성 검사도 잠금 아래에서 수행).
        val active = userPersistencePort.lockActive(listOf(followerId, targetId))
        if (targetId !in active) {
            throw BusinessException(UserErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: id=$targetId")
        }
        if (followerId !in active) {
            throw BusinessException(UserErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: id=$followerId")
        }
        followPersistencePort.follow(followerId, targetId)
        val followersCnt = userPersistencePort.findProfile(targetId)!!.followersCnt
        return FollowResult(isFollowing = true, followersCnt = followersCnt)
    }

    @Transactional
    override fun unfollow(
        followerId: Long,
        targetId: Long,
    ): FollowResult {
        // follow 와 같은 행 잠금으로 직렬화한다. 삭제는 멱등이지만 두 사용자 모두 활성일 때만 진행한다(follow 와 대칭).
        val active = userPersistencePort.lockActive(listOf(followerId, targetId))
        if (targetId !in active) {
            throw BusinessException(UserErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: id=$targetId")
        }
        if (followerId !in active) {
            throw BusinessException(UserErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: id=$followerId")
        }
        followPersistencePort.unfollow(followerId, targetId)
        val followersCnt = userPersistencePort.findProfile(targetId)!!.followersCnt
        return FollowResult(isFollowing = false, followersCnt = followersCnt)
    }
}
