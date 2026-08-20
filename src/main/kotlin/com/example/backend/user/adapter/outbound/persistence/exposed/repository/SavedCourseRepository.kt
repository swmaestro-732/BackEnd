package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.exposed.SavedCourseTable
import com.example.backend.user.adapter.outbound.persistence.exposed.TracingCourseTable
import com.example.backend.user.application.port.outbound.SavedCourseRow
import com.example.backend.user.domain.model.SavedCourse
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.notInSubQuery
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import kotlin.time.Clock
import kotlin.time.toJavaInstant

/** saved_courses 테이블 접근 리포지토리 — 저장 코스의 조회·삽입·소프트 삭제·완주 필터를 담당한다. */
@Repository
class SavedCourseRepository {
    fun existsSavedCourse(
        userId: Long,
        courseId: Long,
    ): Boolean =
        SavedCourseTable
            .selectAll()
            .where {
                (SavedCourseTable.userId eq userId) and
                    (SavedCourseTable.courseId eq courseId) and
                    SavedCourseTable.deletedAt.isNull()
            }.limit(1)
            .empty()
            .not()

    /**
     * 저장 레코드를 삽입한다. 같은 (user, course) 의 **소프트 삭제된 행이 남아 있으면 먼저 지우고** 새로 발행해
     * 코스당 행을 하나로 유지한다 — 되살리기(UPDATE) 대신 새 id 를 받으므로 id 가 저장 시각 순으로 증가하고,
     * 목록의 최신 저장순 정렬(id DESC)과 커서가 단순한 id 하나로 유지된다.
     */
    fun insert(
        userId: Long,
        courseId: Long,
        folderId: Long?,
    ): SavedCourse {
        // 소프트 삭제 행은 아무도 읽지 않는다(조회·집계는 전부 deleted_at IS NULL) — 재저장 시 정리하고 새로 발행한다.
        SavedCourseTable.deleteWhere {
            (SavedCourseTable.userId eq userId) and
                (SavedCourseTable.courseId eq courseId) and
                SavedCourseTable.deletedAt.isNotNull()
        }

        val now = Clock.System.now()
        val id =
            SavedCourseTable
                .insert {
                    it[SavedCourseTable.userId] = userId
                    it[SavedCourseTable.courseId] = courseId
                    it[SavedCourseTable.folderId] = folderId
                    it[createdAt] = now
                }[SavedCourseTable.id]
                .value
        return SavedCourse(
            id = id,
            userId = userId,
            courseId = courseId,
            folderId = folderId,
            savedAt = now.toJavaInstant(),
        )
    }

    /**
     * 소프트 삭제 — 살아있는(deleted_at IS NULL) 행에만 삭제 스탬프를 찍는다.
     * WHERE 의 deleted_at IS NULL 가드 덕에 동시 이중 삭제가 와도 두 번째 호출은 0행이라 안전하다.
     * 이후 같은 코스 재저장은 partial 유니크 인덱스(deleted_at IS NULL) 덕에 새 행으로 허용된다.
     */
    fun deleteByUserAndCourse(
        userId: Long,
        courseId: Long,
    ): Boolean =
        SavedCourseTable.update({
            (SavedCourseTable.userId eq userId) and
                (SavedCourseTable.courseId eq courseId) and
                SavedCourseTable.deletedAt.isNull()
        }) {
            it[deletedAt] = Clock.System.now()
        } > 0

    /** 사용자가 지금 살아있게 저장 중인(deleted_at IS NULL) 코스 id 목록 — 탈퇴 정리 시 원저자 saves_cnt 보정용. */
    fun findAliveSavedCourseIds(userId: Long): List<Long> =
        SavedCourseTable
            .select(SavedCourseTable.courseId)
            .where { (SavedCourseTable.userId eq userId) and SavedCourseTable.deletedAt.isNull() }
            .map { it[SavedCourseTable.courseId] }

    /** 사용자의 저장 코스 레코드를 전부 하드 삭제한다(살아있는 것·이미 소프트 삭제된 것 모두 — 탈퇴 정리). */
    fun deleteAllByUser(userId: Long) {
        SavedCourseTable.deleteWhere { SavedCourseTable.userId eq userId }
    }

    fun count(
        userId: Long,
        folderId: Long?,
        completed: Boolean?,
    ): Long =
        SavedCourseTable
            .selectAll()
            .where(filter(userId, folderId, completed))
            .count()

    /**
     * 폴더에 넣지 않고 저장한(folder_id IS NULL) 코스 개수.
     * [count] 의 folderId=null 은 "폴더 무관 전체"라 이 질문에 답할 수 없어 별도 메서드로 둔다.
     */
    fun countWithoutFolder(userId: Long): Long =
        SavedCourseTable
            .selectAll()
            .where {
                (SavedCourseTable.userId eq userId) and
                    SavedCourseTable.deletedAt.isNull() and
                    SavedCourseTable.folderId.isNull()
            }.count()

    /** 최신 저장순 = id DESC. 재저장이 새 id 를 발행하므로 id 순서가 곧 저장 시각 순서다([insert]). */
    fun findPage(
        userId: Long,
        folderId: Long?,
        completed: Boolean?,
        cursorId: Long?,
        limit: Int,
    ): List<SavedCourseRow> {
        var condition = filter(userId, folderId, completed)
        cursorId?.let { condition = condition and (SavedCourseTable.id less it) }

        return SavedCourseTable
            .selectAll()
            .where(condition)
            .orderBy(SavedCourseTable.id to SortOrder.DESC)
            .limit(limit)
            .map {
                SavedCourseRow(
                    id = it[SavedCourseTable.id].value,
                    folderId = it[SavedCourseTable.folderId],
                    courseId = it[SavedCourseTable.courseId],
                    savedAt = it[SavedCourseTable.createdAt].toJavaInstant(),
                )
            }
    }

    /** (user, courseIds) 중 살아있는 저장 레코드가 있는 course_id 집합을 배치 조회한다. */
    fun findSavedCourseIds(
        userId: Long,
        courseIds: List<Long>,
    ): Set<Long> {
        if (courseIds.isEmpty()) return emptySet()
        return SavedCourseTable
            .select(SavedCourseTable.courseId)
            .where {
                (SavedCourseTable.userId eq userId) and
                    (SavedCourseTable.courseId inList courseIds) and
                    SavedCourseTable.deletedAt.isNull()
            }.mapTo(mutableSetOf()) { it[SavedCourseTable.courseId] }
    }

    /**
     * 소유자 + 폴더 + 완주 여부 필터.
     * - folderId 가 null 이면 전체(폴더 미분류 포함).
     * - completed 가 null 이면 완주 여부 무관, true 면 따라간(=완주) 코스만, false 면 아직 안 따라간 코스만.
     *   완주 판정은 tracing_courses 에 (user_id, course_id) 행이 있는지로 한다(course_id in/ not in 서브쿼리).
     */
    private fun filter(
        userId: Long,
        folderId: Long?,
        completed: Boolean?,
    ): Op<Boolean> {
        var condition: Op<Boolean> = (SavedCourseTable.userId eq userId) and SavedCourseTable.deletedAt.isNull()
        folderId?.let { condition = condition and (SavedCourseTable.folderId eq it) }
        completed?.let { condition = condition and completedOp(userId, it) }
        return condition
    }

    /** (user, course) 가 tracing_courses 에 있으면 완주로 본다 — completed=true 는 in, false 는 not in. */
    private fun completedOp(
        userId: Long,
        completed: Boolean,
    ): Op<Boolean> {
        val tracedCourseIds =
            TracingCourseTable
                .select(TracingCourseTable.courseId)
                .where { TracingCourseTable.userId eq userId }
        return if (completed) {
            SavedCourseTable.courseId inSubQuery tracedCourseIds
        } else {
            SavedCourseTable.courseId notInSubQuery tracedCourseIds
        }
    }
}
