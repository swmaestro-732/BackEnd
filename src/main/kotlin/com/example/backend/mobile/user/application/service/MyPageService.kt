package com.example.backend.mobile.user.application.service

import com.example.backend.mobile.user.application.port.inbound.MyPageUseCase
import com.example.backend.mobile.user.application.port.inbound.dto.MyPageResult
import com.example.backend.user.application.port.inbound.AccountUseCase
import com.example.backend.user.application.port.inbound.UserUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 마이페이지 화면 조합 서비스 (BFF). 도메인 인바운드 포트만 호출해 한 화면 응답 재료를 만든다 —
 * 프로필(user) + 그 사용자의 코스 목록. 조합 한 번을 한 읽기 트랜잭션으로 묶는다.
 */
@Service
@Transactional(readOnly = true)
class MyPageService(
    private val accountUseCase: AccountUseCase,
    private val userUseCase: UserUseCase,
) : MyPageUseCase {
    override fun getMyPage(userId: Long): MyPageResult {
        val profile = accountUseCase.getProfile(userId)
        // 코스 목록은 course 도메인 담당자의 작성자별 코스 조회(listByAuthor) 구현 후 연결한다(담당자 이관). 현재는 빈 목록.
        return MyPageResult(profile, courses = emptyList())
    }

    override fun getUserPage(
        handle: String,
        viewerId: Long?,
    ): MyPageResult {
        val profile = userUseCase.getProfileByHandle(handle, viewerId)
        // 코스 목록은 course 도메인 담당자의 작성자별 코스 조회(listByAuthor) 구현 후 연결한다(담당자 이관). 현재는 빈 목록.
        return MyPageResult(profile, courses = emptyList())
    }
}
