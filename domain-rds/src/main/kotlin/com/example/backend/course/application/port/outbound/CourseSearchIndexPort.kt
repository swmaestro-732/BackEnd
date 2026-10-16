package com.example.backend.course.application.port.outbound

import com.example.backend.course.domain.model.Course

/**
 * 아웃바운드 포트 — 코스를 검색 인덱스(OpenSearch)에 색인한다.
 * 검색은 부가 기능이라 색인 실패·미연결은 쓰기 흐름을 막지 않는다(구현체가 fail-soft·no-op 처리).
 */
interface CourseSearchIndexPort {
    /** 코스를 검색 인덱스에 저장한다(docId=course.id, upsert). */
    fun save(course: Course)

    /** 코스들을 검색 인덱스에 저장한다(docId=course.id, upsert). id 가 없는(미영속) 코스는 건너뛴다 — 재색인용 bulk. */
    fun save(courses: List<Course>)

    /** 코스를 색인에서 제거한다(삭제 시). */
    fun delete(courseId: Long)

    /** 작성자의 모든 코스를 색인에서 제거한다(회원 탈퇴 정리). */
    fun deleteByAuthor(authorId: Long)
}
