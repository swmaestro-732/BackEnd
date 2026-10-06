package com.example.backend.course.application.port.outbound

/**
 * 아웃바운드 포트 — 코스 좋아요(course_likes) 접근.
 * 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다.
 */
interface CourseLikePersistencePort {
    /** (user_id, course_id) 좋아요가 이미 있는지. 중복 좋아요 차단(409)에 쓴다. */
    fun existsLike(
        userId: Long,
        courseId: Long,
    ): Boolean

    /** 좋아요 레코드를 삽입한다. */
    fun insert(
        userId: Long,
        courseId: Long,
    )

    /** (user_id, course_id) 좋아요를 하드 삭제한다. 실제 삭제된 행이 있으면 true(없으면 false, 멱등). */
    fun deleteByUserAndCourse(
        userId: Long,
        courseId: Long,
    ): Boolean

    /** 사용자의 좋아요를 전부 하드 삭제하고, 실제로 삭제된 course_id 들을 반환한다(탈퇴 정리용). */
    fun deleteAllByUser(userId: Long): List<Long>
}
