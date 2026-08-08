package com.example.backend.course.adapter.outbound.persistence.exposed.repository

import com.example.backend.course.adapter.outbound.persistence.CoursePlaceEntity
import com.example.backend.course.adapter.outbound.persistence.CoursePlaceTable
import com.example.backend.course.application.port.outbound.CoursePlaceRow
import com.example.backend.course.domain.model.CoursePlace
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

/**
 * course_places 테이블 접근 리포지토리 — 코스에 담긴 장소와 그 이미지를 한 단위로 다룬다.
 * 이미지 영속은 [CoursePlaceImageRepository] 에 위임한다.
 */
@Repository
class CoursePlaceRepository(
    private val coursePlaceImageRepository: CoursePlaceImageRepository,
) {
    /** 장소 한 곳을 삽입하고, 그 장소의 이미지들을 순서대로 함께 저장한다. */
    fun insert(
        courseId: Long,
        place: CoursePlace,
    ) {
        val coursePlaceId =
            CoursePlaceEntity
                .new {
                    this.courseId = courseId
                    placeId = place.placeId
                    orderNo = place.orderNo.toShort()
                    caption = place.caption
                    // 요청이 보낸 값을 그대로 저장한다 — -1(도보 이동 불가)·null(마지막 장소) 포함.
                    walkingMinutes = place.walkingMinutes
                }.id.value

        place.imageUrls.forEachIndexed { index, url ->
            coursePlaceImageRepository.insert(coursePlaceId, url, index)
        }
    }

    /** 코스에 담긴 모든 장소와 그 이미지를 삭제한다(전체 치환 편집 전처리) — 이미지 삭제 후 장소를 지운다. */
    fun deleteByCourseId(courseId: Long) {
        val placeIds =
            CoursePlaceTable
                .select(CoursePlaceTable.id)
                .where { CoursePlaceTable.courseId eq courseId }
                .map { it[CoursePlaceTable.id].value }
        if (placeIds.isEmpty()) return
        coursePlaceImageRepository.deleteByCoursePlaceIds(placeIds)
        CoursePlaceTable.deleteWhere { CoursePlaceTable.courseId eq courseId }
    }

    /** 코스의 장소들을 orderNo 오름차순으로, 각 장소의 이미지를 채워 반환한다. */
    fun findByCourseId(courseId: Long): List<CoursePlaceRow> = findByCourseIds(listOf(courseId))[courseId].orEmpty()

    /** 여러 코스의 장소들을 courseId 별로(각 코스 안은 orderNo 오름차순) 한 번에 읽는다 — 이미지도 배치로 채운다. */
    fun findByCourseIds(courseIds: List<Long>): Map<Long, List<CoursePlaceRow>> {
        if (courseIds.isEmpty()) return emptyMap()
        val placeRows =
            CoursePlaceTable
                .selectAll()
                .where { CoursePlaceTable.courseId inList courseIds }
                .orderBy(CoursePlaceTable.orderNo to SortOrder.ASC)
                .toList()
        if (placeRows.isEmpty()) return emptyMap()

        val imagesByPlace =
            coursePlaceImageRepository.findByCoursePlaceIds(
                placeRows.map { it[CoursePlaceTable.id].value },
            )

        // orderNo 전역 정렬이라 courseId 로 묶어도 각 코스 안 순서가 유지된다.
        return placeRows
            .groupBy { it[CoursePlaceTable.courseId] }
            .mapValues { (_, rows) ->
                rows.map { row ->
                    val coursePlaceId = row[CoursePlaceTable.id].value
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
    }
}
