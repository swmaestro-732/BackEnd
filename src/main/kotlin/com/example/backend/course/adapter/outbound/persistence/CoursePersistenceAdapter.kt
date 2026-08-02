package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.course.adapter.outbound.persistence.exposed.repository.CoursePlaceRepository
import com.example.backend.course.adapter.outbound.persistence.exposed.repository.CourseRepository
import com.example.backend.course.adapter.outbound.persistence.exposed.repository.CourseTagRepository
import com.example.backend.course.adapter.outbound.persistence.exposed.repository.TagRepository
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

    override fun findPublishedByAuthor(authorId: Long): List<CourseSummaryRow> =
        courseRepository.findPublishedByAuthor(authorId)

    override fun findPublishedPublic(limit: Int): List<CourseSummaryRow> = courseRepository.findPublishedPublic(limit)

    override fun existsById(courseId: Long): Boolean = courseRepository.existsById(courseId)

    override fun findPlaces(courseId: Long): List<CoursePlaceRow> = coursePlaceRepository.findByCourseId(courseId)

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

    /** 코스에 담긴 장소·이미지와 태그 연결을 심는다(생성·편집 공용). */
    private fun insertChildren(
        courseId: Long,
        course: Course,
    ) {
        course.places.forEach { place -> coursePlaceRepository.insert(courseId, place) }
        course.tags.forEach { tagName ->
            courseTagRepository.link(courseId, tagRepository.findOrCreate(tagName))
        }
    }
}
