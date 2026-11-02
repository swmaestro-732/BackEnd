package com.example.backend.course.application.port.inbound

/** 탈퇴한 사용자와 관련된 course 데이터를 정리한다. user 의 탈퇴 이벤트를 받은 course 어댑터가 호출한다. */
interface UserWithdrawalCleanupUseCase {
    fun cleanUpWithdrawnUser(
        userId: Long,
        savedCourseIds: List<Long>,
    )
}
