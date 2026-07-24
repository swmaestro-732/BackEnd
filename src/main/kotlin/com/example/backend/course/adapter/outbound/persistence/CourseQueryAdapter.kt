package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.course.application.port.outbound.CourseDetailRow
import com.example.backend.course.application.port.outbound.CoursePlaceImageRow
import com.example.backend.course.application.port.outbound.CoursePlaceRow
import com.example.backend.course.application.port.outbound.CourseQueryPort
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

/**
 * 아웃바운드 어댑터 — [CourseQueryPort] 를 Exposed 로 구현한다.
 * courses·course_places·course_place_images 를 읽어 상세 조회 읽기 모델을 만든다.
 */
@Repository
class CourseQueryAdapter : CourseQueryPort {
    override fun findCourseDetail(courseId: Long): CourseDetailRow? =
        CourseTable
            .selectAll()
            .where { (CourseTable.id eq courseId) and CourseTable.deletedAt.isNull() }
            .singleOrNull()
            ?.let {
                CourseDetailRow(
                    id = it[CourseTable.id],
                    userId = it[CourseTable.userId],
                    title = it[CourseTable.title],
                    coverImageUrl = it[CourseTable.coverImageUrl],
                    description = it[CourseTable.description],
                    category = it[CourseTable.category],
                    tracingsCnt = it[CourseTable.tracingsCnt],
                    status = it[CourseTable.status],
                    visibility = it[CourseTable.visibility],
                )
            }

    override fun findPlaces(courseId: Long): List<CoursePlaceRow> {
        val placeRows =
            CoursePlaceTable
                .selectAll()
                .where { CoursePlaceTable.courseId eq courseId }
                .orderBy(CoursePlaceTable.orderNo to SortOrder.ASC)
                .toList()
        if (placeRows.isEmpty()) return emptyList()

        val imagesByPlace = findImagesByPlace(placeRows.map { it[CoursePlaceTable.id] })

        return placeRows.map { row ->
            val coursePlaceId = row[CoursePlaceTable.id]
            CoursePlaceRow(
                id = coursePlaceId,
                placeId = row[CoursePlaceTable.placeId],
                orderNo = row[CoursePlaceTable.orderNo].toInt(),
                caption = row[CoursePlaceTable.caption],
                walkingMinutes = row[CoursePlaceTable.walkingMinutes],
                images = imagesByPlace[coursePlaceId].orEmpty(),
            )
        }
    }

    private fun findImagesByPlace(coursePlaceIds: List<Long>): Map<Long, List<CoursePlaceImageRow>> =
        CoursePlaceImageTable
            .selectAll()
            .where { CoursePlaceImageTable.coursePlaceId inList coursePlaceIds }
            .orderBy(CoursePlaceImageTable.orderNo to SortOrder.ASC)
            .groupBy({ it[CoursePlaceImageTable.coursePlaceId] }, ::toImageRow)

    private fun toImageRow(row: ResultRow): CoursePlaceImageRow =
        CoursePlaceImageRow(
            imageUrl = row[CoursePlaceImageTable.imageUrl],
            orderNo = row[CoursePlaceImageTable.orderNo]?.toInt() ?: 0,
        )
}
