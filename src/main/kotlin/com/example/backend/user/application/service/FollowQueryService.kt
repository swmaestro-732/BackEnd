package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.user.application.port.inbound.FollowQueryUseCase
import com.example.backend.user.application.port.inbound.dto.FollowListCommand
import com.example.backend.user.application.port.inbound.dto.FollowListResult
import com.example.backend.user.application.port.outbound.FollowPersistencePort
import com.example.backend.user.application.port.outbound.FollowUserRow
import com.example.backend.user.application.port.outbound.UserPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 팔로워/팔로잉 목록 조회 유스케이스 — 인바운드 포트([FollowQueryUseCase]) 구현.
 *
 * 대상 사용자 존재 검증과 totalCount 를 [UserPersistencePort.findProfile] 한 번으로 처리하고
 * (없으면 404), follows 레코드를 최신 팔로우순(id 내림차순)으로 커서 페이지네이션한다.
 * 각 항목의 isFollowing/isFollower 는 조회자 기준으로 배치 조회([FollowPersistencePort.filterFollowing]
 * /filterFollowers)해 N+1 을 피한다([UserService.getProfiles] 와 동일 방식).
 */
@Service
@Transactional(readOnly = true)
class FollowQueryService(
    private val followPersistencePort: FollowPersistencePort,
    private val userPersistencePort: UserPersistencePort,
) : FollowQueryUseCase {
    override fun getFollowers(command: FollowListCommand): FollowListResult {
        val profile = findProfileOrThrow(command.targetUserId)
        return buildResult(command, totalCount = profile.followersCnt.toLong()) { cursorId, limit ->
            followPersistencePort.findFollowers(command.targetUserId, cursorId, limit)
        }
    }

    override fun getFollowings(command: FollowListCommand): FollowListResult {
        val profile = findProfileOrThrow(command.targetUserId)
        return buildResult(command, totalCount = profile.followingsCnt.toLong()) { cursorId, limit ->
            followPersistencePort.findFollowings(command.targetUserId, cursorId, limit)
        }
    }

    private fun findProfileOrThrow(targetUserId: Long) =
        userPersistencePort.findProfile(targetUserId)
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: id=$targetUserId")

    /** 커서 디코드 → +1 조회로 hasNext 판정 → 조회자 기준 관계 배선 → 결과 조립. 팔로워/팔로잉 공통 로직. */
    private fun buildResult(
        command: FollowListCommand,
        totalCount: Long,
        fetchPage: (cursorId: Long?, limit: Int) -> List<FollowUserRow>,
    ): FollowListResult {
        val cursorId = command.cursor?.let(::decodeCursor)
        val rows = fetchPage(cursorId, command.size + 1)
        val hasNext = rows.size > command.size
        val page = rows.take(command.size)

        val ids = page.map { it.userId }
        val following = command.viewerId?.let { followPersistencePort.filterFollowing(it, ids) } ?: emptySet()
        val followers = command.viewerId?.let { followPersistencePort.filterFollowers(it, ids) } ?: emptySet()

        return FollowListResult(
            totalCount = totalCount,
            nextCursor = if (hasNext) page.lastOrNull()?.followId?.toString() else null,
            hasNext = hasNext,
            users =
                page.map { row ->
                    FollowListResult.FollowUserItem(
                        id = row.userId,
                        nickname = row.nickname,
                        handle = row.handle,
                        profileImageUrl = row.profileImageUrl,
                        isFollowing = row.userId in following,
                        isFollower = row.userId in followers,
                    )
                },
        )
    }

    /** 커서(=follows 레코드 id)를 파싱한다. 형식이 잘못되면 400. */
    private fun decodeCursor(cursor: String): Long =
        cursor.toLongOrNull()
            ?: throw BusinessException(ErrorCode.INVALID_INPUT, "잘못된 커서입니다: $cursor")
}
