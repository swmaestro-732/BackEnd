package com.example.backend.course.application.port.outbound

import com.example.backend.course.application.event.CourseCountDeltaEvent

/**
 * 아웃바운드 포트 — 작성자 코스 개수 델타를 메시지 큐로 발행한다.
 * 구현(어댑터)은 SQS 전송을 담당하며, 큐 미설정(로컬·CI)·전송 실패는 fail-soft 로 삼킨다.
 */
interface CourseCountMessagePort {
    fun send(event: CourseCountDeltaEvent)
}
