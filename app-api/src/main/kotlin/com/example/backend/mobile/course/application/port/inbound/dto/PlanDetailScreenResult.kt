package com.example.backend.mobile.course.application.port.inbound.dto

import com.example.backend.course.application.port.inbound.dto.PlanDetailResult
import com.example.backend.place.application.port.inbound.dto.PlaceSummary

/**
 * 계획 상세 화면 조합 결과(BFF 애플리케이션 계층 DTO).
 * 도메인 인바운드 포트 결과를 묶기만 한다 — 표시 로직(장소명·좌표 결합)은 web 계층 매퍼가 입힌다
 * ([CourseDetailScreenResult] 와 같은 형태).
 *
 * - [plan] 계획 상세([com.example.backend.course.application.port.inbound.PlanQueryUseCase]) — 소유자 판정도 여기서 끝난다.
 * - [places] 계획에 담긴 장소들의 요약([com.example.backend.place.application.port.inbound.PlaceQueryUseCase]) —
 *   삭제된 장소는 빠질 수 있어 계획의 장소 수보다 적을 수 있다.
 */
data class PlanDetailScreenResult(
    val plan: PlanDetailResult,
    val places: List<PlaceSummary>,
)
