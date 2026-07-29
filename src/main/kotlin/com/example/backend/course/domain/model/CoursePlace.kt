package com.example.backend.course.domain.model

/**
 * 코스에 담긴 장소([Course] 애그리거트의 구성 요소). placeId 는 place 도메인 식별자(크로스 도메인, FK 없음).
 * 생성 시 도보 시간은 서버 자동 계산 전이라 담지 않는다(course_places.walking_minutes 는 null 로 저장).
 */
data class CoursePlace(
    val placeId: Long,
    val orderNo: Int,
    val caption: String?,
    val imageUrls: List<String>,
)
