package com.example.backend.user.adapter.inbound.messaging

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.user.adapter.messaging.CourseCountMessage
import com.example.backend.user.application.port.inbound.CourseCountUseCase
import io.awspring.cloud.sqs.annotation.SqsListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component

/**
 * 인바운드 어댑터 — 폴백 큐(SQS)의 코스 개수 메시지를 수신해 [CourseCountUseCase] 로 반영한다(동기 경로 실패분 재시도).
 *
 * 큐가 설정된 배포에서만 활성(@ConditionalOnExpression) — 로컬·CI 는 큐 미설정이라 이 빈이 생성되지 않아
 * 리스너 컨테이너도 뜨지 않는다. 반영 성공 시 spring-cloud-aws 가 자동 ack, 예외면 ack 하지 않아
 * (가시성 타임아웃 후) 재전송된다. 반영은 eventId 로 멱등이라 at-least-once 재전송에도 안전하다.
 */
@Component
@ConditionalOnExpression("'\${aws.sqs.course-count-queue-url:}'.trim().length() > 0")
class CourseCountSqsConsumer(
    private val courseCountUseCase: CourseCountUseCase,
) {
    private val log = KotlinLogging.logger {}

    @SqsListener("\${aws.sqs.course-count-queue-url}")
    fun onMessage(message: CourseCountMessage) {
        courseCountUseCase.apply(
            eventId = message.eventId,
            authorId = message.authorId,
            oldVisibility = parseVisibility(message.oldVisibility),
            newVisibility = parseVisibility(message.newVisibility),
        )
    }

    /**
     * 공개범위 문자열을 enum 으로 파싱한다. null 은 "카운트 대상 아님"(임시저장·삭제 후)이라 그대로 통과시키지만,
     * 값이 있는데 이 인스턴스가 모르는 공개범위면 **예외를 던진다** — null 로 강등하면 전이 델타가 틀려(예: PUBLIC→새값 이
     * publicDelta=-1 만 반영) 카운트가 영구 손상되기 때문이다. 예외 시 ack 하지 않아 가시성 타임아웃 후 재전송되고,
     * 새 enum 을 아는 배포가 완료되면 정상 처리된다(반복 실패는 DLQ 로 격리). 롤링 배포 중 새 공개범위 추가 시나리오 대비.
     */
    private fun parseVisibility(name: String?): CourseVisibility? {
        if (name == null) return null
        return runCatching { CourseVisibility.valueOf(name) }
            .getOrElse {
                log.warn { "미정의 공개범위 수신 — 재처리 위해 예외(ack 안 함): $name" }
                throw IllegalArgumentException("미정의 공개범위: $name")
            }
    }
}
