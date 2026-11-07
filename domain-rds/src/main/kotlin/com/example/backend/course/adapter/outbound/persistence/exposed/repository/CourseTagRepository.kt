package com.example.backend.course.adapter.outbound.persistence.exposed.repository

import com.example.backend.course.adapter.outbound.persistence.exposed.CourseTagTable
import com.example.backend.course.adapter.outbound.persistence.exposed.TagTable
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.springframework.stereotype.Repository

/**
 * course_tags 테이블 접근 리포지토리 — 코스와 태그의 연결(조인 테이블) 삽입.
 */
@Repository
class CourseTagRepository {
    /** 코스와 태그들의 연결을 배치 1문으로 삽입한다 — 생성 값이 없는 조인 테이블이라 RETURNING 을 끈다. */
    fun linkAll(
        courseId: Long,
        tagIds: List<Long>,
    ) {
        CourseTagTable.batchInsert(tagIds, shouldReturnGeneratedValues = false) { tagId ->
            this[CourseTagTable.courseId] = courseId
            this[CourseTagTable.tagId] = tagId
        }
    }

    /** 코스의 태그 연결을 모두 삭제한다(전체 치환 편집 전처리). */
    fun deleteByCourseId(courseId: Long) {
        CourseTagTable.deleteWhere { CourseTagTable.courseId eq courseId }
    }

    /**
     * 여러 코스의 태그명을 한 번에 읽는다(코스 id → 태그명 목록). course_tags→tags 조인.
     * 재색인 배치가 코스별로 태그를 개별 조회(N+1)하지 않도록 페이지 단위로 모아 읽는다.
     */
    fun findNamesByCourseIds(courseIds: List<Long>): Map<Long, List<String>> {
        if (courseIds.isEmpty()) return emptyMap()
        return CourseTagTable
            .join(TagTable, JoinType.INNER, CourseTagTable.tagId, TagTable.id)
            .select(CourseTagTable.courseId, TagTable.name)
            .where { CourseTagTable.courseId inList courseIds }
            .map { it[CourseTagTable.courseId] to it[TagTable.name] }
            .groupBy({ it.first }, { it.second })
    }
}
