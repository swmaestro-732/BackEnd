package com.example.backend.course.adapter.outbound.persistence.exposed

import com.example.backend.course.adapter.outbound.persistence.CourseEntity
import com.example.backend.course.adapter.outbound.persistence.CourseTable
import com.example.backend.course.application.port.outbound.CourseDetailRow
import com.example.backend.course.domain.model.Course
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

/**
 * courses 테이블 접근 리포지토리 — 코스 본문의 조회·삽입만 담당한다.
 * 삽입은 DAO([CourseEntity])로 한다. created_at·updated_at·카운터는 [CourseTable] 의 클라이언트 기본값이 채우고
 * (DAO batch insert 는 DB 전용 DEFAULT 미지원), insert 후 [Entity.refresh] 로 확정 상태를 되읽는다.
 */
@Repository
class CourseRepository {
    /** 코스 본문을 삽입하고, 생성값(id·created_at·카운터 등)까지 적재된 엔티티를 반환한다(엔티티는 모듈 내부 구현이라 internal). */
    internal fun insert(course: Course): CourseEntity =
        CourseEntity
            .new {
                status = course.status
                userId = course.userId
                title = course.title
                description = course.description
                coverImageUrl = course.coverImageUrl
                category = course.category
                isPublished = course.isPublished
                visibility = course.visibility
                forkedFromId = course.forkedFromId
            }.also { it.refresh(flush = true) }

    /** deleted_at IS NULL 인 코스가 존재하는지만 확인한다(fork 원본 검증 등, 본문 미적재). */
    fun existsById(courseId: Long): Boolean =
        !CourseTable
            .selectAll()
            .where { (CourseTable.id eq courseId) and CourseTable.deletedAt.isNull() }
            .limit(1)
            .empty()

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
