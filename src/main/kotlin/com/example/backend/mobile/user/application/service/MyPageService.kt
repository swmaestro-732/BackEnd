package com.example.backend.mobile.user.application.service

import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.mobile.user.application.port.inbound.MyPageUseCase
import com.example.backend.mobile.user.application.port.inbound.dto.MyPageResult
import com.example.backend.user.application.port.inbound.AccountUseCase
import com.example.backend.user.application.port.inbound.UserUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 마이페이지 화면 조합 서비스 (BFF). 도메인 인바운드 포트만 호출해 한 화면 응답 재료를 만든다 —
 * 프로필(user) + 그 사용자의 발행 코스([CourseQueryUseCase.listByAuthor]). 조합 한 번을 한 읽기 트랜잭션으로 묶는다.
 */
@Service
@Transactional(readOnly = true)
class MyPageService(
    private val accountUseCase: AccountUseCase,
    private val userUseCase: UserUseCase,
    private val courseQueryUseCase: CourseQueryUseCase,
) : MyPageUseCase {
    override fun getMyPage(userId: Long): MyPageResult {
        val profile = accountUseCase.getProfile(userId)
        // 내 코스 — viewerId==userId 라 발행 코스 전체가 통과한다(공개범위 무관).
        val courses = courseQueryUseCase.listByAuthor(userId, viewerId = userId)
        return MyPageResult(profile, courses)
    }

    override fun getUserPage(
        handle: String,
        viewerId: Long?,
    ): MyPageResult {
        val profile = userUseCase.getProfileByHandle(handle, viewerId)
        val courses = courseQueryUseCase.listByAuthor(profile.id, viewerId)
        return MyPageResult(profile, courses)
    }
}
