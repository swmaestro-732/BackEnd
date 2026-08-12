package com.example.backend.user.application.port.outbound

/**
 * 아웃바운드 포트 — user 도메인이 course 도메인에 필요한 것(존재 검증·저장 카운터 갱신)을 요청하는 경계.
 *
 * user 코어는 course 를 직접 알지 않고 이 포트에만 의존한다(MSA 분리 대비). 실제 course 인바운드 포트
 * 호출은 ACL 어댑터([com.example.backend.user.adapter.outbound.course.CourseAccessAdapter])가 담당하며,
 * 도메인 분리 시 그 어댑터만 REST/이벤트 클라이언트로 교체하면 코어는 무변경이다.
 */
interface CourseAccessPort {
    /** 코스가 존재(활성)하는지. 저장 대상 코스 검증에 쓴다. */
    fun existsCourse(courseId: Long): Boolean

    /**
     * 코스 저장 수(saves_cnt)를 1 증가시키고 실제 갱신된 행 수를 반환한다.
     * 0 이면 코스가 (동시 삭제 등으로) 활성이 아님 → 호출부가 저장을 롤백한다.
     */
    fun increaseSavesCount(courseId: Long): Int

    /** 코스 저장 수(saves_cnt)를 1 감소시킨다 — 실제 저장 취소가 일어난 경우만 호출한다. */
    fun decreaseSavesCount(courseId: Long)

    /** 코스에 담긴 place_id 목록 — 따라가기 체크인 대상(코스 소속 장소) 검증용. */
    fun getCoursePlaceIds(courseId: Long): List<Long>

    /**
     * 코스 따라가기 수(tracings_cnt)를 1 증가시키고 실제 갱신된 행 수를 반환한다 — 코스 완주 시 호출한다.
     * 0 이면 코스가 (동시 삭제 등으로) 활성이 아님.
     */
    fun increaseTracingsCount(courseId: Long): Int
}
