package com.example.backend.course.application.port.outbound

import com.example.backend.course.application.port.inbound.dto.EndCourseTraceCommand

/** 아웃바운드 포트 — 코스 따라가기 기록(tracing_courses·tracing_course_places) 접근. 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다. */
interface CourseTracePersistencePort {
    /** 따라가기 기록과 방문 장소를 삽입한다. */
    fun insert(command: EndCourseTraceCommand)
}
