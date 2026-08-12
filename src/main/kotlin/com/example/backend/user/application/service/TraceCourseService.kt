package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.user.application.port.inbound.TraceCourseUseCase
import com.example.backend.user.application.port.inbound.dto.TracingProgress
import com.example.backend.user.application.port.inbound.dto.TracingResult
import com.example.backend.user.application.port.outbound.CourseAccessPort
import com.example.backend.user.application.port.outbound.TracingPersistencePort
import com.example.backend.user.application.port.outbound.TracingRow
import com.example.backend.user.application.port.outbound.UserPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 코스 따라가기 유스케이스 — 시작·장소 체크인·진행 조회.
 *
 * 시작(start): 사용자 행을 잠가 동시 탈퇴와 직렬화하고(그 외 USER_NOT_FOUND), 코스 존재를 ACL 로 검증한다(그 외 COURSE_NOT_FOUND).
 *   한 코스당 진행중 트레이스는 1개다 — 이미 진행중이면 막는다(409).
 * 체크인(checkInPlace): 소유 tracing 이 아니면 404. 이미 완주면 no-op 로 현재 진행을 그대로 돌려준다(멱등).
 *   코스에 담긴 장소만 허용하고(그 외 400), 체크인 후 서로 다른 코스 장소를 모두 채우면 자동 완주한다
 *   (completed_at 세팅 + courses.tracings_cnt 증가). course 접근은 [CourseAccessPort](ACL)로만 한다.
 *
 * 기본은 읽기 전용 트랜잭션이고, 쓰기(start·checkInPlace)만 메서드 레벨에서 재정의한다.
 */
@Service
@Transactional(readOnly = true)
class TraceCourseService(
    private val tracingPersistencePort: TracingPersistencePort,
    private val courseAccessPort: CourseAccessPort,
    private val userPersistencePort: UserPersistencePort,
) : TraceCourseUseCase {
    @Transactional
    override fun start(
        userId: Long,
        courseId: Long,
    ): TracingResult {
        // 사용자 행을 FOR UPDATE 로 잠가 동시 탈퇴 정리와 직렬화한다(탈퇴 계정에 따라가기 유입 차단).
        if (userPersistencePort.lockActive(listOf(userId)).isEmpty()) {
            throw BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: id=$userId")
        }
        if (!courseAccessPort.existsCourse(courseId)) {
            throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "따라갈 코스를 찾을 수 없습니다: courseId=$courseId")
        }
        if (tracingPersistencePort.findActiveByUserCourse(userId, courseId) != null) {
            throw BusinessException(ErrorCode.TRACING_ALREADY_STARTED, "이미 따라가는 중인 코스입니다: courseId=$courseId")
        }

        val tracingId = tracingPersistencePort.insertTracing(userId, courseId)
        return TracingResult(tracingId = tracingId, courseId = courseId, startedAt = Instant.now())
    }

    @Transactional
    override fun checkInPlace(
        userId: Long,
        tracingId: Long,
        placeId: Long,
    ): TracingProgress {
        val row =
            tracingPersistencePort.findOwned(userId, tracingId)
                ?: throw BusinessException(ErrorCode.TRACING_NOT_FOUND, "따라가기를 찾을 수 없습니다: id=$tracingId")

        val placeIds = courseAccessPort.getCoursePlaceIds(row.courseId)

        // 이미 완주한 tracing 이면 아무것도 바꾸지 않고 현재 진행을 그대로 돌려준다(멱등).
        if (row.completedAt != null) {
            return progress(row, placeIds.size)
        }
        if (placeId !in placeIds) {
            throw BusinessException(ErrorCode.PLACE_NOT_IN_COURSE, "코스에 포함되지 않은 장소입니다: placeId=$placeId")
        }

        tracingPersistencePort.checkInPlace(tracingId, placeId)
        val checked = tracingPersistencePort.countCheckedPlaces(tracingId)
        val totalPlaces = placeIds.size

        // 서로 다른 코스 장소를 모두 채우면 자동 완주한다 — 완주 시각 스탬프 + 코스 tracings_cnt 증가(같은 트랜잭션).
        if (checked == totalPlaces) {
            val now = Instant.now()
            tracingPersistencePort.markCompleted(tracingId, now)
            courseAccessPort.increaseTracingsCount(row.courseId)
            return TracingProgress(
                tracingId = tracingId,
                courseId = row.courseId,
                totalPlaces = totalPlaces,
                checkedPlaces = checked,
                completed = true,
                completedAt = now,
            )
        }
        return TracingProgress(
            tracingId = tracingId,
            courseId = row.courseId,
            totalPlaces = totalPlaces,
            checkedPlaces = checked,
            completed = false,
            completedAt = null,
        )
    }

    override fun getProgress(
        userId: Long,
        tracingId: Long,
    ): TracingProgress {
        val row =
            tracingPersistencePort.findOwned(userId, tracingId)
                ?: throw BusinessException(ErrorCode.TRACING_NOT_FOUND, "따라가기를 찾을 수 없습니다: id=$tracingId")
        return progress(row, courseAccessPort.getCoursePlaceIds(row.courseId).size)
    }

    private fun progress(
        row: TracingRow,
        totalPlaces: Int,
    ): TracingProgress =
        TracingProgress(
            tracingId = row.id,
            courseId = row.courseId,
            totalPlaces = totalPlaces,
            checkedPlaces = tracingPersistencePort.countCheckedPlaces(row.id),
            completed = row.completedAt != null,
            completedAt = row.completedAt,
        )
}
