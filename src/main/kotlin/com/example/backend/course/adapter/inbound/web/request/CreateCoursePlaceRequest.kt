package com.example.backend.course.adapter.inbound.web.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

/**
 * 코스에 담는 장소 한 곳. orderNo 는 0부터 시작한다.
 * 코스 생성([CreateCourseRequest])·수정([EditCourseRequest]) 요청에서 공용으로 재사용한다.
 * 사진 최소 1장 규칙은 발행 여부에 따라 달라지는 비즈니스 규칙이라 도메인([com.example.backend.course.domain.model.Course])에서
 * 검증한다(발행은 장소마다 1장 이상, 임시저장은 0장 허용). 여기선 URL 형식(@NotBlank)만 본다.
 */
data class CreateCoursePlaceRequest(
    @field:Positive
    val placeId: Long,
    @field:Min(0)
    val orderNo: Int,
    @field:Size(max = 200)
    val caption: String?,
    val imageUrls: List<@NotBlank String>,
)
