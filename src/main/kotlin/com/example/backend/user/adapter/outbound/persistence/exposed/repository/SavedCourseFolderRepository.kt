package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.exposed.SavedCourseFolderEntity
import com.example.backend.user.adapter.outbound.persistence.exposed.SavedCourseFolderTable
import com.example.backend.user.adapter.outbound.persistence.exposed.SavedCourseTable
import com.example.backend.user.application.port.outbound.CourseFolderCountRow
import com.example.backend.user.application.port.outbound.CourseFolderRow
import com.example.backend.user.domain.model.CourseFolder
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

/** saved_course_folders 테이블 접근 리포지토리 — 폴더 생성·소유권 검증·폴더별 저장 개수 집계를 담당한다. */
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

    /** 같은 사용자에게 같은 이름의 폴더가 이미 있는지 — 중복 생성 차단(409)에 쓴다. */
    fun existsByName(
        userId: Long,
        name: String,
    ): Boolean =
        SavedCourseFolderTable
            .selectAll()
            .where { (SavedCourseFolderTable.userId eq userId) and (SavedCourseFolderTable.name eq name) }
            .limit(1)
            .empty()
            .not()

    /**
     * 폴더를 삽입하고 생성 id 까지 적재된 도메인 [CourseFolder] 를 반환한다.
     * 새 폴더는 폴더 칩 맨 뒤에 붙으므로 order_no 는 그 사용자의 현재 최대값 + 1(첫 폴더는 0)로 채운다.
     */
    fun insert(
        userId: Long,
        name: String,
    ): CourseFolder {
        val orderNo = ((maxOrderNo(userId)?.toInt() ?: -1) + 1).toShort()
        return SavedCourseFolderEntity
            .new {
                this.userId = userId
                this.name = name
                this.orderNo = orderNo
            }.also { it.refresh(flush = true) }
            .toDomain()
    }

    private fun maxOrderNo(userId: Long): Short? {
        val maxOrderNo = SavedCourseFolderTable.orderNo.max()
        return SavedCourseFolderTable
            .select(maxOrderNo)
            .where { SavedCourseFolderTable.userId eq userId }
            .firstOrNull()
            ?.get(maxOrderNo)
    }

    /** 폴더 자체만 order_no 순으로 — 저장 개수가 필요 없는 화면(폴더 목록 조회)은 조인·집계 없이 이걸 쓴다. */
    fun findByUser(userId: Long): List<CourseFolderRow> =
        SavedCourseFolderTable
            .select(SavedCourseFolderTable.id, SavedCourseFolderTable.name)
            .where { SavedCourseFolderTable.userId eq userId }
            .orderBy(SavedCourseFolderTable.orderNo to SortOrder.ASC)
            .map { CourseFolderRow(id = it[SavedCourseFolderTable.id].value, name = it[SavedCourseFolderTable.name]) }

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

    /** 사용자의 저장 폴더를 전부 하드 삭제한다(회원 탈퇴 정리 — 저장 코스를 먼저 지운 뒤 호출해 FK 위반을 피한다). */
    fun deleteAllByUser(userId: Long) {
        SavedCourseFolderTable.deleteWhere { SavedCourseFolderTable.userId eq userId }
    }
}
