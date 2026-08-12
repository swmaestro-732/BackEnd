package com.example.backend.course.application.port.inbound

/**
 * 인바운드 포트 — 코스 집계 카운터 갱신(크로스 도메인). 다른 도메인(user 저장함 등)이 코스의
 * 카운터를 갱신할 때 이 포트로만 접근한다. 코스 도메인이 자기 카운터의 소유자다([CourseQueryUseCase] 선례).
 */
interface CourseCounterUseCase {
    /**
     * 저장 수(saves_cnt)를 1 증가시키고 실제 갱신된 행 수를 반환한다 — 코스 저장 시 호출한다.
     * 미삭제(deleted_at IS NULL) 행에만 적용하므로, 0 이면 코스가 (동시 삭제 등으로) 활성 상태가 아님을 뜻한다.
     * 호출부는 0 일 때 저장을 원자적으로 롤백해 저장 행과 카운터 불일치를 막는다.
     */
    fun increaseSavesCount(courseId: Long): Int

    /** 저장 수(saves_cnt)를 1 감소시킨다 — 저장 취소 시 호출한다(실제 취소된 경우만). 정합성은 추후 비동기 집계로 옮긴다. */
    fun decreaseSavesCount(courseId: Long)

    /**
     * 따라가기 수(tracings_cnt)를 1 증가시키고 실제 갱신된 행 수를 반환한다 — 코스 완주 시 호출한다.
     * 미삭제(deleted_at IS NULL) 행에만 적용하므로, 0 이면 코스가 (동시 삭제 등으로) 활성 상태가 아님을 뜻한다.
     */
    fun increaseTracingsCount(courseId: Long): Int
}
