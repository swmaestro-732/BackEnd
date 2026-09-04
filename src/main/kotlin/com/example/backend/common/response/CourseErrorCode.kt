package com.example.backend.common.response

enum class CourseErrorCode(
    override val status: Int,
    override val code: Int,
    override val message: String,
) : ErrorCode {
    PUBLISHED_COURSE_PLACES_IMMUTABLE(400, 4003, "게시된 코스의 장소 구성은 변경할 수 없습니다. 캡션만 수정할 수 있습니다."),
    FORK_PLACES_NOT_KEPT(400, 4004, "포크한 코스는 원본 장소를 일정 수 이상 그대로 담아야 합니다."),
    COURSE_NOT_FOUND(404, 4041, "코스를 찾을 수 없습니다."),
}
