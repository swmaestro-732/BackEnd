package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.exposed.AddedPlaceTable
import com.example.backend.user.adapter.outbound.persistence.exposed.TracingCourseTable
import com.example.backend.user.application.port.outbound.TracingRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

/** tracing_courses·added_places 테이블 접근 리포지토리 — 따라가기 시작·체크인·완주 처리를 담당한다. */
@Repository
class TracingRepository {
    /** (user, course) 로 진행중(completed_at IS NULL)인 tracing id 를 반환한다(없으면 null). */
    fun findActiveByUserCourse(
        userId: Long,
        courseId: Long,
    ): Long? =
        TracingCourseTable
            .select(TracingCourseTable.id)
            .where {
                (TracingCourseTable.userId eq userId) and
                    (TracingCourseTable.courseId eq courseId) and
                    TracingCourseTable.completedAt.isNull()
            }.limit(1)
            .firstOrNull()
            ?.get(TracingCourseTable.id)
            ?.value

    /** 따라가기 레코드를 삽입하고 생성된 tracing id 를 반환한다(created_at 은 clientDefault 가 채운다). */
    fun insertTracing(
        userId: Long,
        courseId: Long,
    ): Long =
        TracingCourseTable
            .insert {
                it[TracingCourseTable.userId] = userId
                it[TracingCourseTable.courseId] = courseId
            }[TracingCourseTable.id]
            .value

    /** 사용자가 소유한 tracing 행(courseId·completedAt)을 반환한다(소유 아님/없음이면 null). */
    fun findOwned(
        userId: Long,
        tracingId: Long,
    ): TracingRow? =
        TracingCourseTable
            .selectAll()
            .where { (TracingCourseTable.id eq tracingId) and (TracingCourseTable.userId eq userId) }
            .singleOrNull()
            ?.let {
                TracingRow(
                    id = it[TracingCourseTable.id].value,
                    courseId = it[TracingCourseTable.courseId],
                    completedAt = it[TracingCourseTable.completedAt]?.toJavaInstant(),
                )
            }

    /** tracing 에 장소 체크인을 삽입한다 — (tracing, place) 유니크 제약과 짝지어 중복은 무시한다(멱등). */
    fun checkInPlace(
        tracingId: Long,
        placeId: Long,
    ) {
        AddedPlaceTable.insertIgnore {
            it[AddedPlaceTable.tracingCourseId] = tracingId
            it[AddedPlaceTable.placeId] = placeId
        }
    }

    /** tracing 에 체크인된 place 수를 센다(유니크 제약 덕에 곧 서로 다른 place 수다). */
    fun countCheckedPlaces(tracingId: Long): Int =
        AddedPlaceTable
            .selectAll()
            .where { AddedPlaceTable.tracingCourseId eq tracingId }
            .count()
            .toInt()

    /** tracing 을 완주 처리한다(completed_at 세팅). */
    fun markCompleted(
        tracingId: Long,
        at: Instant,
    ) {
        TracingCourseTable.update({ TracingCourseTable.id eq tracingId }) {
            it[completedAt] = at.toKotlinInstant()
        }
    }
}
