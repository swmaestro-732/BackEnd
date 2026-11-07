package com.example.backend.course.application.service

import com.example.backend.course.application.port.inbound.CourseCounterUseCase
import com.example.backend.course.application.port.inbound.CourseLikeUseCase
import com.example.backend.course.application.port.inbound.CourseTraceUseCase
import com.example.backend.course.application.port.inbound.CourseUseCase
import com.example.backend.course.application.port.inbound.PlanUseCase
import com.example.backend.course.application.port.inbound.UserWithdrawalCleanupUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 탈퇴 사용자 정리(예전 user 의 CourseCleanupAdapter 가 하던 일). 호출한 쪽 트랜잭션에 참여한다(REQUIRED):
 * 탈퇴 이벤트를 커밋 직전에 받아 처리하므로 user 쪽 정리와 한 트랜잭션으로 묶인다.
 */
@Service
class UserWithdrawalCleanupService(
    private val courseCounterUseCase: CourseCounterUseCase,
    private val courseLikeUseCase: CourseLikeUseCase,
    private val courseTraceUseCase: CourseTraceUseCase,
    private val courseUseCase: CourseUseCase,
    private val planUseCase: PlanUseCase,
) : UserWithdrawalCleanupUseCase {
    @Transactional
    override fun cleanUpWithdrawnUser(
        userId: Long,
        savedCourseIds: List<Long>,
    ) {
        // 저장했던 코스의 saves_cnt 를 되돌린다.
        savedCourseIds.forEach(courseCounterUseCase::decreaseSavesCount)
        // 좋아요 레코드를 지우고 대상 코스 likes_cnt 를 보정한다.
        courseLikeUseCase.purgeByUser(userId)
        // 따라가기 세션과 체크인(위치 이력)을 지운다. tracings_cnt 는 누적 통계라 되돌리지 않는다.
        courseTraceUseCase.purgeByUser(userId)
        // 작성 코스와 계획을 소프트 삭제한다(피드, 프로필, 내 계획 목록에서 사라짐).
        courseUseCase.deleteAllByAuthor(userId)
        planUseCase.deleteAllByOwner(userId)
    }
}
