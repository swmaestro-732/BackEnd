package com.example.backend.mobile.user.application.service

import com.example.backend.mobile.user.application.MyPageCursorCodec
import com.example.backend.mobile.user.application.port.inbound.MyPageUseCase
import com.example.backend.mobile.user.application.port.inbound.dto.MyPageResult
import com.example.backend.mobile.user.application.port.outbound.MyPageCoursePort
import com.example.backend.mobile.user.application.port.outbound.MyPageProfilePort
import com.example.backend.mobile.user.application.port.outbound.dto.AuthoredCourseCursor
import com.example.backend.mobile.user.application.port.outbound.dto.AuthoredCoursePage
import com.example.backend.mobile.user.application.port.outbound.dto.CourseCounts
import com.example.backend.mobile.user.application.port.outbound.dto.ProfileSnapshot
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 마이페이지 화면 조합 서비스 (BFF). 자신의 아웃바운드 포트만 호출해 한 화면 응답 재료를 만든다 —
 * 프로필([MyPageProfilePort]) + 그 사용자의 발행 코스([MyPageCoursePort]). 타 도메인 인바운드에 직접 의존하지 않아
 * MSA 분리 시 어댑터만 교체하면 된다. 조합 한 번을 한 읽기 트랜잭션으로 묶는다.
 */
@Service
@Transactional(readOnly = true)
class MyPageService(
    private val profilePort: MyPageProfilePort,
    private val coursePort: MyPageCoursePort,
) : MyPageUseCase {
    override fun getMyPage(
        userId: Long,
        cursor: String?,
        size: Int,
    ): MyPageResult {
        val profile = profilePort.getMyProfile(userId)
        // 내 페이지는 전부 공개(마스킹 없음). 개수는 프로필과 함께 읽은 저장 캐시에서 온다(추가 쿼리 없음).
        val counts = profile.courseCounts()
        val page =
            coursePort.listByAuthor(
                authorId = userId,
                viewerId = userId,
                cursor = cursor,
                size = size,
            )
        return toResult(profile, counts, page)
    }

    override fun getUserPage(
        handle: String,
        viewerId: Long?,
        cursor: String?,
        size: Int,
    ): MyPageResult {
        val profile = profilePort.getProfileByHandle(handle, viewerId)
        // 개수는 프로필과 함께 읽은 저장 캐시에서 오며, 조회자 관계에 따라 마스킹한다(추가 쿼리 없음).
        val counts = profile.courseCounts().maskFor(profile, viewerId)
        val page =
            coursePort.listByAuthor(
                authorId = profile.id,
                viewerId = viewerId,
                cursor = cursor,
                size = size,
            )
        return toResult(profile, counts, page)
    }

    private fun toResult(
        profile: ProfileSnapshot,
        counts: CourseCounts,
        page: AuthoredCoursePage,
    ): MyPageResult {
        val nextCursor =
            if (page.hasNext) {
                page.courses.last().let {
                    MyPageCursorCodec.encode(AuthoredCourseCursor(createdAt = it.createdAt, id = it.id))
                }
            } else {
                null
            }
        return MyPageResult(
            profile = profile,
            courseCounts = counts,
            courses = page.courses,
            nextCursor = nextCursor,
            hasNext = page.hasNext,
        )
    }

    /** 프로필과 함께 읽어온 공개범위별 코스 개수 캐시를 BFF 카운트 DTO 로 옮긴다. */
    private fun ProfileSnapshot.courseCounts(): CourseCounts =
        CourseCounts(
            publicCount = publicCoursesCnt,
            followerCount = followerCoursesCnt,
            privateCount = privateCoursesCnt,
        )

    /** 타인 페이지에서는 관계에 따라 공개범위별 개수를 마스킹한다. 자신의 handle 조회는 내 페이지와 동일하게 전부 공개한다. */
    private fun CourseCounts.maskFor(
        profile: ProfileSnapshot,
        viewerId: Long?,
    ): CourseCounts =
        if (viewerId == profile.id) {
            this
        } else {
            copy(
                // 팔로워 공개 코스는 "조회자가 대상을 팔로우할 때"(isFollowing = 나→대상) 보인다.
                // isFollower(대상→나)가 아니다 — 관계 방향을 반대로 쓰면 실제 팔로워에게 0을 주고 아닌 사람에게 노출된다.
                followerCount = if (profile.isFollowing) followerCount else 0,
                privateCount = 0,
            )
        }
}
