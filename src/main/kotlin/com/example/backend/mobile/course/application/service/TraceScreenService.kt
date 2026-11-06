package com.example.backend.mobile.course.application.service

import com.example.backend.course.application.port.inbound.CourseTraceUseCase
import com.example.backend.mobile.course.application.port.inbound.TraceScreenUseCase
import com.example.backend.mobile.course.application.port.inbound.dto.TraceScreenResult
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 따라가기 상세 화면 조합 서비스 (BFF) — 세션 상세에 장소 요약(이름·카테고리·좌표·대표 이미지)을 붙인다. */
@Service
@Transactional(readOnly = true)
class TraceScreenService(
    private val courseTraceUseCase: CourseTraceUseCase,
    private val placeQueryUseCase: PlaceQueryUseCase,
) : TraceScreenUseCase {
    override fun getScreen(
        traceId: Long,
        userId: Long,
    ): TraceScreenResult {
        val trace = courseTraceUseCase.getDetail(userId, traceId)
        val places = placeQueryUseCase.findPlacesById(trace.places.map { it.placeId }.distinct())
        return TraceScreenResult(trace = trace, places = places)
    }
}
