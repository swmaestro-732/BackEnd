package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.CheckInPlaceCommand
import com.example.backend.course.application.port.inbound.dto.CourseTraceDetailResult
import com.example.backend.course.application.port.inbound.dto.CourseTraceResult
import com.example.backend.course.application.port.inbound.dto.CourseTracesQuery
import com.example.backend.course.application.port.inbound.dto.CourseTracesResult
import com.example.backend.course.application.port.inbound.dto.EndCourseTraceCommand
import com.example.backend.course.application.port.inbound.dto.TraceRouteResult
import java.time.Instant

/**
 * 인바운드 포트 — 코스 따라가기 시작·장소 체크인·종료와 완주 조회.
 * 따라가기 기록(tracing_courses)은 course 도메인 소유 데이터다. 다른 도메인(user 저장함·mobile 화면)이 완주 여부를 필요로 하면 이 포트로만 접근한다.
 */
interface CourseTraceUseCase {
    /**
     * 따라가기 시작. 볼 수 없는(비활성·비공개·미팔로우) 코스는 404.
     * 같은 코스에 진행 중 세션이 있으면 새로 만들지 않고 그 세션(체크인 포함)을 돌려준다 — 앱을 껐다 켠 뒤 이어가기.
     * 따라가기는 사용자당 한 코스만 — 다른 코스를 진행 중이면 409.
     */
    fun start(
        userId: Long,
        courseId: Long,
    ): CourseTraceResult

    /**
     * 루트를 수정해 따라가기 시작 — [placeIds] 순서대로 따라간다(코스 장소를 빼거나 순서를 바꾸거나 코스 밖 장소를 넣을 수 있다).
     * 같은 장소가 두 번 오면 400, 없는 장소가 있으면 404. 나머지는 [start] 와 같고, 이어가기면 요청 루트가 아니라 기존 세션의 루트를 유지한다.
     */
    fun startWithRoute(
        userId: Long,
        courseId: Long,
        placeIds: List<Long>,
    ): CourseTraceResult

    /** 내 완주 목록 — 최근 시작순 커서 페이지. */
    fun listCompleted(query: CourseTracesQuery): CourseTracesResult

    /** 진행 중 세션(사용자당 최대 하나)의 상세 — [getDetail] 과 같은 내용. 없으면 null — 홈 "따라가는 중" 카드용. */
    fun findInProgress(userId: Long): CourseTraceDetailResult?

    /**
     * 따라가기 상세 — 세션·코스 요약·따라가는 장소(순서·캡션·사진)·체크인. 진행 중이면 따라가기 화면을 다시 그리고, 완주면 기록을 본다.
     * 코스가 삭제·비공개됐어도 장소는 내려주고 요약만 isDeleted 다. 내 세션이 아니거나 없으면 404.
     */
    fun getDetail(
        userId: Long,
        traceId: Long,
    ): CourseTraceDetailResult

    /** 장소 체크인. 내 세션이 아니거나 없으면 404, 이미 종료된 세션이면 409, 따라가는 장소(수정한 루트가 있으면 그 루트, 없으면 코스 장소)가 아니면 400. 같은 장소 재전송은 무시한다. */
    fun checkIn(command: CheckInPlaceCommand)

    /** 따라가기 종료. 내 세션이 아니거나 없으면 404, 이미 종료된 세션이면 409, 병합할 장소가 따라가는 장소가 아니면 400. 이때만 코스 tracings_cnt 를 올린다. 진행 중 코스가 삭제됐어도 종료된다. */
    fun end(command: EndCourseTraceCommand)

    /**
     * 출발 장소 → 도착 장소 도보 경로(Tmap). 세션·코스 소속은 보지 않는다 — 따라가는 중 코스 밖 장소를 끼워 넣는 경우도 같은 API 로 조회한다.
     * 같은 장소면 400, 장소가 없으면 404. 도보로 갈 수 없는 구간이면 route 가 null, 일시적 산출 불가는 503.
     */
    fun findRoute(
        fromPlaceId: Long,
        toPlaceId: Long,
    ): TraceRouteResult

    /** 주어진 코스들 중 사용자가 완주한 코스의 완주 시각(가장 이른 종료)을 courseId 별로 반환한다. 완주하지 않은 코스는 결과에 없다. */
    fun findCompletedAt(
        userId: Long,
        courseIds: List<Long>,
    ): Map<Long, Instant>

    /** 사용자가 한 번이라도 완주한 코스 id 집합 — 저장함 완주 필터·완주 수에 쓴다. */
    fun findTracedCourseIds(userId: Long): Set<Long>

    /** 탈퇴자의 따라가기 세션·체크인 기록을 전부 삭제한다(크로스 도메인 정리용). 코스 tracings_cnt 는 누적 통계라 되돌리지 않는다. */
    fun purgeByUser(userId: Long)
}
