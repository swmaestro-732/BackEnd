package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.SavedCourseFolderTable
import com.example.backend.user.adapter.outbound.persistence.SavedCourseTable
import com.example.backend.user.application.port.outbound.CourseFolderCountRow
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

/** saved_course_folders 테이블 접근 리포지토리 — 폴더 소유권 검증과 폴더별 저장 개수 집계를 담당한다. */
@Repository
class SavedCourseFolderRepository {
    fun exists(
        userId: Long,
        folderId: Long,
    ): Boolean =
        SavedCourseFolderTable
            .selectAll()
            .where { (SavedCourseFolderTable.id eq folderId) and (SavedCourseFolderTable.userId eq userId) }
            .limit(1)
            .empty()
            .not()

    fun listFolders(userId: Long): List<CourseFolderCountRow> {
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
}
