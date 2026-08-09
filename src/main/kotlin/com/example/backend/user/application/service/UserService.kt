package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.user.application.port.inbound.UserUseCase
import com.example.backend.user.application.port.inbound.dto.UserProfileResult
import com.example.backend.user.application.port.outbound.CourseCleanupPort
import com.example.backend.user.application.port.outbound.FollowPersistencePort
import com.example.backend.user.application.port.outbound.RefreshTokenPort
import com.example.backend.user.application.port.outbound.SavedCoursePersistencePort
import com.example.backend.user.application.port.outbound.UserAreaPersistencePort
import com.example.backend.user.application.port.outbound.UserLikeThemePort
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
    private val savedCoursePersistencePort: SavedCoursePersistencePort,
    private val userAreaPersistencePort: UserAreaPersistencePort,
    private val userLikeThemePort: UserLikeThemePort,
    private val courseCleanupPort: CourseCleanupPort,
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
        // 이 사용자 행을 먼저 FOR UPDATE 로 잠가, 탈퇴 정리 도중 유입되는 이 사용자 대상 쓰기(팔로우·저장)를 직렬화한다.
        // (같은 행 잠금을 follow/save 도 획득 → 정리 후 유입분이 잔여 행·카운터를 남기지 못한다.)
        // 잠금 결과가 비면 그 사이 다른 트랜잭션이 이미 탈퇴시킨 것 → 활성 상태를 확인한 뒤에야 조회·정리한다(중복 탈퇴 방어).
        if (userId !in userPersistencePort.lockActive(listOf(userId))) {
            throw BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: id=$userId")
        }
        val user =
            userPersistencePort.findById(userId)
                ?: throw BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: id=$userId")

        // 소유 데이터를 먼저 정리해 (같은 소셜로) 재가입 시 "처음 계정처럼" 시작하게 한다 — 전 과정 단일 트랜잭션.
        // 1) 저장 코스: 원저자 saves_cnt 를 먼저 보정한 뒤 저장 레코드·폴더를 지운다.
        courseCleanupPort.decreaseSavesCounts(savedCoursePersistencePort.findAliveSavedCourseIds(userId))
        savedCoursePersistencePort.deleteAllByUser(userId)
        // 2) 팔로우: 양방향 삭제 + 상대 카운터 보정.
        followPersistencePort.purgeFollowsOf(userId)
        // 3) 관심 지역·테마: 빈 목록으로 전체 치환(=전부 삭제).
        userAreaPersistencePort.replaceAreas(userId, emptyList())
        userLikeThemePort.replaceLikeThemes(userId, emptyList())
        // 4) 작성 코스: 전부 소프트 삭제(피드·프로필에서 사라짐).
        courseCleanupPort.softDeleteCoursesByAuthor(userId)
        // 5) users 행: 탈퇴 스탬프 + 핸들 해제 + bio·카운터 리셋.
        userPersistencePort.softDelete(user.withdraw())
        // 6) 리프레시 토큰 폐기.
        refreshTokenPort.revokeAllByUser(userId)
    }

    private companion object {
        /** 사용 불가로 내려줄 예약 핸들(도메인 정책). */
        val RESERVED_HANDLES = setOf("admin", "courmy", "test")
    }
}
