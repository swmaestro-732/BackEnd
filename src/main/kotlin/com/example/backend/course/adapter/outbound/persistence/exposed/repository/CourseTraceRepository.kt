package com.example.backend.course.adapter.outbound.persistence.exposed.repository

import com.example.backend.common.domain.CourseTraceStatus
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseTracePlaceTable
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseTraceTable
import com.example.backend.course.application.port.inbound.dto.VisitedPlaceResult
import com.example.backend.course.application.port.outbound.CourseTraceRow
import com.example.backend.course.domain.model.CourseTracePlaceStatus
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.min
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertIgnoreAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.time.Instant
import kotlin.time.Clock
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

/** tracing_courses·tracing_course_places 테이블 접근 리포지토리 — 따라가기 세션의 시작·루트·체크인·종료와 완주 조회. */
@Repository
class CourseTraceRepository {
    fun findInProgress(userId: Long): CourseTraceRow? =
        CourseTraceTable
            .selectAll()
            .where {
                (CourseTraceTable.userId eq userId) and (CourseTraceTable.status eq CourseTraceStatus.IN_PROGRESS)
            }.singleOrNull()
            ?.toRow()

    /** 사용자의 완주 세션을 id DESC 키셋으로 읽는다 — (user_id, course_id) 인덱스의 user_id 접두로 탄다. 한 사용자의 세션 수는 적다. */
    fun findCompleted(
        userId: Long,
        beforeId: Long?,
        size: Int,
    ): List<CourseTraceRow> {
        var condition = (CourseTraceTable.userId eq userId) and (CourseTraceTable.status eq CourseTraceStatus.COMPLETED)
        beforeId?.let { condition = condition and (CourseTraceTable.id less it) }
        return CourseTraceTable
            .selectAll()
            .where(condition)
            .orderBy(CourseTraceTable.id to SortOrder.DESC)
            .limit(size)
            .map { it.toRow() }
    }

    /** 부분 유니크 인덱스(진행 중 세션은 사용자당 하나)와 충돌하면 null — 동시 시작은 호출부가 기존 세션을 다시 읽는다. */
    fun insertInProgress(
        userId: Long,
        courseId: Long,
    ): Long? =
        CourseTraceTable
            .insertIgnoreAndGetId {
                it[CourseTraceTable.userId] = userId
                it[CourseTraceTable.courseId] = courseId
                it[status] = CourseTraceStatus.IN_PROGRESS
                it[durationMinutes] = 0
                it[distanceMeters] = 0
            }?.value

    /** 세션이 따라갈 루트를 받은 순서대로 PLANNED 로 넣는다. */
    fun insertRoutePlaces(
        traceId: Long,
        placeIds: List<Long>,
    ) {
        CourseTracePlaceTable.batchInsert(placeIds.withIndex()) { (index, placeId) ->
            this[CourseTracePlaceTable.tracingCourseId] = traceId
            this[CourseTracePlaceTable.placeId] = placeId
            this[CourseTracePlaceTable.status] = CourseTracePlaceStatus.PLANNED
            this[CourseTracePlaceTable.orderNo] = index.toShort()
        }
    }

    /** PLANNED 행을 지우고, VISITED 행의 order_no 를 체크인 순으로 다시 매긴 뒤 그 뒤에 [placeIds] 를 PLANNED 로 넣는다. */
    fun replaceRemainingRoutePlaces(
        traceId: Long,
        placeIds: List<Long>,
    ) {
        CourseTracePlaceTable.deleteWhere {
            (tracingCourseId eq traceId) and (status eq CourseTracePlaceStatus.PLANNED)
        }
        val visitedRowIds =
            CourseTracePlaceTable
                .select(CourseTracePlaceTable.id)
                .where { CourseTracePlaceTable.tracingCourseId eq traceId }
                .orderBy(CourseTracePlaceTable.visitedAt to SortOrder.ASC, CourseTracePlaceTable.id to SortOrder.ASC)
                .map { it[CourseTracePlaceTable.id] }
        visitedRowIds.forEachIndexed { index, rowId ->
            CourseTracePlaceTable.update({ CourseTracePlaceTable.id eq rowId }) { it[orderNo] = index.toShort() }
        }
        CourseTracePlaceTable.batchInsert(placeIds.withIndex()) { (index, placeId) ->
            this[CourseTracePlaceTable.tracingCourseId] = traceId
            this[CourseTracePlaceTable.placeId] = placeId
            this[CourseTracePlaceTable.status] = CourseTracePlaceStatus.PLANNED
            this[CourseTracePlaceTable.orderNo] = (visitedRowIds.size + index).toShort()
        }
    }

    /** 세션 루트의 장소 id 를 순서대로(방문 여부 무관) — 루트 없이 시작한 세션은 빈 목록. */
    fun findRoutePlaceIds(traceId: Long): List<Long> =
        CourseTracePlaceTable
            .select(CourseTracePlaceTable.placeId)
            .where { (CourseTracePlaceTable.tracingCourseId eq traceId) and CourseTracePlaceTable.orderNo.isNotNull() }
            .orderBy(CourseTracePlaceTable.orderNo to SortOrder.ASC)
            .map { it[CourseTracePlaceTable.placeId] }

    /** 세션 행을 FOR UPDATE 로 읽어 체크인·종료가 같은 잠금으로 직렬화되게 한다 — 종료가 먼저 커밋되면 뒤따르는 체크인은 COMPLETED 를 본다. */
    fun findById(traceId: Long): CourseTraceRow? =
        CourseTraceTable
            .selectAll()
            .where { CourseTraceTable.id eq traceId }
            .forUpdate()
            .singleOrNull()
            ?.toRow()

    /** 잠금 없이 읽는다 — 읽기 전용 트랜잭션에서는 FOR UPDATE 를 쓸 수 없다. */
    fun readById(traceId: Long): CourseTraceRow? =
        CourseTraceTable
            .selectAll()
            .where { CourseTraceTable.id eq traceId }
            .singleOrNull()
            ?.toRow()

    fun findVisitedPlaces(traceId: Long): List<VisitedPlaceResult> =
        CourseTracePlaceTable
            .select(CourseTracePlaceTable.placeId, CourseTracePlaceTable.visitedAt)
            .where {
                (CourseTracePlaceTable.tracingCourseId eq traceId) and
                    (CourseTracePlaceTable.status eq CourseTracePlaceStatus.VISITED)
            }
            // 루트 행은 시작할 때 미리 들어가 id 순이 체크인 순이 아니다 — 체크인 시각으로 정렬한다.
            .orderBy(CourseTracePlaceTable.visitedAt to SortOrder.ASC, CourseTracePlaceTable.id to SortOrder.ASC)
            .map {
                VisitedPlaceResult(
                    it[CourseTracePlaceTable.placeId],
                    it[CourseTracePlaceTable.visitedAt]!!.toJavaInstant(),
                )
            }

    /** 그 장소의 PLANNED 행 중 루트에서 가장 앞선 것을 VISITED 로 바꾸고, 없으면(루트 없이 시작한 세션) VISITED 행을 넣는다. */
    fun insertVisitedPlace(
        traceId: Long,
        placeId: Long,
        visitedAt: Instant,
    ) {
        val plannedRowId =
            CourseTracePlaceTable
                .select(CourseTracePlaceTable.id)
                .where {
                    (CourseTracePlaceTable.tracingCourseId eq traceId) and
                        (CourseTracePlaceTable.placeId eq placeId) and
                        (CourseTracePlaceTable.status eq CourseTracePlaceStatus.PLANNED)
                }.orderBy(CourseTracePlaceTable.orderNo to SortOrder.ASC)
                .limit(1)
                .singleOrNull()
                ?.get(CourseTracePlaceTable.id)
        if (plannedRowId != null) {
            CourseTracePlaceTable.update({ CourseTracePlaceTable.id eq plannedRowId }) {
                it[status] = CourseTracePlaceStatus.VISITED
                it[CourseTracePlaceTable.visitedAt] = visitedAt.toKotlinInstant()
            }
            return
        }
        CourseTracePlaceTable.insert {
            it[tracingCourseId] = traceId
            it[CourseTracePlaceTable.placeId] = placeId
            it[status] = CourseTracePlaceStatus.VISITED
            it[CourseTracePlaceTable.visitedAt] = visitedAt.toKotlinInstant()
        }
    }

    /** status = IN_PROGRESS 조건으로 갱신해 이미 종료된 세션(동시 종료 포함)은 0행이다. */
    fun complete(
        traceId: Long,
        durationMinutes: Int,
        distanceMeters: Int,
    ): Int =
        CourseTraceTable.update({
            (CourseTraceTable.id eq traceId) and (CourseTraceTable.status eq CourseTraceStatus.IN_PROGRESS)
        }) {
            it[status] = CourseTraceStatus.COMPLETED
            it[CourseTraceTable.durationMinutes] = durationMinutes
            it[CourseTraceTable.distanceMeters] = distanceMeters
            it[endedAt] = Clock.System.now()
        }

    /** (user, courseIds) 중 완주한 코스의 가장 이른 ended_at 을 courseId 별로 반환한다(없으면 빈 맵). */
    fun findCompletedAt(
        userId: Long,
        courseIds: List<Long>,
    ): Map<Long, Instant> {
        if (courseIds.isEmpty()) return emptyMap()
        val firstEndedAt = CourseTraceTable.endedAt.min()
        return CourseTraceTable
            .select(CourseTraceTable.courseId, firstEndedAt)
            .where {
                (CourseTraceTable.userId eq userId) and
                    (CourseTraceTable.courseId inList courseIds) and
                    (CourseTraceTable.status eq CourseTraceStatus.COMPLETED)
            }.groupBy(CourseTraceTable.courseId)
            .mapNotNull { row -> row[firstEndedAt]?.let { row[CourseTraceTable.courseId] to it.toJavaInstant() } }
            .toMap()
    }

    /** 사용자가 한 번이라도 완주한 코스 id 집합. */
    fun findTracedCourseIds(userId: Long): Set<Long> =
        CourseTraceTable
            .select(CourseTraceTable.courseId)
            .where { (CourseTraceTable.userId eq userId) and (CourseTraceTable.status eq CourseTraceStatus.COMPLETED) }
            .withDistinct()
            .mapTo(mutableSetOf()) { it[CourseTraceTable.courseId] }

    /** 사용자의 따라가기 세션과 체크인을 전부 하드 삭제한다(탈퇴 정리). FK 때문에 체크인(자식) → 세션(부모) 순. */
    fun deleteAllByUser(userId: Long) {
        val traceIds = CourseTraceTable.select(CourseTraceTable.id).where { CourseTraceTable.userId eq userId }
        CourseTracePlaceTable.deleteWhere { tracingCourseId inSubQuery traceIds }
        CourseTraceTable.deleteWhere { CourseTraceTable.userId eq userId }
    }

    private fun ResultRow.toRow() =
        CourseTraceRow(
            id = this[CourseTraceTable.id].value,
            userId = this[CourseTraceTable.userId],
            courseId = this[CourseTraceTable.courseId],
            status = this[CourseTraceTable.status],
            startedAt = this[CourseTraceTable.createdAt].toJavaInstant(),
            endedAt = this[CourseTraceTable.endedAt]?.toJavaInstant(),
            durationMinutes = this[CourseTraceTable.durationMinutes],
            distanceMeters = this[CourseTraceTable.distanceMeters],
        )
}
