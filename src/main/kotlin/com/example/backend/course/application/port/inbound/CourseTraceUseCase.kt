package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.CheckInPlaceCommand
import com.example.backend.course.application.port.inbound.dto.CourseTraceResult
import com.example.backend.course.application.port.inbound.dto.EndCourseTraceCommand
import java.time.Instant

/**
 * 인바운드 포트 — 코스 따라가기 시작·장소 체크인·종료와 완주 조회.
 * 따라가기 기록(tracing_courses)은 course 도메인 소유 데이터다. 다른 도메인(user 저장함·mobile 화면)이 완주 여부를 필요로 하면 이 포트로만 접근한다.
 */
interface CourseTraceUseCase {
    /**
     * 따라가기 시작. 볼 수 없는(비활성·비공개·미팔로우) 코스는 404.
     * 같은 코스에 진행 중 세션이 있으면 새로 만들지 않고 그 세션(체크인 포함)을 돌려준다 — 앱을 껐다 켠 뒤 이어가기.
     */
    fun start(
        userId: Long,
        courseId: Long,
    ): CourseTraceResult

    /** 장소 체크인. 내 세션이 아니거나 없으면 404, 이미 종료된 세션이면 409, 코스에 없는 장소면 400. 같은 장소 재전송은 무시한다. */
    fun checkIn(command: CheckInPlaceCommand)

    /** 따라가기 종료. 내 세션이 아니거나 없으면 404, 이미 종료된 세션이면 409, 병합할 장소가 코스에 없으면 400. 이때만 코스 tracings_cnt 를 올린다. */
    fun end(command: EndCourseTraceCommand)

    /** 주어진 코스들 중 사용자가 완주한 코스의 완주 시각(가장 이른 종료)을 courseId 별로 반환한다. 완주하지 않은 코스는 결과에 없다. */
    fun findCompletedAt(
        userId: Long,
        courseIds: List<Long>,
    ): Map<Long, Instant>

    /** 사용자가 한 번이라도 완주한 코스 id 집합 — 저장함 완주 필터·완주 수에 쓴다. */
    fun findTracedCourseIds(userId: Long): Set<Long>
}
