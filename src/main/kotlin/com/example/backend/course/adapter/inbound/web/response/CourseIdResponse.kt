package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.course.domain.model.Course

/**
 * 코스 식별자만 담는 응답 — 생성·편집·포크가 공유한다.
 * 세 경우 모두 이후 화면은 코스 상세 API 재조회로 구성하므로 courseId 만 내려준다.
 * 포크는 원본이 아니라 **새로 만들어진 내 코스**의 id 를 담는다.
 */
data class CourseIdResponse(
    val courseId: Long,
) {
    companion object {
        /**
         * 저장된 코스([Course])를 응답으로 옮긴다. 도메인의 id 는 insert 전 미정이라 nullable 이지만
         * 저장·갱신을 마친 코스에는 항상 채워져 있으므로, 여기서 한 번만 확정한다.
         */
        fun from(course: Course) = CourseIdResponse(courseId = requireNotNull(course.id))

        /**
         * 코스 생성 `?mock=true` 폴백 응답. 코스 상세 목 데이터([com.example.backend.course.adapter.inbound.web.CourseController]
         * 의 MOCK_DETAIL, courseId=1)와 이어지도록 항상 1을 반환한다. 실구현 전환 시 호출부와 함께 제거한다.
         */
        val MOCK = CourseIdResponse(courseId = 1L)
    }
}
