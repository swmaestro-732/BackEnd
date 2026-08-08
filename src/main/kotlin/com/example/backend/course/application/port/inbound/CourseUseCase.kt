package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.CreateCourseCommand
import com.example.backend.course.application.port.inbound.dto.EditCourseCommand
import com.example.backend.course.domain.model.Course

/**
 * 인바운드 포트 — 코스 쓰기(커맨드) 유스케이스. 조회는 [CourseQueryUseCase] 가 담당한다.
 * - [create] 코스 생성(발행·임시저장 공통) — 저장된 코스 애그리거트(생성 id·DB 생성값 포함)를 반환한다.
 * - [edit] 코스 편집(전체 치환) — 소유자만 가능하며, 편집된 코스 애그리거트(DB 생성값 포함)를 반환한다.
 * - [delete] 코스 삭제(소프트 삭제) — 소유자만 가능하다. 성공/실패는 예외로 표현하므로 반환값은 없다.
 */
interface CourseUseCase {
    fun create(command: CreateCourseCommand): Course

    fun edit(command: EditCourseCommand): Course

    fun delete(
        userId: Long,
        courseId: Long,
    )
}
