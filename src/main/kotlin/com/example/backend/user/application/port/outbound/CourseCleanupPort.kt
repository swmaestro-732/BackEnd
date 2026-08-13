package com.example.backend.user.application.port.outbound

/**
 * 아웃바운드 포트 — 회원 탈퇴 시 필요한 코스 도메인 정리(크로스 도메인).
 * user 애플리케이션은 이 포트로만 course 에 접근하고, 어댑터가 course 인바운드 포트에 위임한다.
 * MSA 분리 시 어댑터만 course 서비스 클라이언트로 교체한다.
 */
interface CourseCleanupPort {
    /** 작성자의 살아있는 코스를 전부 소프트 삭제한다(탈퇴 정리). */
    fun softDeleteCoursesByAuthor(authorId: Long)

    /** 주어진 코스들의 저장 수(saves_cnt)를 1씩 감소시킨다(탈퇴자 저장 취소 반영). */
    fun decreaseSavesCounts(courseIds: List<Long>)
}
