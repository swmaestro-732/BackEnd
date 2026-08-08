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
    /**
     * 다음 장소까지 도보 소요 시간(분). **양수만 허용하지 않는다** — 세 가지 값을 받는다:
     * - 정상: 0 이상의 분
     * - [UNREACHABLE_ON_FOOT](-1): 걸어서 갈 수 없는 구간
     * - null: 마지막 장소(다음 장소가 없음)
     *
     * 그래서 `@Positive` 가 아니라 `@Min(-1)` 로 하한만 막는다(-2 이하는 의미 없는 값이라 거부).
     * "마지막 장소만 null" 은 강제하지 않는다 — 클라이언트가 중간 구간을 미측정 상태로 보낼 수 있어서다.
     */
    @field:Min(UNREACHABLE_ON_FOOT)
    val walkingMinutes: Int? = null,
) {
    companion object {
        /** 도보 이동 불가 구간을 나타내는 센티널. 소요 시간이 아니므로 합계·표시에서 제외해야 한다. */
        const val UNREACHABLE_ON_FOOT = -1L
    }
}
