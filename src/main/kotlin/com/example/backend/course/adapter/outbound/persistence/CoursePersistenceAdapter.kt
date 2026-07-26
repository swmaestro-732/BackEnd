package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.course.adapter.outbound.persistence.exposed.CoursePlaceRepository
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseRepository
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseTagRepository
import com.example.backend.course.adapter.outbound.persistence.exposed.TagRepository
import com.example.backend.course.application.port.outbound.CourseDetailRow
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CoursePlaceRow
import com.example.backend.course.domain.model.Course
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [CoursePersistencePort] 를 구현한다(조회·생성).
 * 실제 테이블 접근은 테이블별 리포지토리([CourseRepository]·[CoursePlaceRepository]·[TagRepository]·[CourseTagRepository])에
 * 위임하고, 이 어댑터는 애그리거트 저장 순서(courses → course_places → 태그 연결)만 조율한다.
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

    override fun findPlaces(courseId: Long): List<CoursePlaceRow> = coursePlaceRepository.findByCourseId(courseId)

    override fun save(course: Course): Course {
        val courseEntity = courseRepository.insert(course)
        val courseId = courseEntity.id.value

        course.places.forEach { place -> coursePlaceRepository.insert(courseId, place) }

        course.tags.forEach { tagName ->
            courseTagRepository.link(courseId, tagRepository.findOrCreate(tagName))
        }

        // 자식(tags·places)은 방금 저장한 입력 애그리거트를 재사용해 조립한다(도메인은 자식의 생성 id 를 담지 않음).
        return courseEntity.toDomain(course.tags, course.places)
    }
}
