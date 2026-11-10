package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.course.adapter.outbound.persistence.exposed.CourseCategoryTable
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseEntity
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseTable
import com.example.backend.course.adapter.outbound.persistence.exposed.repository.CourseCategoryRepository
import com.example.backend.course.adapter.outbound.persistence.exposed.repository.CourseTagRepository
import com.example.backend.course.application.port.inbound.dto.AdminCourseStatus
import com.example.backend.course.application.port.outbound.AdminCourseFilter
import com.example.backend.course.application.port.outbound.AdminCoursePersistencePort
import com.example.backend.course.application.port.outbound.AdminCourseRow
import com.example.backend.course.domain.model.Course
import com.example.backend.course.domain.model.CourseCategory
import com.example.backend.course.domain.model.CourseStatus
import org.jetbrains.exposed.v1.core.LikePattern
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Component
import kotlin.time.Clock
import kotlin.time.toJavaInstant

/**
 * 아웃바운드 어댑터: [AdminCoursePersistencePort]. 사용자 경로([CoursePersistenceAdapter])와 달리 상태로 거르지 않는다.
 * 목록은 OpenSearch 가 아니라 DB 에서 바로 읽는다(숨김, 초안, 삭제 코스도 봐야 해서).
 */
@Component
class AdminCoursePersistenceAdapter(
    private val courseCategoryRepository: CourseCategoryRepository,
    private val courseTagRepository: CourseTagRepository,
) : AdminCoursePersistencePort {
    override fun findPage(
        filter: AdminCourseFilter,
        offset: Long,
        limit: Int,
    ): List<AdminCourseRow> {
        val rows =
            CourseTable
                .selectAll()
                .where(condition(filter))
                .orderBy(CourseTable.id to SortOrder.DESC)
                .limit(limit)
                .offset(offset)
                .toList()
        val categoriesByCourse = courseCategoryRepository.findByCourseIds(rows.map { it[CourseTable.id].value })
        return rows.map { it.toRow(categoriesByCourse) }
    }

    override fun count(filter: AdminCourseFilter): Long = CourseTable.selectAll().where(condition(filter)).count()

    override fun findById(
        courseId: Long,
        lock: Boolean,
    ): AdminCourseRow? {
        val query = CourseTable.selectAll().where { CourseTable.id eq courseId }
        val row = (if (lock) query.forUpdate() else query).singleOrNull() ?: return null
        return row.toRow(courseCategoryRepository.findByCourseIds(listOf(courseId)))
    }

    override fun changeStatus(
        courseId: Long,
        from: CourseStatus,
        to: CourseStatus,
    ): Course? {
        val affected =
            CourseTable.update({
                (CourseTable.id eq courseId) and (CourseTable.status eq from) and CourseTable.deletedAt.isNull()
            }) {
                it[status] = to
                it[updatedAt] = Clock.System.now()
            }
        return if (affected == 0) null else loadForIndex(courseId)
    }

    override fun restore(courseId: Long): Course? {
        val affected =
            CourseTable.update({
                (CourseTable.id eq courseId) and (CourseTable.status eq CourseStatus.DELETED) and
                    CourseTable.deletedAt.isNotNull()
            }) {
                it[status] = CourseStatus.HIDDEN
                it[deletedAt] = null
                it[updatedAt] = Clock.System.now()
            }
        return if (affected == 0) null else loadForIndex(courseId)
    }

    /** 색인 문서에 쓰는 태그와 카테고리까지 채운 코스(장소는 색인에 쓰지 않아 비움, findForIndex 와 같은 방식). */
    private fun loadForIndex(courseId: Long): Course {
        val entity = CourseEntity.findById(courseId) ?: error("갱신 직후 코스를 되읽지 못했습니다: id=$courseId")
        entity.refresh()
        val ids = listOf(courseId)
        return entity.toDomain(
            courseTagRepository.findNamesByCourseIds(ids)[courseId].orEmpty(),
            emptyList(),
            courseCategoryRepository.findByCourseIds(ids)[courseId].orEmpty(),
        )
    }

    private fun condition(filter: AdminCourseFilter): Op<Boolean> {
        var condition: Op<Boolean> =
            when (filter.status) {
                null -> {
                    CourseTable.deletedAt.isNull()
                }

                AdminCourseStatus.ACTIVE -> {
                    activeWith(published = true)
                }

                AdminCourseStatus.DRAFT -> {
                    activeWith(published = false)
                }

                AdminCourseStatus.HIDDEN -> {
                    // 목록 표시(AdminCourseStatus.of)와 같게 SUSPENDED 도 숨김으로 본다.
                    (CourseTable.status inList listOf(CourseStatus.HIDDEN, CourseStatus.SUSPENDED)) and
                        CourseTable.deletedAt.isNull()
                }

                AdminCourseStatus.DELETED -> {
                    CourseTable.status eq CourseStatus.DELETED
                }
            }
        filter.authorId?.let { condition = condition and (CourseTable.userId eq it) }
        filter.excludeAuthorId?.let { condition = condition and (CourseTable.userId neq it) }
        filter.areaCodePrefix?.let { condition = condition and (CourseTable.areaCode like "$it%") }
        filter.category?.let {
            val courseIds =
                CourseCategoryTable.select(CourseCategoryTable.courseId).where { CourseCategoryTable.category eq it }
            condition = condition and (CourseTable.id inSubQuery courseIds)
        }
        filter.keyword?.let { keyword ->
            var matched: Op<Boolean> = CourseTable.title.lowerCase() like containsPattern(keyword)
            if (filter.keywordAuthorIds.isNotEmpty()) {
                matched =
                    matched or (CourseTable.userId inList filter.keywordAuthorIds)
            }
            filter.keywordCourseId?.let { matched = matched or (CourseTable.id eq it) }
            condition = condition and matched
        }
        return condition
    }

    /** 부분 일치 패턴. 검색어의 %, _, \ 는 문자 그대로 찾도록 이스케이프한다. */
    private fun containsPattern(keyword: String) =
        LikePattern("%", ESCAPE) + LikePattern.ofLiteral(keyword.lowercase(), ESCAPE) + "%"

    private fun activeWith(published: Boolean): Op<Boolean> =
        (CourseTable.status eq CourseStatus.ACTIVE) and (CourseTable.isPublished eq published) and
            CourseTable.deletedAt.isNull()

    private fun ResultRow.toRow(categoriesByCourse: Map<Long, List<CourseCategory>>) =
        AdminCourseRow(
            id = this[CourseTable.id].value,
            userId = this[CourseTable.userId],
            title = this[CourseTable.title],
            description = this[CourseTable.description],
            coverImageUrl = this[CourseTable.coverImageUrl],
            categories = categoriesByCourse[this[CourseTable.id].value].orEmpty(),
            area = this[CourseTable.area],
            areaCode = this[CourseTable.areaCode],
            status = this[CourseTable.status],
            isPublished = this[CourseTable.isPublished],
            visibility = this[CourseTable.visibility],
            likesCnt = this[CourseTable.likesCnt],
            savesCnt = this[CourseTable.savesCnt],
            createdAt = this[CourseTable.createdAt].toJavaInstant(),
            updatedAt = this[CourseTable.updatedAt].toJavaInstant(),
        )

    private companion object {
        const val ESCAPE = '\\'
    }
}
