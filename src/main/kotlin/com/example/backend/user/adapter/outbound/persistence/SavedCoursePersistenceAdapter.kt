package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.application.port.outbound.CourseFolderCountRow
import com.example.backend.user.application.port.outbound.SavedCoursePersistencePort
import com.example.backend.user.application.port.outbound.SavedCourseRow
import com.example.backend.user.domain.model.SavedCourse
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.notInSubQuery
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import kotlin.time.toJavaInstant

/**
 * 아웃바운드 어댑터 — [SavedCoursePersistencePort] 를 Exposed 로 구현한다.
 * saved_courses(저장 레코드)·saved_course_folders(폴더 소유권 검증)에 접근한다.
 */
@Repository
class SavedCoursePersistenceAdapter : SavedCoursePersistencePort {
    override fun existsFolder(
        userId: Long,
        folderId: Long,
    ): Boolean =
        SavedCourseFolderTable
            .selectAll()
            .where { (SavedCourseFolderTable.id eq folderId) and (SavedCourseFolderTable.userId eq userId) }
            .limit(1)
            .empty()
            .not()

    override fun existsSavedCourse(
        userId: Long,
        courseId: Long,
    ): Boolean =
        SavedCourseTable
            .selectAll()
            .where { (SavedCourseTable.userId eq userId) and (SavedCourseTable.courseId eq courseId) }
            .limit(1)
            .empty()
            .not()

    override fun insert(
        userId: Long,
        courseId: Long,
        folderId: Long?,
    ): SavedCourse =
        SavedCourseEntity
            .new {
                this.userId = userId
                this.courseId = courseId
                this.folderId = folderId
                // created_at 은 테이블 clientDefault 가 채운다.
            }.also { it.refresh(flush = true) }
            .toDomain()

    override fun deleteByUserAndCourse(
        userId: Long,
        courseId: Long,
    ): Boolean =
        SavedCourseTable.deleteWhere {
            (SavedCourseTable.userId eq userId) and (SavedCourseTable.courseId eq courseId)
        } > 0

    override fun count(
        userId: Long,
        folderId: Long?,
        completed: Boolean?,
    ): Long =
        SavedCourseTable
            .selectAll()
            .where(filter(userId, folderId, completed))
            .count()

    override fun findPage(
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

    override fun listFolders(userId: Long): List<CourseFolderCountRow> =
        SavedCourseFolderTable
            .selectAll()
            .where { SavedCourseFolderTable.userId eq userId }
            .orderBy(SavedCourseFolderTable.orderNo to SortOrder.ASC)
            .map { row ->
                val folderId = row[SavedCourseFolderTable.id].value
                CourseFolderCountRow(
                    id = folderId,
                    name = row[SavedCourseFolderTable.name],
                    count = count(userId, folderId, completed = null).toInt(),
                )
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
        var condition: Op<Boolean> = SavedCourseTable.userId eq userId
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
