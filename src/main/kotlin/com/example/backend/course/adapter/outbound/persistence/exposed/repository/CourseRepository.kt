package com.example.backend.course.adapter.outbound.persistence.exposed.repository

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.course.adapter.outbound.persistence.CourseEntity
import com.example.backend.course.adapter.outbound.persistence.CourseTable
import com.example.backend.course.application.port.outbound.CourseDetailRow
import com.example.backend.course.application.port.outbound.CourseSummaryRow
import com.example.backend.course.domain.model.Course
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.course.domain.model.CourseVisibility
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.minus
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import kotlin.time.Clock
import kotlin.time.toJavaInstant

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

    /**
     * 코스 본문을 전체 치환으로 갱신하고 updated_at 을 명시로 새로 채운다(장소·태그 연결은 어댑터가 별도 조율).
     * 존재·소유권은 서비스가 사전 검증하므로 여기서는 id·deleted_at IS NULL 로 원자 갱신만 한다.
     * 갱신값(updated_at 등)까지 반영된 엔티티를 되읽어 반환한다.
     */
    internal fun update(course: Course): CourseEntity {
        val courseId = checkNotNull(course.id) { "영속화된 Course 는 id 를 가진다." }
        val now = Clock.System.now()
        // deleted_at IS NULL 가드를 WHERE 에 담아 원자 갱신한다(softDelete 와 같은 이유) —
        // 엔티티를 적재해 flush 하면 PK 만으로 UPDATE 가 나가 조회~flush 사이 동시 소프트 삭제를 덮어쓸 수 있다.
        val affected =
            CourseTable.update({ (CourseTable.id eq courseId) and CourseTable.deletedAt.isNull() }) {
                it[title] = course.title
                it[description] = course.description
                it[coverImageUrl] = course.coverImageUrl
                it[category] = course.category
                it[isPublished] = course.isPublished
                it[visibility] = course.visibility
                it[updatedAt] = now
            }
        // 서비스가 존재·소유권을 사전 검증하므로 0행은 동시 소프트 삭제가 이긴 경우 — 500 대신 404 로 드러낸다.
        if (affected == 0) {
            throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "갱신할 코스를 찾을 수 없습니다: id=$courseId")
        }
        // 갱신하지 않은 컬럼(created_at·카운터·area 등)까지 담아 도메인을 재구성하도록 확정 상태를 되읽는다.
        return CourseEntity.findById(courseId) ?: error("갱신 직후 코스를 되읽지 못했습니다: id=$courseId")
    }

    /**
     * deleted_at IS NULL 인 행에만 소프트 삭제 스탬프를 찍는다(deleted_at·updated_at 세팅, status=DELETED).
     * WHERE 의 deleted_at IS NULL 가드 덕에 동시 이중 삭제가 와도 두 번째 호출은 0행이라 안전하다.
     * 반환은 영향받은 행 수(0 또는 1).
     */
    fun softDelete(courseId: Long): Int {
        val now = Clock.System.now()
        return CourseTable.update({ (CourseTable.id eq courseId) and CourseTable.deletedAt.isNull() }) {
            it[deletedAt] = now
            it[status] = CourseStatus.DELETED
            it[updatedAt] = now
        }
    }

    /** deleted_at IS NULL 인 행의 saves_cnt 를 1 증가시킨다. 반환은 영향받은 행 수(0 또는 1). */
    fun increaseSavesCount(courseId: Long): Int =
        CourseTable.update({ (CourseTable.id eq courseId) and CourseTable.deletedAt.isNull() }) {
            it[savesCnt] = savesCnt + 1
        }

    /** deleted_at IS NULL 인 행의 saves_cnt 를 1 감소시킨다. 반환은 영향받은 행 수(0 또는 1). */
    fun decreaseSavesCount(courseId: Long): Int =
        CourseTable.update({ (CourseTable.id eq courseId) and CourseTable.deletedAt.isNull() }) {
            it[savesCnt] = savesCnt - 1
        }

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
            ?.let(::toDetailRow)

    /** deleted_at IS NULL 인 코스 본문들을 id 목록으로 한 번에 읽는다(없는 id 는 빠짐). */
    fun findDetails(courseIds: List<Long>): List<CourseDetailRow> {
        if (courseIds.isEmpty()) return emptyList()
        return CourseTable
            .selectAll()
            .where { (CourseTable.id inList courseIds) and CourseTable.deletedAt.isNull() }
            .map(::toDetailRow)
    }

    private fun toDetailRow(it: org.jetbrains.exposed.v1.core.ResultRow): CourseDetailRow =
        CourseDetailRow(
            id = it[CourseTable.id].value,
            userId = it[CourseTable.userId],
            title = it[CourseTable.title],
            coverImageUrl = it[CourseTable.coverImageUrl],
            description = it[CourseTable.description],
            category = it[CourseTable.category],
            area = it[CourseTable.area],
            tracingsCnt = it[CourseTable.tracingsCnt],
            status = it[CourseTable.status],
            visibility = it[CourseTable.visibility],
            isPublished = it[CourseTable.isPublished],
        )

    /** 작성자의 발행·활성 코스 요약을 createdAt 내림차순으로 읽는다(공개범위 필터는 서비스). */
    fun findPublishedByAuthor(authorId: Long): List<CourseSummaryRow> =
        CourseTable
            .selectAll()
            .where {
                (CourseTable.userId eq authorId) and
                    (CourseTable.isPublished eq true) and
                    (CourseTable.status eq CourseStatus.ACTIVE) and
                    CourseTable.deletedAt.isNull()
            }.orderBy(CourseTable.createdAt to SortOrder.DESC)
            .map(::toSummaryRow)

    /** 작성자의 임시저장·활성 코스 요약을 updatedAt 내림차순으로 읽는다. */
    fun findDraftsByAuthor(authorId: Long): List<CourseSummaryRow> =
        CourseTable
            .selectAll()
            .where {
                (CourseTable.userId eq authorId) and
                    (CourseTable.isPublished eq false) and
                    (CourseTable.status eq CourseStatus.ACTIVE) and
                    CourseTable.deletedAt.isNull()
            }.orderBy(CourseTable.updatedAt to SortOrder.DESC)
            .map(::toSummaryRow)

    /**
     * 전체 공개(visibility=PUBLIC)·발행·활성 코스 요약을 createdAt 내림차순으로 limit 개까지 읽는다(피드 후보).
     * visibility 는 enumerationByName 컬럼이라 enum 값([CourseVisibility.PUBLIC])으로 비교하면 이름 문자열로 매칭된다.
     */
    fun findPublishedPublic(limit: Int): List<CourseSummaryRow> =
        CourseTable
            .selectAll()
            .where {
                (CourseTable.isPublished eq true) and
                    (CourseTable.status eq CourseStatus.ACTIVE) and
                    CourseTable.deletedAt.isNull() and
                    (CourseTable.visibility eq CourseVisibility.PUBLIC)
            }.orderBy(CourseTable.savesCnt to SortOrder.DESC, CourseTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .map(::toSummaryRow)

    /** 코스 목록 쿼리의 공통 컬럼을 범용 요약 읽기 모델로 변환한다. */
    private fun toSummaryRow(it: org.jetbrains.exposed.v1.core.ResultRow): CourseSummaryRow =
        CourseSummaryRow(
            id = it[CourseTable.id].value,
            userId = it[CourseTable.userId],
            title = it[CourseTable.title],
            coverImageUrl = it[CourseTable.coverImageUrl],
            category = it[CourseTable.category],
            visibility = it[CourseTable.visibility],
            isPublished = it[CourseTable.isPublished],
            likesCnt = it[CourseTable.likesCnt],
            savesCnt = it[CourseTable.savesCnt],
            createdAt = it[CourseTable.createdAt].toJavaInstant(),
        )
}
