package com.example.backend.course.application.port.inbound

/**
 * 인바운드 포트 — 코스 좋아요(course_likes) 토글 공개 API.
 *
 * 좋아요/취소는 코스 행 비관락(FOR UPDATE) 아래에서 카운터 증감을 직렬화한다(v1). 코스당 좋아요는 1개다.
 * 취소(unlike)는 멱등이다 — 좋아요돼 있지 않아도 성공으로 수렴한다(활성 코스가 전제).
 * 각 메서드는 연산 후의 코스 좋아요 수(likes_cnt)를 반환한다(응답 표기용).
 */
interface CourseLikeUseCase {
    /** (userId, courseId) 좋아요. 이미 좋아요한 코스면 중복으로 막는다(409). 반환은 증가 후 likes_cnt. */
    fun like(
        userId: Long,
        courseId: Long,
    ): Int

    /** (userId, courseId) 좋아요 취소. 좋아요돼 있지 않아도 성공한다(멱등). 반환은 (감소 후) likes_cnt. */
    fun unlike(
        userId: Long,
        courseId: Long,
    ): Int

    /** 탈퇴자의 코스 좋아요를 전부 삭제하고 대상 코스의 likes_cnt 를 보정한다(크로스 도메인 정리용). */
    fun purgeByUser(userId: Long)
}
