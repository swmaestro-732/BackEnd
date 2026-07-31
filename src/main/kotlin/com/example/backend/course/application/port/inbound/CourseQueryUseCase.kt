package com.example.backend.course.application.port.inbound

/**
 * 인바운드 포트 — 코스 조회(크로스 도메인). 다른 도메인(user 저장함 등)이 코스 존재 여부를
 * 확인할 때 이 포트로만 접근한다. 도메인 모델을 경계 밖으로 노출하지 않는다([PlaceQueryUseCase] 선례).
 */
interface CourseQueryUseCase {
    /** 미삭제(deleted_at IS NULL) 코스가 존재하는지 확인한다. */
    fun existsById(courseId: Long): Boolean
}
