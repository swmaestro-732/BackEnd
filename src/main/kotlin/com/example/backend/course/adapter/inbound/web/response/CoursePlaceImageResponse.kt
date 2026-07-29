package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.course.application.port.inbound.dto.CoursePlaceImageResult

/** 장소 사진(course_place_images 행). orderNo 는 해당 장소 안에서의 사진 순서. */
data class CoursePlaceImageResponse(
    val imageUrl: String,
    val orderNo: Int,
) {
    companion object {
        fun from(image: CoursePlaceImageResult): CoursePlaceImageResponse =
            CoursePlaceImageResponse(image.imageUrl, image.orderNo)
    }
}
