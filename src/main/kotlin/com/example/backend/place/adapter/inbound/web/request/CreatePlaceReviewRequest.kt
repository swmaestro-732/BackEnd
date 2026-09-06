package com.example.backend.place.adapter.inbound.web.request

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.place.application.port.inbound.dto.CreatePlaceReviewCommand
import com.example.backend.place.domain.model.PlaceReview
import com.example.backend.place.domain.model.PlaceReviewTag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** 장소 리뷰 생성 요청 — 웹 어댑터 DTO. */
data class CreatePlaceReviewRequest(
    @field:Min(1)
    @field:Max(5)
    val rating: Int,
    val content: String? = null,
    @field:Size(max = PlaceReview.MAX_PHOTOS)
    val photoUrls: List<
        @NotBlank
        String,
    > = emptyList(),
    @field:Size(max = PlaceReview.MAX_TAGS)
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
            tags =
                tagCodes
                    .map { code ->
                        PlaceReviewTag.fromCodeOrNull(code)
                            ?: throw BusinessException(CommonErrorCode.INVALID_INPUT, "알 수 없는 리뷰 태그입니다: $code")
                    }.toSet(),
        )
}
