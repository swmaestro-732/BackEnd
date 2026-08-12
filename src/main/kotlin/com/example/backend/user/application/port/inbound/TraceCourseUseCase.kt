package com.example.backend.user.application.port.inbound

import com.example.backend.user.application.port.inbound.dto.TracingProgress
import com.example.backend.user.application.port.inbound.dto.TracingResult

/**
 * 인바운드 포트 — 코스 따라가기(tracing_courses) 시작·장소 체크인·진행 조회.
 *
 * 따라가기 레코드는 user 도메인 소유 데이터다. 시작 시 한 코스당 진행중(completed_at IS NULL) 트레이스는 1개로 막고(409),
 * 체크인은 코스에 담긴 장소만 허용한다(그 외 400). 체크인한 서로 다른 코스 장소 수가 코스 전체 장소 수와 같아지면
 * 자동 완주된다(completed_at 세팅 + courses.tracings_cnt 증가). course 접근은 ACL([CourseAccessPort])로만 한다.
 */
interface TraceCourseUseCase {
    /** (userId, courseId) 따라가기를 시작한다. 이미 진행중이면 409. */
    fun start(
        userId: Long,
        courseId: Long,
    ): TracingResult

    /** tracing 에 장소를 체크인한다(코스 소속 장소만). 마지막 장소면 자동 완주한다. 이미 완주면 no-op(멱등). */
    fun checkInPlace(
        userId: Long,
        tracingId: Long,
        placeId: Long,
    ): TracingProgress

    /** tracing 진행 상태를 조회한다. */
    fun getProgress(
        userId: Long,
        tracingId: Long,
    ): TracingProgress
}
