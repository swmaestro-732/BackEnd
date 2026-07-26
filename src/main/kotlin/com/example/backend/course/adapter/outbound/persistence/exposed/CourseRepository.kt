package com.example.backend.course.adapter.outbound.persistence.exposed

import com.example.backend.course.adapter.outbound.persistence.CourseEntity
import com.example.backend.course.adapter.outbound.persistence.CourseTable
import com.example.backend.course.application.port.outbound.CourseDetailRow
import com.example.backend.course.domain.model.Course
import com.example.backend.course.domain.model.CourseStatus
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

/**
 * courses 테이블 접근 리포지토리 — 코스 본문의 조회·삽입만 담당한다.
 * 삽입은 DAO([CourseEntity]), 조회는 DSL 로 한다(같은 테이블을 두 방식으로 사용하는 예).
 * created_at·updated_at·카운터 등은 미지정으로 두어 DB DEFAULT 에 맡긴다(status 만 명시).
 */
@Repository
class CourseRepository {
    fun insert(course: Course): Long =
        CourseEntity
            .new {
                status = CourseStatus.ACTIVE
                userId = course.userId
                title = course.title
                description = course.description
                coverImageUrl = course.coverImageUrl
                category = course.category
                isPublished = course.isPublished
                visibility = course.visibility
            }.id.value

    /** deleted_at IS NULL 인 코스 본문만 읽어 상세 읽기 모델을 만든다(상태·공개범위 판정은 서비스). */
    fun findDetail(courseId: Long): CourseDetailRow? =
        CourseTable
            .selectAll()
            .where { (CourseTable.id eq courseId) and CourseTable.deletedAt.isNull() }
            .singleOrNull()
            ?.let {
                CourseDetailRow(
                    id = it[CourseTable.id].value,
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
}
