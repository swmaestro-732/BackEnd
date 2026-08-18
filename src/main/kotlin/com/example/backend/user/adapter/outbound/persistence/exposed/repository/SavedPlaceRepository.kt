package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.exposed.SavedPlaceTable
import com.example.backend.user.application.port.outbound.SavedPlaceCategoryCountRow
import com.example.backend.user.application.port.outbound.SavedPlaceRow
import com.example.backend.user.domain.model.SavedPlace
import com.example.backend.user.domain.model.SavedPlaceCategory
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import kotlin.time.Clock
import kotlin.time.toJavaInstant

/** saved_places 테이블 접근 리포지토리 — 저장 장소의 조회·삽입·소프트 삭제·카운트 집계를 담당한다. */
@Repository
class SavedPlaceRepository {
    fun existsSavedPlace(
        userId: Long,
        placeId: Long,
    ): Boolean =
        SavedPlaceTable
            .selectAll()
            .where {
                (SavedPlaceTable.userId eq userId) and
                    (SavedPlaceTable.placeId eq placeId) and
                    SavedPlaceTable.deletedAt.isNull()
            }.limit(1)
            .empty()
            .not()

    /**
     * 저장 레코드를 넣고 도메인 모델로 돌려준다(DSL — [UserRepository.save] 와 같은 형식).
     * created_at 은 테이블 clientDefault 대신 여기서 만든 [now] 를 명시해 넣는다 —
     * 그래야 삽입 후 재조회 없이 반환 객체를 조립할 수 있다.
     *
     * 같은 (user, place) 의 **소프트 삭제된 행이 남아 있으면 먼저 지우고** 새로 발행해 장소당 행을 하나로 유지한다.
     * 되살리기(UPDATE)가 아니라 새 id 를 받으므로 id 순서가 곧 저장 시각 순서고, 목록의 최신 저장순 정렬(id DESC)과
     * 커서가 단순한 id 하나로 유지된다.
     */
    fun insert(
        userId: Long,
        placeId: Long,
        category: SavedPlaceCategory?,
    ): SavedPlace {
        // 소프트 삭제 행은 아무도 읽지 않는다(조회·집계는 전부 deleted_at IS NULL) — 재저장 시 정리하고 새로 발행한다.
        SavedPlaceTable.deleteWhere {
            (SavedPlaceTable.userId eq userId) and
                (SavedPlaceTable.placeId eq placeId) and
                SavedPlaceTable.deletedAt.isNotNull()
        }

        val now = Clock.System.now()
        val id =
            SavedPlaceTable
                .insert {
                    it[SavedPlaceTable.userId] = userId
                    it[SavedPlaceTable.placeId] = placeId
                    it[SavedPlaceTable.category] = category
                    it[visited] = false // 저장 직후는 미방문 — 방문 처리 PATCH 로만 전환된다.
                    it[createdAt] = now
                }[SavedPlaceTable.id]
                .value
        return SavedPlace(
            id = id,
            userId = userId,
            placeId = placeId,
            category = category,
            visited = false,
            savedAt = now.toJavaInstant(),
        )
    }

    /**
     * 소프트 삭제 — 살아있는(deleted_at IS NULL) 행에만 삭제 스탬프를 찍는다.
     * WHERE 의 deleted_at IS NULL 가드 덕에 동시 이중 삭제가 와도 두 번째 호출은 0행이라 안전하다.
     * 이후 같은 장소 재저장은 남은 이 행을 지우고 새 행을 발행한다([insert]) — partial 유니크 인덱스(deleted_at IS NULL)가
     * 살아있는 행만 보므로 그 사이에도 유일성은 유지된다.
     */
    fun deleteByUserAndPlace(
        userId: Long,
        placeId: Long,
    ): Boolean =
        SavedPlaceTable.update({
            (SavedPlaceTable.userId eq userId) and
                (SavedPlaceTable.placeId eq placeId) and
                SavedPlaceTable.deletedAt.isNull()
        }) {
            it[deletedAt] = Clock.System.now()
        } > 0

    fun countByVisited(
        userId: Long,
        visited: Boolean,
    ): Long =
        SavedPlaceTable
            .selectAll()
            .where { alive(userId) and (SavedPlaceTable.visited eq visited) }
            .count()

    /** 카테고리별 저장 개수를 카테고리 수만큼의 count 쿼리 대신 groupBy 한 번으로 집계한다(미분류 null 은 제외). */
    fun countByCategory(userId: Long): List<SavedPlaceCategoryCountRow> {
        val categoryCount = SavedPlaceTable.id.count()
        return SavedPlaceTable
            .select(SavedPlaceTable.category, categoryCount)
            .where { alive(userId) and SavedPlaceTable.category.isNotNull() }
            .groupBy(SavedPlaceTable.category)
            .mapNotNull { row ->
                row[SavedPlaceTable.category]?.let {
                    SavedPlaceCategoryCountRow(
                        category = it,
                        count = row[categoryCount],
                    )
                }
            }
    }

    fun findPage(
        userId: Long,
        visited: Boolean,
        category: SavedPlaceCategory?,
        cursorId: Long?,
        limit: Int,
    ): List<SavedPlaceRow> {
        var condition = alive(userId) and (SavedPlaceTable.visited eq visited)
        category?.let { condition = condition and (SavedPlaceTable.category eq it) }
        cursorId?.let { condition = condition and (SavedPlaceTable.id less it) }

        return SavedPlaceTable
            .selectAll()
            .where(condition)
            .orderBy(SavedPlaceTable.id to SortOrder.DESC)
            .limit(limit)
            .map {
                SavedPlaceRow(
                    id = it[SavedPlaceTable.id].value,
                    placeId = it[SavedPlaceTable.placeId],
                    category = it[SavedPlaceTable.category],
                    visited = it[SavedPlaceTable.visited],
                    savedAt = it[SavedPlaceTable.createdAt].toJavaInstant(),
                )
            }
    }

    /** 소유자의 살아있는(소프트 삭제 제외) 저장 레코드 필터. */
    private fun alive(userId: Long): Op<Boolean> =
        (SavedPlaceTable.userId eq userId) and SavedPlaceTable.deletedAt.isNull()
}
