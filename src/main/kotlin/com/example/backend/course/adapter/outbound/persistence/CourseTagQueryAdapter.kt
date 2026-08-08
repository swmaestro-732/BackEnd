package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.course.application.port.outbound.CourseTagQueryPort
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.countDistinct
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import org.springframework.stereotype.Repository

/**
 * 아웃바운드 어댑터 — [CourseTagQueryPort] 를 Exposed 로 구현한다.
 * 추천 근거: 해당 장소가 포함된 코스들이 사용한 태그의 코스 수 내림차순(동률은 이름 오름차순).
 */
@Repository
class CourseTagQueryAdapter : CourseTagQueryPort {
    private val courseCount = CourseTagTable.courseId.countDistinct()

    /**
     * 코스 한 건의 태그명. 정렬하지 않고 DB 가 준 순서 그대로 내보낸다 —
     * 표시 순서를 쓰는 화면이 없어 `ORDER BY` 비용을 두지 않는다.
     */
    override fun findTagNamesByCourseId(courseId: Long): List<String> =
        CourseTagTable
            .join(TagTable, JoinType.INNER, CourseTagTable.tagId, TagTable.id)
            .select(TagTable.name)
            .where { CourseTagTable.courseId eq courseId }
            .map { it[TagTable.name] }

    override fun findTagNamesByPlaceIds(
        placeIds: List<Long>,
        limit: Int,
    ): List<String> =
        CoursePlaceTable
            .join(CourseTagTable, JoinType.INNER, CoursePlaceTable.courseId, CourseTagTable.courseId)
            .join(TagTable, JoinType.INNER, CourseTagTable.tagId, TagTable.id)
            .select(TagTable.name, courseCount)
            .where { CoursePlaceTable.placeId inList placeIds }
            .groupBy(TagTable.name)
            .orderBy(courseCount to SortOrder.DESC, TagTable.name to SortOrder.ASC)
            .limit(limit)
            .map { it[TagTable.name] }

    override fun findPopularTagNames(limit: Int): List<String> =
        CourseTagTable
            .join(TagTable, JoinType.INNER, CourseTagTable.tagId, TagTable.id)
            .select(TagTable.name, courseCount)
            .groupBy(TagTable.name)
            .orderBy(courseCount to SortOrder.DESC, TagTable.name to SortOrder.ASC)
            .limit(limit)
            .map { it[TagTable.name] }
}
