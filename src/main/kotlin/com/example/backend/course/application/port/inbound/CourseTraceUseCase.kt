package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.EndCourseTraceCommand

/** 인바운드 포트 — 코스 따라가기 종료. 따라간 기록(tracing_courses·방문 장소)을 남기고 코스 tracings_cnt 를 올린다. */
interface CourseTraceUseCase {
    /** 따라가기 종료. 볼 수 없는(비활성·비공개·미팔로우) 코스는 404. 같은 코스를 여러 번 따라가도 매번 기록한다. */
    fun end(command: EndCourseTraceCommand)
}
