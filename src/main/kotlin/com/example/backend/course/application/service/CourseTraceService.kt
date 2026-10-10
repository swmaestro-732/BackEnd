package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.course.application.port.inbound.CourseTraceUseCase
import com.example.backend.course.application.port.inbound.dto.EndCourseTraceCommand
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CourseTracePersistencePort
import com.example.backend.course.domain.model.CourseStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 코스 따라가기 유스케이스 — 종료 시 기록·방문 장소 삽입 + tracings_cnt 원자적 +1. CourseLikeService 미러(열람 권한 검사 동일). */
@Service
class CourseTraceService(
    private val courseTracePersistencePort: CourseTracePersistencePort,
    private val coursePersistencePort: CoursePersistencePort,
    private val courseViewPolicy: CourseViewPolicy,
) : CourseTraceUseCase {
    @Transactional
    override fun end(command: EndCourseTraceCommand) {
        val courseId = command.courseId
        val course = coursePersistencePort.findCourseDetail(courseId)
        if (course == null ||
            course.status != CourseStatus.ACTIVE ||
            !courseViewPolicy.isViewable(course.visibility, course.userId, command.userId)
        ) {
            throw BusinessException(CourseErrorCode.COURSE_NOT_FOUND, "따라가기를 종료할 코스를 찾을 수 없습니다: courseId=$courseId")
        }

        courseTracePersistencePort.insert(command)
        // 0행이면 코스가 그 사이 비활성된 것 → 삽입까지 롤백해 기록·카운터 불일치를 막는다.
        if (coursePersistencePort.increaseTracingsCount(courseId) == 0) {
            throw BusinessException(CourseErrorCode.COURSE_NOT_FOUND, "따라가기를 종료할 코스를 찾을 수 없습니다: courseId=$courseId")
        }
    }
}
