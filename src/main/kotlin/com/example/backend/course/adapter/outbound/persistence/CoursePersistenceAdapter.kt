package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.adapter.outbound.persistence.exposed.repository.CoursePlaceRepository
import com.example.backend.course.adapter.outbound.persistence.exposed.repository.CourseRepository
import com.example.backend.course.adapter.outbound.persistence.exposed.repository.CourseTagRepository
import com.example.backend.course.adapter.outbound.persistence.exposed.repository.TagRepository
import com.example.backend.course.application.port.inbound.dto.AuthorCourseCursor
import com.example.backend.course.application.port.inbound.dto.FeedCursor
import com.example.backend.course.application.port.outbound.CourseDetailRow
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CoursePlaceRow
import com.example.backend.course.application.port.outbound.CourseSummaryRow
import com.example.backend.course.domain.model.Course
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [CoursePersistencePort] 를 구현한다(조회·생성·편집).
 * 실제 테이블 접근은 테이블별 리포지토리([CourseRepository]·[CoursePlaceRepository]·[TagRepository]·[CourseTagRepository])에
 * 위임하고, 이 어댑터는 애그리거트 저장 순서(courses → course_places → 태그 연결)만 조율한다.
 * 편집(전체 치환)은 코스 본문을 갱신하고 기존 장소·이미지·태그 연결을 지운 뒤 요청 값으로 다시 심는다.
 * 트랜잭션 경계는 호출하는 서비스(@Transactional)가 소유한다.
 */
@Component
class CoursePersistenceAdapter(
    private val courseRepository: CourseRepository,
    private val coursePlaceRepository: CoursePlaceRepository,
    private val tagRepository: TagRepository,
    private val courseTagRepository: CourseTagRepository,
) : CoursePersistencePort {
    override fun findCourseDetail(courseId: Long): CourseDetailRow? = courseRepository.findDetail(courseId)

    override fun findCourseDetails(courseIds: List<Long>): List<CourseDetailRow> =
        courseRepository.findDetails(courseIds)

    override fun findPublishedByAuthor(
        authorId: Long,
        visibilities: Set<CourseVisibility>,
        cursor: AuthorCourseCursor?,
        size: Int,
    ): List<CourseSummaryRow> = courseRepository.findPublishedByAuthor(authorId, visibilities, cursor, size)

    override fun findDraftsByAuthor(authorId: Long): List<CourseSummaryRow> =
        courseRepository.findDraftsByAuthor(authorId)

    override fun findPublishedPublic(
        cursor: FeedCursor?,
        size: Int,
    ): List<CourseSummaryRow> = courseRepository.findPublishedPublic(cursor, size)

    override fun existsById(courseId: Long): Boolean = courseRepository.existsById(courseId)

    override fun findPlaces(courseId: Long): List<CoursePlaceRow> = coursePlaceRepository.findByCourseId(courseId)

    override fun findPlacesByCourseIds(courseIds: List<Long>): Map<Long, List<CoursePlaceRow>> =
        coursePlaceRepository.findByCourseIds(courseIds)

    override fun save(course: Course): Course {
        val courseEntity = courseRepository.insert(course)
        insertChildren(courseEntity.id.value, course)
        // 자식(tags·places)은 방금 저장한 입력 애그리거트를 재사용해 조립한다(도메인은 자식의 생성 id 를 담지 않음).
        return courseEntity.toDomain(course.tags, course.places)
    }

    override fun update(course: Course): Course {
        val courseId = checkNotNull(course.id) { "영속화된 Course 는 id 를 가진다." }
        val courseEntity = courseRepository.update(course)
        // 전체 치환 — 기존 장소·이미지·태그 연결을 지우고 요청 값으로 다시 심는다.
        coursePlaceRepository.deleteByCourseId(courseId)
        courseTagRepository.deleteByCourseId(courseId)
        insertChildren(courseId, course)
        // 자식(tags·places)은 방금 저장한 입력 애그리거트를 재사용해 조립한다(도메인은 자식의 생성 id 를 담지 않음).
        return courseEntity.toDomain(course.tags, course.places)
    }

    override fun softDelete(courseId: Long): Int = courseRepository.softDelete(courseId)

    override fun softDeleteAllByAuthor(authorId: Long): Int = courseRepository.softDeleteAllByAuthor(authorId)

    override fun increaseSavesCount(courseId: Long): Int = courseRepository.increaseSavesCount(courseId)

    override fun decreaseSavesCount(courseId: Long): Int = courseRepository.decreaseSavesCount(courseId)

    override fun increaseCommentsCount(courseId: Long): Int = courseRepository.increaseCommentsCount(courseId)

    override fun decreaseCommentsCount(courseId: Long): Int = courseRepository.decreaseCommentsCount(courseId)

    override fun findForIndex(
        afterId: Long?,
        limit: Int,
    ): List<Course> {
        val entities = courseRepository.findForIndex(afterId, limit)
        // 재색인 문서도 태그 검색 대상이 되도록 태그를 채운다(태그를 비우면 재색인분이 태그 필터에서 누락).
        // 페이지의 코스 id 를 모아 태그를 배치로 읽어 N+1 을 피한다. 장소는 색인 문서에 쓰지 않아 비운다.
        val tagsByCourse = courseTagRepository.findNamesByCourseIds(entities.map { it.id.value })
        return entities.map { it.toDomain(tagsByCourse[it.id.value] ?: emptyList(), emptyList()) }
    }

    /** 코스에 담긴 장소·이미지와 태그 연결을 심는다(생성·편집 공용) — 테이블별 배치 insert 로 왕복을 줄인다. */
    private fun insertChildren(
        courseId: Long,
        course: Course,
    ) {
        coursePlaceRepository.insertAll(courseId, course.places)
        courseTagRepository.linkAll(courseId, tagRepository.findOrCreateAll(course.tags))
    }
}
