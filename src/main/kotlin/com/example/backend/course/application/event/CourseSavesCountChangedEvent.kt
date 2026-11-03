package com.example.backend.course.application.event

/**
 * 도메인 이벤트 — 코스 저장수(saves_cnt)가 [delta] 만큼 바뀌었음을 알린다(저장 +1, 취소 -1).
 * 저장/취소는 코스 본문을 바꾸지 않아 [CourseSavedEvent] 를 내지 않으므로, 검색 색인의 savesCnt 만 따로 맞추는 용도다.
 */
data class CourseSavesCountChangedEvent(
    val courseId: Long,
    val delta: Int,
)
