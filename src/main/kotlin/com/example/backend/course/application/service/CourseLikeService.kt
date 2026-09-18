package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.course.application.port.inbound.CourseLikeUseCase
import com.example.backend.course.application.port.outbound.CourseLikePersistencePort
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 코스 좋아요 유스케이스 — 좋아요/취소 토글. CourseReviewService 미러(사용자 검증·잠금 없이 인증된 userId 를 신뢰한다).
 *
 * 동시성 제어는 코스 행 잠금 없이, 좋아요 유니크 제약(중복 차단)과 원자적 +1/-1 … RETURNING 으로 확보한다.
 * `UPDATE … likes_cnt = likes_cnt ± 1 … RETURNING` 은 UPDATE 가 잡는 행 락 아래에서 DB 가 현재 값 기준으로
 * 증감하고 갱신된 값을 그대로 돌려주므로, 별도 잠금 SELECT 없이 로스트 업데이트(이중 집계·누락)를 막는다.
 */
@Service
class CourseLikeService(
    private val courseLikePersistencePort: CourseLikePersistencePort,
    private val coursePersistencePort: CoursePersistencePort,
) : CourseLikeUseCase {
    @Transactional
    override fun like(
        userId: Long,
        courseId: Long,
    ): Int {
        // 유니크 제약이 최종 방어선 — 사전검사로 중복은 409 로 빠르게 걸러낸다(잠금 없이).
        if (courseLikePersistencePort.existsLike(userId, courseId)) {
            throw BusinessException(CourseErrorCode.COURSE_ALREADY_LIKED, "이미 좋아요한 코스입니다: courseId=$courseId")
        }

        courseLikePersistencePort.insert(userId, courseId)
        // 원자적 +1 … RETURNING — 0행(비활성/삭제)이면 삽입도 롤백하고 404.
        return coursePersistencePort.increaseLikesCountReturning(courseId)
            ?: throw BusinessException(CourseErrorCode.COURSE_NOT_FOUND, "좋아요할 코스를 찾을 수 없습니다: courseId=$courseId")
    }

    @Transactional
    override fun unlike(
        userId: Long,
        courseId: Long,
    ): Int =
        // 실제로 지웠을 때만 원자적 -1 … RETURNING 으로 감소한다 — 멱등 no-op 은 현재 likes_cnt 를 그대로 반환한다.
        if (courseLikePersistencePort.deleteByUserAndCourse(userId, courseId)) {
            coursePersistencePort.decreaseLikesCountReturning(courseId)
                ?: throw BusinessException(
                    CourseErrorCode.COURSE_NOT_FOUND,
                    "좋아요를 취소할 코스를 찾을 수 없습니다: courseId=$courseId",
                )
        } else {
            coursePersistencePort.readLikesCount(courseId)
                ?: throw BusinessException(
                    CourseErrorCode.COURSE_NOT_FOUND,
                    "좋아요를 취소할 코스를 찾을 수 없습니다: courseId=$courseId",
                )
        }

    @Transactional
    override fun purgeByUser(userId: Long) {
        // 좋아요를 한 번의 DELETE … RETURNING 으로 지우고, 실제로 지워진 코스만 배치로 카운터를 내린다(총 2쿼리).
        // 행은 한 번만 삭제되므로 동시 unlike 와 경합해도 그 코스의 감소는 한 번뿐 — 코스 행 잠금 없이 이중 감소를 막는다.
        val courseIds = courseLikePersistencePort.deleteAllByUser(userId)
        if (courseIds.isNotEmpty()) {
            coursePersistencePort.decreaseLikesCounts(courseIds)
        }
    }
}
