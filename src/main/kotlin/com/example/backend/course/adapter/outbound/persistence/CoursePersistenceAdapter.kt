package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.course.application.port.outbound.CourseDetailRow
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CoursePlaceImageRow
import com.example.backend.course.application.port.outbound.CoursePlaceRow
import com.example.backend.course.domain.model.Course
import com.example.backend.course.domain.model.CourseStatus
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

/**
 * 아웃바운드 어댑터 — [CoursePersistencePort] 를 Exposed 로 구현한다(조회·생성).
 * courses·course_places·course_place_images 를 읽어 상세 조회 읽기 모델을 만들고,
 * 생성 시엔 courses → course_places → course_place_images 순으로 삽입한 뒤 태그를 tags(find-or-create)·course_tags 로 연결한다.
 * created_at·updated_at·카운터·status 기본값은 DB DEFAULT 에 맡긴다(status 만 명시).
 */
@Repository
class CoursePersistenceAdapter : CoursePersistencePort {
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

    override fun save(course: Course): Long {
        val courseId =
            CourseTable.insert {
                it[status] = CourseStatus.ACTIVE
                it[userId] = course.userId
                it[title] = course.title
                it[description] = course.description
                it[coverImageUrl] = course.coverImageUrl
                it[category] = course.category
                it[isPublished] = course.isPublished
                it[visibility] = course.visibility
            }[CourseTable.id]

        course.places.forEach { place ->
            val coursePlaceId =
                CoursePlaceTable.insert {
                    it[CoursePlaceTable.courseId] = courseId
                    it[placeId] = place.placeId
                    it[orderNo] = place.orderNo.toShort()
                    it[caption] = place.caption
                    // walking_minutes 는 서버 자동 계산(후속) — 생성 시 null.
                }[CoursePlaceTable.id]

            place.imageUrls.forEachIndexed { index, url ->
                CoursePlaceImageTable.insert {
                    it[CoursePlaceImageTable.coursePlaceId] = coursePlaceId
                    it[imageUrl] = url
                    it[orderNo] = index.toShort()
                }
            }
        }

        course.tags.forEach { tagName ->
            val resolvedTagId = findOrCreateTag(tagName)
            CourseTagTable.insert {
                it[CourseTagTable.courseId] = courseId
                it[CourseTagTable.tagId] = resolvedTagId
            }
        }

        return courseId
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

    /** 태그 이름으로 tags 행을 찾고 없으면 생성해 id 를 반환한다. */
    private fun findOrCreateTag(tagName: String): Long =
        TagTable
            .select(TagTable.id)
            .where { TagTable.name eq tagName }
            .singleOrNull()
            ?.get(TagTable.id)
            ?: TagTable.insert { it[name] = tagName }[TagTable.id]
}
