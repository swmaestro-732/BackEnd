package com.example.backend.place.adapter.inbound.web.request

import com.example.backend.place.application.port.inbound.dto.CreatePlaceReviewCommand
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** 리뷰 사진 최대 개수 — 디자인(장소 리뷰 작성 "사진 추가 1/6"). */
private const val MAX_PHOTOS = 6

/** 한 리뷰에 선택할 수 있는 태그 최대 개수 — 디자인 태그 칩 그룹(업종별 6 + 공통) 기준 상한. */
private const val MAX_TAGS = 5

/** 장소 리뷰 생성 요청 — 웹 어댑터 DTO. */
data class CreatePlaceReviewRequest(
    @field:Min(1)
    @field:Max(5)
    val rating: Int,
    val content: String? = null,
    @field:Size(max = MAX_PHOTOS)
    val photoUrls: List<
        @NotBlank
        String,
    > = emptyList(),
    @field:Size(max = MAX_TAGS)
    val tagCodes: List<
        @NotBlank
        String,
    > = emptyList(),
) {
    fun toCommand(
        placeId: Long,
        userId: Long,
    ): CreatePlaceReviewCommand =
        CreatePlaceReviewCommand(
            placeId = placeId,
            userId = userId,
            rating = rating,
            content = content,
            photoUrls = photoUrls,
            tagCodes = tagCodes,
        )
}
