package com.example.backend.course.application.event

/** 검색 색인에 따로 맞추는 코스 카운터. 저장·좋아요는 코스 본문을 바꾸지 않아 [CourseSavedEvent] 를 내지 않는다. */
enum class CourseCounter { SAVES, LIKES }

/** 도메인 이벤트 — 코스의 [counter] 가 [delta] 만큼 바뀌었음을 알린다(+1/-1). 검색 색인의 카운터만 맞추는 용도다. */
data class CourseCounterChangedEvent(
    val courseId: Long,
    val counter: CourseCounter,
    val delta: Int,
)
