package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.CreateCourseCommand
import com.example.backend.course.application.port.inbound.dto.EditCourseCommand
import com.example.backend.course.application.port.inbound.dto.ForkCourseCommand
import com.example.backend.course.domain.model.Course

/**
 * 인바운드 포트 — 코스 쓰기(커맨드) 유스케이스. 조회는 [CourseQueryUseCase] 가 담당한다.
 * - [create] 코스 생성(발행·임시저장 공통) — 저장된 코스 애그리거트(생성 id·DB 생성값 포함)를 반환한다.
 * - [edit] 코스 편집(전체 치환) — 소유자만 가능하며, 편집된 코스 애그리거트(DB 생성값 포함)를 반환한다.
 * - [fork] 코스 포크 — 원본을 볼 수 있어야 가능하며, 새로 만들어진 **내** 코스 애그리거트를 반환한다.
 * - [delete] 코스 삭제(소프트 삭제) — 소유자만 가능하다. 성공/실패는 예외로 표현하므로 반환값은 없다.
 */
interface CourseUseCase {
    fun create(command: CreateCourseCommand): Course

    fun edit(command: EditCourseCommand): Course

    /**
     * 코스 포크 — 원본의 장소 구성 위에 포크하는 사람의 콘텐츠를 얹어 **새 코스**로 저장한다.
     * 원본을 볼 수 없으면(없음·삭제·비활성·공개범위 미달) 존재를 드러내지 않도록 404(COURSE_NOT_FOUND).
     */
    fun fork(command: ForkCourseCommand): Course

    fun delete(
        userId: Long,
        courseId: Long,
    )
}
