package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.CreateCourseCommand
import com.example.backend.course.application.port.inbound.dto.EditCourseCommand
import com.example.backend.course.application.port.inbound.dto.ForkCourseCommand
import com.example.backend.course.domain.model.Course

/**
 * 인바운드 포트 — 코스 쓰기(커맨드) 유스케이스. 조회는 [CourseQueryUseCase] 가 담당한다.
 */
interface CourseUseCase {
    fun 코스생성(command: CreateCourseCommand): Course

    fun 코스수정(command: EditCourseCommand): Course

    fun fork(command: ForkCourseCommand): Course

    fun delete(
        userId: Long,
        courseId: Long,
    )

    /** 작성자의 살아있는 코스를 전부 소프트 삭제한다 — 회원 탈퇴 정리용(user 도메인이 호출). */
    fun deleteAllByAuthor(authorId: Long)
}
