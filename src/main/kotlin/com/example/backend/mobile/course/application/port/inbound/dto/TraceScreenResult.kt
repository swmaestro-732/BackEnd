package com.example.backend.mobile.course.application.port.inbound.dto

import com.example.backend.course.application.port.inbound.dto.CourseTraceDetailResult
import com.example.backend.place.application.port.inbound.dto.PlaceSummary

/**
 * 따라가기 상세 화면 조합 결과(BFF 애플리케이션 계층 DTO). 도메인 인바운드 포트 결과를 묶기만 한다 — 결합은 web 계층 매퍼가 한다.
 * - [trace] 세션·코스 요약·코스 장소·체크인([com.example.backend.course.application.port.inbound.CourseTraceUseCase]).
 * - [places] 코스 장소들의 요약([com.example.backend.place.application.port.inbound.PlaceQueryUseCase]) — 삭제된 장소는 빠진다.
 */
data class TraceScreenResult(
    val trace: CourseTraceDetailResult,
    val places: List<PlaceSummary>,
)
