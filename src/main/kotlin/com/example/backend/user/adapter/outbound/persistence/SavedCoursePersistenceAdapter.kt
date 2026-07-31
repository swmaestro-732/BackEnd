package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.application.port.outbound.SavedCoursePersistencePort
import com.example.backend.user.application.port.outbound.SavedCourseRow
import com.example.backend.user.domain.model.SavedCourse
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteWhere
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
    ): Long =
        SavedCourseTable
            .selectAll()
            .where(filter(userId, folderId))
            .count()

    override fun findPage(
        userId: Long,
        folderId: Long?,
        cursorId: Long?,
        limit: Int,
    ): List<SavedCourseRow> {
        var condition = filter(userId, folderId)
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

    /** 소유자 + 폴더 필터(folderId 가 null 이면 전체). */
    private fun filter(
        userId: Long,
        folderId: Long?,
    ): Op<Boolean> {
        val base = SavedCourseTable.userId eq userId
        return folderId?.let { base and (SavedCourseTable.folderId eq it) } ?: base
    }
}
