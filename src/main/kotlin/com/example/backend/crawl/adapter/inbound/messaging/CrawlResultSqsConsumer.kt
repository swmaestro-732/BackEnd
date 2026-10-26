package com.example.backend.crawl.adapter.inbound.messaging

import com.example.backend.crawl.application.port.inbound.CrawlImportUseCase
import com.example.backend.crawl.application.port.inbound.dto.CrawlResultCommand
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component

/**
 * 크롤링 결과 큐 수신. 큐 URL과 큐레이터 사용자 ID가 모두 설정된 배포에서만 활성화한다 —
 * 하나라도 비면 빈을 만들지 않아(fail-soft) 인프라가 큐 URL만 먼저 주입해도 앱 기동이 깨지지 않는다.
 */
@Component
@ConditionalOnExpression(
    "'\${aws.sqs.crawl-results-queue-url:}'.trim().length() > 0 && '\${crawler.curator-user-id:}'.trim().length() > 0",
)
class CrawlResultSqsConsumer(
    private val crawlImportUseCase: CrawlImportUseCase,
) {
    // 예외는 전파해 ack하지 않는다. DLQ의 maxReceiveCount=5는 큐의 redrive policy에서 설정한다.
    @SqsListener(
        value = ["\${aws.sqs.crawl-results-queue-url}"],
        maxConcurrentMessages = "1",
        maxMessagesPerPoll = "1",
    )
    fun onMessage(message: CrawlResultCommand) {
        crawlImportUseCase.importResult(message)
    }
}
