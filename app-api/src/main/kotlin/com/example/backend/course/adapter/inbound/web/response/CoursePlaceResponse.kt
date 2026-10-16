package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.course.application.port.inbound.dto.CoursePlaceResult

/**
 * 코스에 담긴 장소(course_places 행).
 * id 는 course_place 식별자, placeId 는 place 도메인 식별자(별개). orderNo 는 코스 내 장소 순서.
 */
data class CoursePlaceResponse(
    val id: Long,
    val placeId: Long,
    val orderNo: Int,
    val caption: String?,
    /** 다음 장소까지 도보 이동 시간(분). 마지막 장소면 null. */
    val walkingMinutesToNext: Int?,
    val images: List<CoursePlaceImageResponse>,
) {
    companion object {
        fun from(place: CoursePlaceResult): CoursePlaceResponse =
            CoursePlaceResponse(
                id = place.id,
                placeId = place.placeId,
                orderNo = place.orderNo,
                caption = place.caption,
                walkingMinutesToNext = place.walkingMinutesToNext,
                images = place.images.map(CoursePlaceImageResponse::from),
            )
    }
}
