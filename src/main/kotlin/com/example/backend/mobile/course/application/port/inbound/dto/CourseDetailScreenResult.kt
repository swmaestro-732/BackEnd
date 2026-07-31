package com.example.backend.mobile.course.application.port.inbound.dto

import com.example.backend.course.application.port.inbound.dto.CourseDetailResult
import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import com.example.backend.user.application.port.inbound.dto.UserProfileResult

/**
 * 코스 상세 화면 조합 결과(BFF 애플리케이션 계층 DTO).
 * 도메인 인바운드 포트 3개의 결과를 묶기만 한다 — 표시 로직(축약 라벨·장소명/카테고리 결합 등)은 web 계층 매퍼가 입힌다.
 *
 * - [course] 코스 상세([com.example.backend.course.application.port.inbound.CourseUseCase]).
 * - [author] 작성자 프로필([com.example.backend.user.application.port.inbound.UserUseCase]).
 * - [places] 코스에 담긴 장소들의 요약([com.example.backend.place.application.port.inbound.PlaceQueryUseCase]) — 삭제된 장소는 빠질 수 있다.
 */
data class CourseDetailScreenResult(
    val course: CourseDetailResult,
    val author: UserProfileResult,
    val places: List<PlaceSummary>,
)
