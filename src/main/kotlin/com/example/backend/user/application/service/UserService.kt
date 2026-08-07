package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.user.application.port.inbound.UserUseCase
import com.example.backend.user.application.port.inbound.dto.UserProfileResult
import com.example.backend.user.application.port.outbound.FollowPersistencePort
import com.example.backend.user.application.port.outbound.RefreshTokenPort
import com.example.backend.user.application.port.outbound.UserPersistencePort
import com.example.backend.user.application.port.outbound.UserProfileRow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 유스케이스 구현. 인바운드 포트([UserUseCase])를 구현하고,
 * 아웃바운드 포트([UserPersistencePort])에만 의존한다(구현 세부는 모른다).
 * 도메인 모델은 애플리케이션 밖으로 내보내지 않고 [UserProfileResult] 로 매핑한다.
 */
@Service
@Transactional(readOnly = true)
class UserService(
    private val userPersistencePort: UserPersistencePort,
    private val followPersistencePort: FollowPersistencePort,
    private val refreshTokenPort: RefreshTokenPort,
) : UserUseCase {
    override fun getProfile(
        userId: Long,
        viewerId: Long?,
    ): UserProfileResult {
        val row =
            userPersistencePort.findProfile(userId)
                ?: throw BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: id=$userId")
        return toProfileResult(
            row = row,
            isFollowing = viewerId?.let { followPersistencePort.isFollowing(it, userId) } ?: false,
            isFollower = viewerId?.let { followPersistencePort.isFollowing(userId, it) } ?: false,
        )
    }

    override fun getProfiles(
        userIds: List<Long>,
        viewerId: Long?,
    ): List<UserProfileResult> {
        val ids = userIds.distinct()
        if (ids.isEmpty()) return emptyList()
        val rows = userPersistencePort.findProfiles(ids)
        // 조회자 기준 팔로우 관계를 두 번의 배치 쿼리로 모은다(작성자별 isFollowing/isFollower N+1 회피).
        val following = viewerId?.let { followPersistencePort.filterFollowing(it, ids) } ?: emptySet()
        val followers = viewerId?.let { followPersistencePort.filterFollowers(it, ids) } ?: emptySet()
        return rows.map { row ->
            toProfileResult(
                row = row,
                isFollowing = row.id in following,
                isFollower = row.id in followers,
            )
        }
    }

    private fun toProfileResult(
        row: UserProfileRow,
        isFollowing: Boolean,
        isFollower: Boolean,
    ): UserProfileResult =
        UserProfileResult(
            id = row.id,
            nickname = row.nickname,
            handle = row.handle,
            profileImageUrl = row.profileImageUrl,
            bio = row.bio,
            isFollowing = isFollowing,
            isFollower = isFollower,
            followersCnt = row.followersCnt,
            followingsCnt = row.followingsCnt,
            coursesCnt = row.coursesCnt,
            publicCoursesCnt = row.publicCoursesCnt,
            followerCoursesCnt = row.followerCoursesCnt,
            privateCoursesCnt = row.privateCoursesCnt,
        )

    override fun getProfileByHandle(
        handle: String,
        viewerId: Long?,
    ): UserProfileResult {
        val user =
            userPersistencePort.findByHandle(handle)
                ?: throw BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: handle=$handle")
        val userId = checkNotNull(user.id) { "영속화된 User 는 id 를 가진다." }
        return getProfile(userId, viewerId)
    }

    override fun isHandleAvailable(handle: String): Boolean =
        handle.lowercase() !in RESERVED_HANDLES && !userPersistencePort.existsByHandle(handle)

    @Transactional
    override fun withdraw(userId: Long) {
        val user =
            userPersistencePort.findById(userId)
                ?: throw BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: id=$userId")
        userPersistencePort.softDelete(user.withdraw())
        refreshTokenPort.revokeAllByUser(userId)
    }

    private companion object {
        /** 사용 불가로 내려줄 예약 핸들(도메인 정책). */
        val RESERVED_HANDLES = setOf("admin", "courmy", "test")
    }
}
