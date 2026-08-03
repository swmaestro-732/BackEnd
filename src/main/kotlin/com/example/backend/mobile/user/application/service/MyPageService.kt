package com.example.backend.mobile.user.application.service

import com.example.backend.mobile.user.application.port.inbound.MyPageUseCase
import com.example.backend.mobile.user.application.port.inbound.dto.MyPageResult
import com.example.backend.mobile.user.application.port.outbound.MyPageCoursePort
import com.example.backend.mobile.user.application.port.outbound.MyPageProfilePort
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
    override fun getMyPage(userId: Long): MyPageResult {
        val profile = profilePort.getMyProfile(userId)
        // 내 코스 — viewerId==userId 라 발행 코스 전체가 통과한다(공개범위 무관).
        val courses = coursePort.listByAuthor(userId, viewerId = userId)
        return MyPageResult(profile, courses)
    }

    override fun getUserPage(
        handle: String,
        viewerId: Long?,
    ): MyPageResult {
        val profile = profilePort.getProfileByHandle(handle, viewerId)
        val courses = coursePort.listByAuthor(profile.id, viewerId)
        return MyPageResult(profile, courses)
    }
}
