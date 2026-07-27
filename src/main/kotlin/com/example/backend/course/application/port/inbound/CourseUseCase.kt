package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.CourseDetailResult
import com.example.backend.course.application.port.inbound.dto.CreateCourseCommand
import com.example.backend.course.application.port.inbound.dto.EditCourseCommand
import com.example.backend.course.domain.model.Course

/**
 * 인바운드 포트 — 코스 도메인 유스케이스.
 * - [getDetail] 코스 상세 조회. viewerId 는 로그인 사용자(비로그인이면 null) — 조회자 상태·비공개 접근 판정용.
 * - [create] 코스 생성(발행·임시저장 공통) — 저장된 코스 애그리거트(생성 id·DB 생성값 포함)를 반환한다.
 * - [edit] 코스 편집(전체 치환) — 소유자만 가능하며, 편집된 코스 애그리거트(DB 생성값 포함)를 반환한다.
 */
interface CourseUseCase {
    fun getDetail(
        courseId: Long,
        viewerId: Long?,
    ): CourseDetailResult

    fun create(command: CreateCourseCommand): Course

    fun edit(command: EditCourseCommand): Course
}
