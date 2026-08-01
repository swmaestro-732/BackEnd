package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.application.port.outbound.CourseFolderCountRow
import com.example.backend.user.application.port.outbound.SavedCoursePersistencePort
import com.example.backend.user.application.port.outbound.SavedCourseRow
import com.example.backend.user.domain.model.SavedCourse
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.notInSubQuery
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import kotlin.time.Clock
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
            .where {
                (SavedCourseTable.userId eq userId) and
                    (SavedCourseTable.courseId eq courseId) and
                    SavedCourseTable.deletedAt.isNull()
            }.limit(1)
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

    /**
     * 소프트 삭제 — 살아있는(deleted_at IS NULL) 행에만 삭제 스탬프를 찍는다.
     * WHERE 의 deleted_at IS NULL 가드 덕에 동시 이중 삭제가 와도 두 번째 호출은 0행이라 안전하다.
     * 이후 같은 코스 재저장은 partial 유니크 인덱스(deleted_at IS NULL) 덕에 새 행으로 허용된다.
     */
    override fun deleteByUserAndCourse(
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

    override fun listFolders(userId: Long): List<CourseFolderCountRow> {
        // 폴더별 저장 개수를 폴더 수만큼의 count 쿼리(N+1) 대신 leftJoin + groupBy 한 번으로 집계한다.
        // LEFT 조인이라 저장 코스가 없는 폴더도 남고, saved_courses.id 의 count 는 0 이 된다.
        // deleted_at IS NULL 가드는 조인 조건(additionalConstraint)에 둔다 — WHERE 로 옮기면
        // 소프트 삭제뿐인 폴더가 통째로 빠지므로, 조인 단계에서 걸러 빈 폴더는 count 0 으로 남긴다.
        val savedCount = SavedCourseTable.id.count().alias("saved_count")
        return SavedCourseFolderTable
            .join(
                SavedCourseTable,
                JoinType.LEFT,
                onColumn = SavedCourseFolderTable.id,
                otherColumn = SavedCourseTable.folderId,
                additionalConstraint = { SavedCourseTable.deletedAt.isNull() },
            ).select(
                SavedCourseFolderTable.id,
                SavedCourseFolderTable.name,
                SavedCourseFolderTable.orderNo,
                savedCount,
            ).where { SavedCourseFolderTable.userId eq userId }
            .groupBy(SavedCourseFolderTable.id, SavedCourseFolderTable.name, SavedCourseFolderTable.orderNo)
            .orderBy(SavedCourseFolderTable.orderNo to SortOrder.ASC)
            .map { row ->
                CourseFolderCountRow(
                    id = row[SavedCourseFolderTable.id].value,
                    name = row[SavedCourseFolderTable.name],
                    count = row[savedCount].toInt(),
                )
            }
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
