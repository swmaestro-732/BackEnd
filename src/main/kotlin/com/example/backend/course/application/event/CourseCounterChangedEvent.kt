package com.example.backend.course.application.event

/** 검색 색인에 따로 맞추는 코스 카운터. 저장·좋아요는 코스 본문을 바꾸지 않아 [CourseSavedEvent] 를 내지 않는다. */
enum class CourseCounter { SAVES, LIKES }

/**
 * 도메인 이벤트 — [courseIds] 각각의 [counter] 가 [delta] 만큼 바뀌었음을 알린다(+1/-1). 검색 색인의 카운터만 맞추는 용도다.
 * 탈퇴 정리처럼 여러 코스가 한 번에 바뀌면 코스별 이벤트 대신 목록 하나로 내 색인 갱신을 bulk 한 번으로 묶는다.
 */
data class CourseCounterChangedEvent(
    val courseIds: List<Long>,
    val counter: CourseCounter,
    val delta: Int,
)
