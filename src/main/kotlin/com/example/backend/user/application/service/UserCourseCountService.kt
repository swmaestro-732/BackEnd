package com.example.backend.user.application.service

import com.example.backend.user.application.port.inbound.UserCourseCountUseCase
import com.example.backend.user.application.port.outbound.UserPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 코스 개수 캐시 유지 유스케이스 구현. course 도메인의 쓰기 트랜잭션 안에서 인바운드 포트로 호출되어(REQUIRED 전파)
 * 코스 변경과 카운터 증감을 한 트랜잭션으로 묶는다.
 */
@Service
class UserCourseCountService(
    private val userPersistencePort: UserPersistencePort,
) : UserCourseCountUseCase {
    @Transactional
    override fun applyCourseCountDelta(
        userId: Long,
        publicDelta: Int,
        followerDelta: Int,
        privateDelta: Int,
    ) = userPersistencePort.applyCourseCountDelta(userId, publicDelta, followerDelta, privateDelta)
}
