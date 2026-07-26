package com.example.backend.course.adapter.outbound.persistence.exposed

import com.example.backend.course.adapter.outbound.persistence.CoursePlaceEntity
import com.example.backend.course.adapter.outbound.persistence.CoursePlaceTable
import com.example.backend.course.application.port.outbound.CoursePlaceRow
import com.example.backend.course.domain.model.CoursePlace
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
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
                    // walking_minutes 는 서버 자동 계산(후속) — 생성 시 null.
                }.id.value

        place.imageUrls.forEachIndexed { index, url ->
            coursePlaceImageRepository.insert(coursePlaceId, url, index)
        }
    }

    /** 코스의 장소들을 orderNo 오름차순으로, 각 장소의 이미지를 채워 반환한다. */
    fun findByCourseId(courseId: Long): List<CoursePlaceRow> {
        val placeRows =
            CoursePlaceTable
                .selectAll()
                .where { CoursePlaceTable.courseId eq courseId }
                .orderBy(CoursePlaceTable.orderNo to SortOrder.ASC)
                .toList()
        if (placeRows.isEmpty()) return emptyList()

        val imagesByPlace =
            coursePlaceImageRepository.findByCoursePlaceIds(
                placeRows.map { it[CoursePlaceTable.id].value },
            )

        return placeRows.map { row ->
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
