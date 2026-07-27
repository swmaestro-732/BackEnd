package com.example.backend.course.adapter.outbound.persistence.exposed

import com.example.backend.course.adapter.outbound.persistence.CoursePlaceImageEntity
import com.example.backend.course.adapter.outbound.persistence.CoursePlaceImageTable
import com.example.backend.course.application.port.outbound.CoursePlaceImageRow
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

/**
 * course_place_images 테이블 접근 리포지토리 — 장소별 이미지의 조회·삽입.
 */
@Repository
class CoursePlaceImageRepository {
    fun insert(
        coursePlaceId: Long,
        imageUrl: String,
        orderNo: Int,
    ) {
        CoursePlaceImageEntity.new {
            this.coursePlaceId = coursePlaceId
            this.imageUrl = imageUrl
            this.orderNo = orderNo.toShort()
        }
    }

    /** 주어진 course_place id 들의 이미지를 모두 삭제한다(전체 치환 편집 전처리). */
    fun deleteByCoursePlaceIds(coursePlaceIds: List<Long>) {
        if (coursePlaceIds.isEmpty()) return
        CoursePlaceImageTable.deleteWhere { coursePlaceId inList coursePlaceIds }
    }

    /** 주어진 course_place id 들의 이미지를 orderNo 오름차순으로 묶어 반환한다. */
    fun findByCoursePlaceIds(coursePlaceIds: List<Long>): Map<Long, List<CoursePlaceImageRow>> =
        CoursePlaceImageTable
            .selectAll()
            .where { CoursePlaceImageTable.coursePlaceId inList coursePlaceIds }
            .orderBy(CoursePlaceImageTable.orderNo to SortOrder.ASC)
            .groupBy({ it[CoursePlaceImageTable.coursePlaceId] }, ::toRow)

    private fun toRow(row: ResultRow): CoursePlaceImageRow =
        CoursePlaceImageRow(
            imageUrl = row[CoursePlaceImageTable.imageUrl],
            orderNo = row[CoursePlaceImageTable.orderNo]?.toInt() ?: 0,
        )
}
