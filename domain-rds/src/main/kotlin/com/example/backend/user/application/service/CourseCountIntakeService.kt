package com.example.backend.user.application.service

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.user.application.port.inbound.CourseCountIntakeUseCase
import com.example.backend.user.application.port.inbound.CourseCountUseCase
import com.example.backend.user.application.port.outbound.CourseCountFallbackPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

/**
 * 코스 개수 반영의 진입점. 주 경로는 동기 반영([CourseCountUseCase.apply], 별도 트랜잭션)이고,
 * 실패할 때만 [CourseCountFallbackPort] 로 SQS 폴백에 싣는다. 발행 때 정해진 eventId 를 동기와 폴백에 공유한다.
 * 폴백 발송이 실패하면 예외가 그대로 올라가 이벤트 저장소가 같은 eventId 로 재발행한다.
 * 트랜잭션을 열지 않는다: apply 실패가 이 호출까지 롤백시키지 않아야 폴백을 보낼 수 있다.
 */
@Service
class CourseCountIntakeService(
    private val courseCountUseCase: CourseCountUseCase,
    private val courseCountFallbackPort: CourseCountFallbackPort,
) : CourseCountIntakeUseCase {
    private val log = KotlinLogging.logger {}

    override fun receive(
        eventId: String,
        authorId: Long,
        oldVisibility: CourseVisibility?,
        newVisibility: CourseVisibility?,
    ) {
        try {
            courseCountUseCase.apply(eventId, authorId, oldVisibility, newVisibility)
        } catch (e: Exception) {
            log.warn { "동기 코스 개수 반영 실패, SQS 폴백: authorId=$authorId eventId=$eventId (${e.message})" }
            courseCountFallbackPort.publish(authorId, oldVisibility, newVisibility, eventId)
        }
    }
}
