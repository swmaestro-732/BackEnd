package com.example.backend.course.adapter.outbound.persistence.exposed.repository

import com.example.backend.course.adapter.outbound.persistence.exposed.CourseLikeTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteReturning
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import kotlin.time.Clock

/** course_likes 테이블 접근 리포지토리 — 좋아요 존재·삽입·하드 삭제·배치 조회. */
@Repository
class CourseLikeRepository {
    fun existsLike(
        userId: Long,
        courseId: Long,
    ): Boolean =
        CourseLikeTable
            .selectAll()
            .where {
                (CourseLikeTable.userId eq userId) and
                    (CourseLikeTable.courseId eq courseId)
            }.limit(1)
            .empty()
            .not()

    /** 좋아요 레코드를 삽입한다. (user_id, course_id) 유니크 인덱스가 코스당 좋아요 1개임을 보장한다. */
    fun insert(
        userId: Long,
        courseId: Long,
    ) {
        CourseLikeTable.insert {
            it[CourseLikeTable.userId] = userId
            it[CourseLikeTable.courseId] = courseId
            it[createdAt] = Clock.System.now()
        }
    }

    /**
     * (user, course) 좋아요를 하드 삭제한다. 실제 삭제된 행이 있으면 true(없으면 false, 멱등).
     * 이중 취소가 와도 두 번째 호출은 0행이라 안전하다.
     */
    fun deleteByUserAndCourse(
        userId: Long,
        courseId: Long,
    ): Boolean =
        CourseLikeTable.deleteWhere {
            (CourseLikeTable.userId eq userId) and
                (CourseLikeTable.courseId eq courseId)
        } > 0

    /** (user, courseIds) 중 좋아요한 course_id 집합을 배치 조회한다(조회자 hasLiked 상태용). */
    fun findLikedCourseIds(
        userId: Long,
        courseIds: List<Long>,
    ): Set<Long> {
        if (courseIds.isEmpty()) return emptySet()
        return CourseLikeTable
            .select(CourseLikeTable.courseId)
            .where {
                (CourseLikeTable.userId eq userId) and
                    (CourseLikeTable.courseId inList courseIds)
            }.mapTo(mutableSetOf()) { it[CourseLikeTable.courseId] }
    }

    /**
     * 사용자의 좋아요를 전부 하드 삭제하고, 실제로 삭제된 course_id 들을 반환한다(탈퇴 정리용).
     * DELETE … RETURNING 으로 삭제와 반환을 한 문장에 묶어, 동시 unlike 와 경합해도 각 행은 한 번만
     * 삭제되므로(반환도 한 번) 호출부가 그 코스만 likes_cnt 를 1 내리면 이중 감소가 없다 — 코스 행 잠금 불필요.
     */
    fun deleteAllByUser(userId: Long): List<Long> =
        CourseLikeTable
            .deleteReturning(returning = listOf(CourseLikeTable.courseId)) {
                CourseLikeTable.userId eq userId
            }.map { it[CourseLikeTable.courseId] }
}
